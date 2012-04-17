/*
 * Copyright (C) 2011 GUIGUI Simon, fyhertz@gmail.com
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

package net.majorkernelpanic.spydroid;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

/*
 * Main activity
 * It will also test H.264 support on the phone
 */

public class SpydroidActivity extends Activity {
    
    public ViewGroup topLayout;
    public Button startButton;
    public TextView console;
    public ImageView logo;
    public TextView ipView;
    
    static final public String LOG_TAG = "SPYDROID";
    
    private SharedPreferences settings;
    private String ip;
    private SurfaceView camera;
    private PowerManager.WakeLock wl;
    
    private int resX, resY, fps, br, oldResX, oldResY, oldFps;
    private CameraStreamer streamer = new CameraStreamer();
    
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        ipView = (TextView)findViewById(R.id.ip);
        camera = (SurfaceView)findViewById(R.id.smallcameraview);
        logo = (ImageView)findViewById(R.id.logo);
        topLayout = (ViewGroup) findViewById(R.id.mainlayout);
        console = (TextView) findViewById(R.id.console);
        startButton = (Button) findViewById(R.id.streambutton);
        startButton.setOnClickListener(new OnClickListener() 
        {
        	
        	// Called when pressing the "Stream" button
        	public void onClick(View v) {
        		
        		ip = ipView.getText().toString();
        		
        		SharedPreferences.Editor editor = settings.edit();
        		editor.putString("ip", ip);
        	    editor.commit();
        	    
        	    toggleStreaming();
        		
        	}
        });
   
        logo.setAlpha(100);
        
        settings = getSharedPreferences("spydroid-ipcamera-prefs", 0);
        ip = settings.getString("ip", "192.170.0.1");
       	resX = settings.getInt("resX", 640);
       	resY = settings.getInt("resY", 480);
       	fps = settings.getInt("fps", 15);
       	br = settings.getInt("br", QualityListActivity.DefaultBitRate);
       	oldResX = resX; oldResY = resY; oldFps = fps;
        ((EditText) findViewById(R.id.ip)).setText(ip);
        
        camera.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        Callback shcb = new SurfaceHolder.Callback() {

    		@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {

			}

    		@Override
			public void surfaceCreated(SurfaceHolder holder) {

			}

    		@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
    			stopStreaming();
			}
    		
    	};
    	
    	((SurfaceView)findViewById(R.id.smallcameraview)).getHolder().addCallback(shcb);
    	
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "SpydroidWakeLock");
    	
    }
    
    public void onResume() {
    	super.onResume();
    	
    	console.setText("");
    	log("<b>Hi folks :)</b>");
    	log("Quality is "+String.valueOf(resX)+"x"+String.valueOf(resY)+", "+String.valueOf(fps)+"fps");
    	
    	if (settings.contains("sps") && (oldResX==resX && oldResY==resY && oldFps==fps) ) {
    		// Phone has already been tested
    		log("Phone supported !");
    	    log("Upload /sdcard/spydroid.sdp on your computer");
    	    log("And open it with vlc");
    		return;
    	}

    	// Test H.264 Support
		TestH264.RunTest(this.getCacheDir(),camera.getHolder(), resX, resY, fps, new TestH264.Callback() {
			
			@Override
			public void onStart() {
				log("<b>Testing H.264 Support, please wait...</b>");
			}
			
			// Called if H.264 isn't supported with chosen quality settings
			public void onError(String error) {
				log(error);
				log("Something went wrong, the app may not work properly :/");
			}
			
			// Called if H.264 is supported with chosen quality settings
			public void onSuccess(String result) {

				String[] params = result.split(":");
				
				log("Phone supported ! ");
			    
			    // we have everything we need to generate a proper SDP file
				String sdp = "m=video 5006 RTP/AVP 96\r\n" +
					"b=RR:0\r\n" +
					"a=rtpmap:96 H264/90000\r\n" +
					"a=fmtp:96 packetization-mode=1;profile-level-id="+params[0]+";sprop-parameter-sets="+params[2]+","+params[1]+";\r\n" +
					"m=audio 5004 RTP/AVP 96\r\n" +
					"b=AS:128\r\n" +
					"b=RR:0\r\n" +
					"a=rtpmap:96 AMR/8000\r\n" +
					"a=fmtp:96 octet-align=1\r\n";
			    
			    try {
					
			    	// Store sdp file
					File file = new File("/sdcard/spydroid.sdp");
					RandomAccessFile raf = null;
					raf = new RandomAccessFile(file, "rw");
					raf.writeBytes("v=0\r\ns=Unnamed\r\n");
					raf.writeBytes(sdp);
					raf.close();
			    	
					// Store H264 parameters
		    		SharedPreferences.Editor editor = settings.edit();
		    		editor.putString("profile", params[0]);
		    		editor.putString("pps", params[1]);
		    		editor.putString("sps", params[2]);
		    	    editor.commit();
					
		    	    log("Upload /sdcard/spydroid.sdp on your computer");
		    	    log("And open it with vlc");
					
				} catch (IOException e) {
					log("Could not save sdp file to /sdcard/spydroid.sdp");
				}
				
				oldResX = resX; oldResY = resY; oldFps = fps;
				
			}
			
		});

    }
    
    private void toggleStreaming() {
    	
    	if (streamer.isStreaming())
    		stopStreaming();
    	else
    		startStreaming();

    }
    
    private void startStreaming() {
    	
    	if (streamer.isStreaming()) return;
    	
		try {
			streamer.setup(camera.getHolder(),ip, resX, resY, fps, br);
		} catch (IOException e) {
			log(e.getMessage());
			return;
		}

		streamer.start();
		ipView.setEnabled(false);
		startButton.setText("Stop");
		wl.acquire();
		
    }
    
    private void stopStreaming() {
    	
    	if (!streamer.isStreaming()) return;
    	
		streamer.stop();
		startButton.setText("Stream");
		ipView.setEnabled(true);
		wl.release();
		
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.quality:
        	// Starts QualityListActivity where user can change the streaming quality
    		Intent intent = new Intent(this.getBaseContext(),QualityListActivity.class);
    		intent.putExtra("net.majorkernelpanic.spydroid.resX", resX );
    		intent.putExtra("net.majorkernelpanic.spydroid.resY", resY );
    		intent.putExtra("net.majorkernelpanic.spydroid.fps", fps );
    		intent.putExtra("net.majorkernelpanic.spydroid.br", br );
    		startActivityForResult(intent, 0);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
    	// User has changed quality
    	if (resultCode==1000) {

    		resX = data.getIntExtra("net.majorkernelpanic.spydroid.resX", 0);
    		resY = data.getIntExtra("net.majorkernelpanic.spydroid.resY", 0);
    		fps = data.getIntExtra("net.majorkernelpanic.spydroid.fps", 0);
    		br = data.getIntExtra("net.majorkernelpanic.spydroid.br", 0);

    		SharedPreferences.Editor editor = settings.edit();
    		editor.putInt("resX", resX);
    		editor.putInt("resY", resY);
    		editor.putInt("fps", fps);
    		editor.putInt("br", br);
    	    editor.commit();
    		
    	}
    	
    }
    
    public void log(String s) {
    	console.append(Html.fromHtml(s+"<br />"));
    }
    
    
}