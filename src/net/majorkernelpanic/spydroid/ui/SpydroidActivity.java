/*
 * Copyright (C) 2011-2013 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.majorkernelpanic.spydroid.ui;

import java.io.IOException;

import net.majorkernelpanic.spydroid.R;
import net.majorkernelpanic.spydroid.SpydroidApplication;
import net.majorkernelpanic.spydroid.api.CustomHttpServer;
import net.majorkernelpanic.streaming.SessionManager;
import net.majorkernelpanic.streaming.misc.HttpServer;
import net.majorkernelpanic.streaming.misc.RtspServer;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;
import android.widget.Toast;

/** 
 * Spydroid basically launches an RtspServer and an HttpServer, 
 * clients can then connect to them and start/stop audio/video streams from the phone
 */
public class SpydroidActivity extends FragmentActivity implements OnSharedPreferenceChangeListener {

	static final public String TAG = "SpydroidActivity";

	public static final int HANDSET = 0x01;
	public static final int TABLET = 0x02;

	// We assume that the device is a phone
	public static int device = HANDSET;

	// The HTTP and RTSP servers
	static private CustomHttpServer mHttpServer = null;
	static private RtspServer mRtspServer = null;

	// The HttpServer will use those variables to send reports about the state of the app to the web interface
	public static boolean activityPaused = true;
	public static Exception lastCaughtException;

	// Prevent garbage collection of the Surface
	public static boolean hackEnabled = false;

