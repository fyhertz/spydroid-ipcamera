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

import net.majorkernelpanic.http.TinyHttpServer;
import net.majorkernelpanic.spydroid.R;
import net.majorkernelpanic.spydroid.SpydroidApplication;
import net.majorkernelpanic.spydroid.api.CustomHttpServer;
import net.majorkernelpanic.spydroid.api.CustomRtspServer;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
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
import android.widget.LinearLayout;
import android.widget.Toast;

/** 
 * Spydroid basically launches an RTSP server and an HTTP server, 
 * clients can then connect to them and start/stop audio/video streams on the phone.
 */
public class SpydroidActivity extends FragmentActivity {

	static final public String TAG = "SpydroidActivity";

	public final int HANDSET = 0x01;
	public final int TABLET = 0x02;

	// We assume that the device is a phone
	public int device = HANDSET;

	private ViewPager mViewPager;
	private PowerManager.WakeLock mWakeLock;
	private SectionsPagerAdapter mAdapter;
	private SurfaceView mSurfaceView;
	private SpydroidApplication mApplication;
	private CustomHttpServer mHttpServer;
	private RtspServer mRtspServer;

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
			SessionBuilder.getInstance().setSurfaceView(mSurfaceView);
			SessionBuilder.getInstance().setPreviewOrientation(90);
			
		} else {

			// Tablet detected !
			device = TABLET;
			mAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
			mViewPager = (ViewPager) findViewById(R.id.tablet_pager);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			SessionBuilder.getInstance().setPreviewOrientation(0);
			
		}

		mViewPager.setAdapter(mAdapter);

		// Remove the ads if this is the donate version of the app.
		if (mApplication.DONATE_VERSION) {
			((LinearLayout)findViewById(R.id.adcontainer)).removeAllViews();
		}

		// Prevents the phone from going to sleep mode
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "net.majorkernelpanic.spydroid.wakelock");

		// Starts the service of the HTTP server
		this.startService(new Intent(this,CustomHttpServer.class));

		// Starts the service of the RTSP server
		this.startService(new Intent(this,CustomRtspServer.class));

	}

	public void onStart() {
		super.onStart();

		// Lock screen
		mWakeLock.acquire();

		// Did the user disabled the notification ?
		if (mApplication.notificationEnabled) {
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
		bindService(new Intent(this,CustomRtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);

	}

	@Override
	public void onStop() {
		super.onStop();
		// A WakeLock should only be released when isHeld() is true !
		if (mWakeLock.isHeld()) mWakeLock.release();
		if (mHttpServer != null) mHttpServer.removeCallbackListener(mHttpCallbackListener);
		unbindService(mHttpServiceConnection);
		if (mRtspServer != null) mRtspServer.removeCallbackListener(mRtspCallbackListener);
		unbindService(mRtspServiceConnection);
	}

	@Override
	public void onResume() {
		super.onResume();
		mApplication.applicationForeground = true;
	}

	@Override
	public void onPause() {
		super.onPause();
		mApplication.applicationForeground = false;
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

	private void quitSpydroid() {
		// Removes notification
		if (mApplication.notificationEnabled) removeNotification();       
		// Kills HTTP server
		this.stopService(new Intent(this,CustomHttpServer.class));
		// Kills RTSP server
		this.stopService(new Intent(this,CustomRtspServer.class));
		// Returns to home menu
		finish();
	}
	
	private ServiceConnection mRtspServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mRtspServer = (CustomRtspServer) ((RtspServer.LocalBinder)service).getService();
			mRtspServer.addCallbackListener(mRtspCallbackListener);
			mRtspServer.start();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {}

	};

	private RtspServer.CallbackListener mRtspCallbackListener = new RtspServer.CallbackListener() {

		@Override
		public void onError(RtspServer server, Exception e, int error) {
			// We alert the user that the port is already used by another app.
			if (error == RtspServer.ERROR_BIND_FAILED) {
				new AlertDialog.Builder(SpydroidActivity.this)
				.setTitle(R.string.port_used)
				.setMessage(getString(R.string.bind_failed, "RTSP"))
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						startActivityForResult(new Intent(SpydroidActivity.this, OptionsActivity.class),0);
					}
				})
				.show();
			}
		}

		@Override
		public void onMessage(RtspServer server, int message) {
			if (message==RtspServer.MESSAGE_STREAMING_STARTED) {
				if (mAdapter != null && mAdapter.getHandsetFragment() != null) 
					mAdapter.getHandsetFragment().update();
			} else if (message==RtspServer.MESSAGE_STREAMING_STOPPED) {
				if (mAdapter != null && mAdapter.getHandsetFragment() != null) 
					mAdapter.getHandsetFragment().update();
			}
		}

	};	

	private ServiceConnection mHttpServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mHttpServer = (CustomHttpServer) ((TinyHttpServer.LocalBinder)service).getService();
			mHttpServer.addCallbackListener(mHttpCallbackListener);
			mHttpServer.start();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {}

	};

	private TinyHttpServer.CallbackListener mHttpCallbackListener = new TinyHttpServer.CallbackListener() {

		@Override
		public void onError(TinyHttpServer server, Exception e, int error) {
			// We alert the user that the port is already used by another app.
			if (error == TinyHttpServer.ERROR_HTTP_BIND_FAILED ||
					error == TinyHttpServer.ERROR_HTTPS_BIND_FAILED) {
				String str = error==TinyHttpServer.ERROR_HTTP_BIND_FAILED?"HTTP":"HTTPS";
				new AlertDialog.Builder(SpydroidActivity.this)
				.setTitle(R.string.port_used)
				.setMessage(getString(R.string.bind_failed, str))
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						startActivityForResult(new Intent(SpydroidActivity.this, OptionsActivity.class),0);
					}
				})
				.show();
			}
		}

		@Override
		public void onMessage(TinyHttpServer server, int message) {
			if (message==CustomHttpServer.MESSAGE_STREAMING_STARTED) {
				if (mAdapter != null && mAdapter.getHandsetFragment() != null) 
					mAdapter.getHandsetFragment().update();
				if (mAdapter != null && mAdapter.getPreviewFragment() != null)	
					mAdapter.getPreviewFragment().update();
			} else if (message==CustomHttpServer.MESSAGE_STREAMING_STOPPED) {
				if (mAdapter != null && mAdapter.getHandsetFragment() != null) 
					mAdapter.getHandsetFragment().update();
				if (mAdapter != null && mAdapter.getPreviewFragment() != null)	
					mAdapter.getPreviewFragment().update();
			}
		}

	};

	private void removeNotification() {
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
	}

	public void log(String s) {
		Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
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

}