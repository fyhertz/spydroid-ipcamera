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
import java.security.acl.LastOwnerException;

import net.majorkernelpanic.http.TinyHttpServer;
import net.majorkernelpanic.spydroid.R;
import net.majorkernelpanic.spydroid.SpydroidApplication;
import net.majorkernelpanic.spydroid.api.CustomHttpServer;
import net.majorkernelpanic.streaming.SessionManager;
import net.majorkernelpanic.streaming.misc.HttpServer;
import net.majorkernelpanic.streaming.misc.RtspServer;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
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
 * Spydroid basically launches an RTSP server and an HTTP server, 
 * clients can then connect to them and start/stop audio/video streams from the phone.
 */
public class SpydroidActivity extends FragmentActivity {

	static final public String TAG = "SpydroidActivity";

	public final int HANDSET = 0x01;
	public final int TABLET = 0x02;

	// We assume that the device is a phone
	public int device = HANDSET;

	// The RTSP server
	static private RtspServer sRtspServer = null;

	// The HTTP/S server.
	public CustomHttpServer mHttpServer = null;

	private ViewPager mViewPager;
	private PowerManager.WakeLock mWakeLock;
	private SectionsPagerAdapter mAdapter;

	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;

	private SpydroidApplication mApplication;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mApplication = (SpydroidApplication) getApplication();

		setContentView(R.layout.spydroid);

		if (findViewById(R.id.handset_pager) != null) {

			// Handset detected !

			mAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
			mViewPager = (ViewPager) findViewById(R.id.handset_pager);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			mSurfaceView = (SurfaceView)findViewById(R.id.handset_camera_view);
			mSurfaceHolder = mSurfaceView.getHolder();
			// We still need this line for backward compatibility reasons with android 2
			mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			SessionManager.getManager().setSurfaceHolder(mSurfaceHolder, !mApplication.mHackEnabled);

		} else {

			// Tablet detected !

			device = TABLET;
			mAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
			mViewPager = (ViewPager) findViewById(R.id.tablet_pager);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			mApplication.mVideoQuality.orientation = 0;

		}

		mViewPager.setAdapter(mAdapter);

		// Those callbacks will be called when streaming starts/stops
		SessionManager.getManager().setCallbackListener(mSessionManagerCallbacks);

		// Remove the ads if this is the donate version of the app.
		if (mApplication.DONATE_VERSION) {
			((LinearLayout)findViewById(R.id.adcontainer)).removeAllViews();
		}

		// Prevents the phone to go to sleep mode
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "net.majorkernelpanic.spydroid.wakelock");

		// Instantiation of the RTSP server
		if (sRtspServer == null) 
			sRtspServer = new RtspServer(mApplication.mRtspPort, mHandler);

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
		if (mApplication.mNotificationEnabled) {
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
		} else {
			removeNotification();
		}

		bindService(new Intent(this,CustomHttpServer.class), mHttpServiceConnection, Context.BIND_AUTO_CREATE);

	}

	@Override
	public void onStop() {
		super.onStop();
		// A WakeLock should only be released when isHeld() is true !
		if (mWakeLock.isHeld()) mWakeLock.release();
		unbindService(mHttpServiceConnection);
	}

	@Override
	public void onResume() {
		super.onResume();
		mApplication.mApplicationForeground = true;
		startServers();
	}

	@Override
	public void onPause() {
		super.onPause();
		mApplication.mApplicationForeground = false;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG,"SpydroidActivity destroyed");
		super.onDestroy();
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
			quitSpydroid();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void startServers() {
		if (sRtspServer != null) {
			try {
				sRtspServer.start();
			} catch (IOException e) {
				log("RtspServer could not be started : "+(e.getMessage()!=null?e.getMessage():"Unknown error"));
			}
		}
	}

	private void quitSpydroid() {
		// Removes notification
		if (mApplication.mNotificationEnabled) removeNotification();       
		// Kills HTTP server
		if (mHttpServer != null) {
			mHttpServer.stop();
		}
		// Kills RTSP server
		if (sRtspServer != null) {
			sRtspServer.stop();
			sRtspServer = null;
		}
		finish();
	}

	private HttpServer.CallbackListener mHttpCallbackListener = new HttpServer.CallbackListener() {

		@Override
		public void onError(TinyHttpServer server, Exception e, int error) {
			switch (error) {
			case HttpServer.ERROR_HTTPS_BIND_FAILED:
				server.setHttpsPort(server.getHttpsPort()+1);
				if (mAdapter.getHandsetFragment() != null) 
					mAdapter.getHandsetFragment().displayIpAddress();
				break;
			case HttpServer.ERROR_HTTP_BIND_FAILED:
				server.setHttpPort(server.getHttpPort()+1);
				if (mAdapter.getHandsetFragment() != null) 
					mAdapter.getHandsetFragment().displayIpAddress();
				break;
			case HttpServer.ERROR_START_FAILED:
				mApplication.mLastCaughtException = e;
				break;
			}
		}

	}; 

	private ServiceConnection mHttpServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mHttpServer = (CustomHttpServer) ((TinyHttpServer.LocalBinder)service).getService();
			mHttpServer.setCallbackListener(mHttpCallbackListener);
			mHttpServer.start();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {}

	};
	
	private SessionManager.CallbackListener mSessionManagerCallbacks = new SessionManager.CallbackListener() {
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
				}
			});				
		}
	};

	// The Handler that gets information back from the RtspServer
	private final Handler mHandler = new Handler() {

		public void handleMessage(Message msg) { 
			switch (msg.what) {
			case RtspServer.MESSAGE_ERROR:
				Exception e1 = (Exception)msg.obj;
				mApplication.mLastCaughtException = e1;
				log(e1.getMessage()!=null?e1.getMessage():"An error occurred !");
				break;
			case RtspServer.MESSAGE_LOG:
				//log((String)msg.obj);
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