	private ViewPager mViewPager;
	private PowerManager.WakeLock mWakeLock;
	private SectionsPagerAdapter mAdapter;
	private boolean mNotificationEnabled = true;

	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.spydroid);

		// Restores some settings 
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		mNotificationEnabled = settings.getBoolean("notification_enabled", true);
		hackEnabled = settings.getBoolean("video_hack", false);

		// Listens to changes of preferences
		settings.registerOnSharedPreferenceChangeListener(this);

		if (findViewById(R.id.handset_pager) != null) {

			// Handset detected !

			mAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
			mViewPager = (ViewPager) findViewById(R.id.handset_pager);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			mSurfaceView = (SurfaceView)findViewById(R.id.handset_camera_view);
			mSurfaceHolder = mSurfaceView.getHolder();
			// We still need this line for backward compatibility reasons with android 2
			mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			SessionManager.getManager().setSurfaceHolder(mSurfaceHolder, !SpydroidActivity.hackEnabled);

		} else {

			// Tablet detected !

			device = TABLET;
			mAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
			mViewPager = (ViewPager) findViewById(R.id.tablet_pager);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			SpydroidApplication.sVideoQuality.orientation = 0;

		}

		mViewPager.setAdapter(mAdapter);

		// Those callbacks will be called when streaming starts/stops
		SessionManager.getManager().setCallbackListener(new SessionManager.CallbackListener() {
			@Override
			public void onStreamingStarted(SessionManager manager) {
				runOnUiThread(new Runnable () {
					public void run() {
						if (mAdapter.getHandsetFragment() != null) 
							mAdapter.getHandsetFragment().streamingState(1);
					}
				});
			}
			@Override
			public void onStreamingStopped(SessionManager manager) {
				runOnUiThread(new Runnable () {
					public void run() {
						if (mAdapter.getHandsetFragment() != null) 
							mAdapter.getHandsetFragment().displayIpAddress();
						else
							Log.e(TAG,"HandsetFragment does not exist");
					}
				});				
			}
		});

		// Remove the ads if this is the donate version of the app.
		if (SpydroidApplication.DONATE_VERSION) {
			((LinearLayout)findViewById(R.id.adcontainer)).removeAllViews();
		}

		// Prevents the phone to go to sleep mode
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "net.majorkernelpanic.spydroid.wakelock");

		// Instantiation of the HTTP and RTSP servers
		if (mRtspServer == null) 
			mRtspServer = new RtspServer(SpydroidApplication.sRtspPort, mHandler);
		if (mHttpServer == null && settings.getBoolean("enable_http", true)) 
			mHttpServer = new CustomHttpServer(SpydroidApplication.sHttpPort, this.getApplicationContext(), mHandler);  

	}

	class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			if (device == HANDSET) {
				switch (i) {
				case 0: return new HandsetFragment();
				case 1: return new PreviewFragment();
				case 2: return new AboutFragment();
				}
			} else {
				switch (i) {
				case 0: return new TabletFragment();
				case 1: return new AboutFragment();
				}        		
			}
			return null;
		}

		@Override
		public int getCount() {
			return device==HANDSET ? 3 : 2;
		}

		public HandsetFragment getHandsetFragment() {
			if (device == HANDSET) {
				return (HandsetFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.handset_pager+":0");
			} else {
				return (HandsetFragment) getSupportFragmentManager().findFragmentById(R.id.handset);
			}
		}

		public PreviewFragment getPreviewFragment() {
			if (device == HANDSET) {
				return (PreviewFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.handset_pager+":1");
			} else {
				return (PreviewFragment) getSupportFragmentManager().findFragmentById(R.id.preview);
			}
		}

		@Override
		public CharSequence getPageTitle(int position) {
			if (device == HANDSET) {
				switch (position) {
				case 0: return getString(R.string.page0);
				case 1: return getString(R.string.page1);
				case 2: return getString(R.string.page2);
				}        		
			} else {
				switch (position) {
				case 0: return getString(R.string.page0);
				case 1: return getString(R.string.page2);
				}
			}
			return null;
		}

	}

	public void onStart() {
		super.onStart();

		// Lock screen
		mWakeLock.acquire();

		// Did the user disabled the notification ?
		if (mNotificationEnabled) {
			Intent notificationIntent = new Intent(this, SpydroidActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

			NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
			Notification notification = builder.setContentIntent(pendingIntent)
					.setWhen(System.currentTimeMillis())
					.setTicker(getText(R.string.notification_title))
					.setSmallIcon(R.drawable.icon)
					.setContentTitle(getText(R.string.notification_title))
					.setContentText(getText(R.string.notification_content)).build();
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(0,notification);
		}

	}

	@Override
	public void onStop() {
		super.onStop();
		// A WakeLock should only be released when isHeld() is true !
		if (mWakeLock.isHeld()) mWakeLock.release();
	}

	@Override
	public void onResume() {
		super.onResume();
		activityPaused = true;
		startServers();
	}

	@Override
	public void onPause() {
		super.onPause();
		activityPaused = false;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG,"SpydroidActivity destroyed");
		super.onDestroy();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("video_resX") || key.equals("video_resY")) {
			SpydroidApplication.sVideoQuality.resX = sharedPreferences.getInt("video_resX", 0);
			SpydroidApplication.sVideoQuality.resY = sharedPreferences.getInt("video_resY", 0);
		}
		else if (key.equals("video_framerate")) {
			SpydroidApplication.sVideoQuality.framerate = Integer.parseInt(sharedPreferences.getString("video_framerate", "0"));
		}
		else if (key.equals("video_bitrate")) {
			SpydroidApplication.sVideoQuality.bitrate = Integer.parseInt(sharedPreferences.getString("video_bitrate", "0"))*1000;
		}
		else if (key.equals("audio_encoder") || key.equals("stream_audio")) { 
			SpydroidApplication.sAudioEncoder = Integer.parseInt(sharedPreferences.getString("audio_encoder", "0"));
			SessionManager.getManager().setDefaultAudioEncoder( SpydroidApplication.sAudioEncoder );
			if (!sharedPreferences.getBoolean("stream_audio", false)) 
				SessionManager.getManager().setDefaultAudioEncoder(0);
		}
		else if (key.equals("stream_video") || key.equals("video_encoder")) {
			SpydroidApplication.sVideoEncoder = Integer.parseInt(sharedPreferences.getString("video_encoder", "0"));
			SessionManager.getManager().setDefaultVideoEncoder( SpydroidApplication.sVideoEncoder );
			if (!sharedPreferences.getBoolean("stream_video", true)) 
				SessionManager.getManager().setDefaultVideoEncoder(0);
		}
		else if (key.equals("enable_http")) {
			if (sharedPreferences.getBoolean("enable_http", true)) {
				if (mHttpServer == null) {
					mHttpServer = new CustomHttpServer(SpydroidApplication.sHttpPort, this.getApplicationContext(), mHandler);
					startServers();
				}
			} else {
				if (mHttpServer != null) {
					mHttpServer.stop();
					mHttpServer = null;
				}
			}
		}
		else if (key.equals("enable_rtsp")) {
			if (sharedPreferences.getBoolean("enable_rtsp", true)) {
				if (mRtspServer == null) {
					mRtspServer = new RtspServer(SpydroidApplication.sRtspPort, mHandler);
					startServers();
				}
			} else {
				if (mRtspServer != null) {
					mRtspServer.stop();
					mRtspServer = null;
				}
			}
		}
		else if (key.equals("notification_enabled")) {
			mNotificationEnabled  = sharedPreferences.getBoolean("notification_enabled", true);
			removeNotification();
		}
		else if (key.equals("video_hack")) {
			hackEnabled = sharedPreferences.getBoolean("video_hack", false);
			SurfaceHolder holder = SessionManager.getManager().getSurfaceHolder();
			SessionManager.getManager().setSurfaceHolder(holder,!hackEnabled);
		}
		else if (key.equals("http_port")) {
			int port = Integer.parseInt(sharedPreferences.getString("http_port", String.valueOf(SpydroidApplication.sHttpPort)));
			SpydroidApplication.sHttpPort = port;
			mHttpServer.stop();
			mHttpServer = new CustomHttpServer(port, this.getApplicationContext(), mHandler);
			startServers();
		}
	}  

	@Override    
	public void onBackPressed() {
		Intent setIntent = new Intent(Intent.ACTION_MAIN);
		setIntent.addCategory(Intent.CATEGORY_HOME);
		setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(setIntent);
	}

	@Override    
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.quit), 1);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.options), 1);
		return true;
	}

	@Override    
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;

		switch (item.getItemId()) {
		case R.id.options:
			// Starts QualityListActivity where user can change the streaming quality
			intent = new Intent(this.getBaseContext(),OptionsActivity.class);
			startActivityForResult(intent, 0);
			return true;
		case R.id.quit:
			// Quits Spydroid i.e. stops the HTTP & RTSP servers
			stopServers();  
			// Remove notification
			if (mNotificationEnabled) removeNotification();          	
			finish();	
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void startServers() {
		if (mRtspServer != null) {
			try {
				mRtspServer.start();
			} catch (IOException e) {
				log("RtspServer could not be started : "+(e.getMessage()!=null?e.getMessage():"Unknown error"));
			}
		}
		if (mHttpServer != null) {
			try {
				mHttpServer.start();
			} catch (IOException e) {
				log("HttpServer could not be started : "+(e.getMessage()!=null?e.getMessage():"Unknown error"));
			}
		}
	}

	private void stopServers() {
		if (mHttpServer != null) {
			mHttpServer.stop();
			mHttpServer = null;
		}
		if (mRtspServer != null) {
			mRtspServer.stop();
			mRtspServer = null;
		}
	}

	// The Handler that gets information back from the RtspServer and the HttpServer
	private final Handler mHandler = new Handler() {

		public void handleMessage(Message msg) { 
			switch (msg.what) {
			case RtspServer.MESSAGE_ERROR:
				Exception e1 = (Exception)msg.obj;
				lastCaughtException = e1;
				log(e1.getMessage()!=null?e1.getMessage():"An error occurred !");
				break;
			case RtspServer.MESSAGE_LOG:
				//log((String)msg.obj);
				break;
			case HttpServer.MESSAGE_ERROR:
				Exception e2 = (Exception)msg.obj;
				lastCaughtException = e2;
				break;    			
			}
		}

	};

	private void removeNotification() {
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
	}

	public void log(String s) {
		Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
	}

}