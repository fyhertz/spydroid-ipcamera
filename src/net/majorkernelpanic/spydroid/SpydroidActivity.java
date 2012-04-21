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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import net.majorkernelpanic.streaming.RtspServer;
import net.majorkernelpanic.streaming.TestH264;
import net.majorkernelpanic.streaming.VideoQuality;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 
 * Main activity
 * It will test H.264 support on the phone and then launch the RTSP Server
 * 
 */
public class SpydroidActivity extends Activity {
    
    static final public String TAG = "SPYDROID";
    
    public ViewGroup topLayout;
    public TextView console;
    public ImageView logo;
    private SharedPreferences settings;
    private SurfaceView camera;
    private SurfaceHolder holder;
    private int resX, resY, fps, br;
    
    private static RtspServer rtspServer;
    
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        camera = (SurfaceView)findViewById(R.id.smallcameraview);
        logo = (ImageView)findViewById(R.id.logo);
        topLayout = (ViewGroup) findViewById(R.id.mainlayout);
        console = (TextView) findViewById(R.id.console);
   
        logo.setAlpha(100);
        
        settings = getSharedPreferences("spydroid-ipcamera-prefs", 0);
       	resX = settings.getInt("resX", 640);
       	resY = settings.getInt("resY", 480);
       	fps = settings.getInt("fps", 15);
       	br = settings.getInt("br", 1000);
       	
        camera.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder = camera.getHolder();
		
        startRtspServer();
    
    }
    
    public void onResume() {
    	super.onResume();
    	
    	String s = "";
		int n = 0; 
    	
		// Clean console
		console.setText("");
		
    	// Print version number
        try {
			log("<b>Spydroid v"+this.getPackageManager().getPackageInfo(this.getPackageName(), 0 ).versionName+"</b>");
		} catch (NameNotFoundException e) {
			log("<b>Spydroid</b>");
		}
        
		// Print the device ip address
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface i = en.nextElement();
				// We skip the "lo" interface (127.0.0.1)
				if (!i.getName().equals("lo")) {
					for (Enumeration<InetAddress> al = i.getInetAddresses(); al.hasMoreElements();) {
						InetAddress nextElement = al.nextElement();
						s+="<br />rtsp://"+nextElement.getHostAddress()+":8086/";
						n++;
					}
				}
			}
		} catch (SocketException e) {
		} catch (NullPointerException e) {}
    	
		if (n>1) log("Launch VLC and try opening one of the following stream:"+s);
		else if (n>0) log("Launch VLC and open the following stream:"+s); 
		else log("You don't seem to be connected to any network :(. Is the wifi on ?");
		

    }
    
    // The Handler that gets information back from the RtspServer
    private final Handler handler = new Handler() {
    	
    	public void handleMessage(Message msg) {
    		
    		switch (msg.what) {
    		
    		// We test H264 support before starting streaming
    		case RtspServer.MESSAGE_H264_TEST:
    			log("Testing H264 support: "+resX+"x"+resY+","+fps+" fps");
    			TestH264.RunTest(SpydroidActivity.this.getCacheDir(),camera.getHolder(), resX, resY, fps, new TestH264.Callback() {
    				public void onError(String error) {
    					log(error);
    					log("Something went wrong !");
    				}
    				public void onSuccess(String result) {
    					log("H264 supported !");
    		    		// The test returns sps and pps parameters, the client needs them to decode the stream
    					VideoQuality videoQuality = new VideoQuality(resX, resY, fps, br*1000);
    					rtspServer.h264TestResult(videoQuality, result.split(":"), holder);
    				}
    			});
    			
    			break;
    			
    		case RtspServer.MESSAGE_LOG:
    			log((String)msg.obj);
    			break;
    		
    		}
    		
    	}
    	
    };
    
    public void startRtspServer() {
    	rtspServer = new RtspServer(8086, handler);
    	rtspServer.start();
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
    	String t = console.getText().toString();
    	if (t.split("\n").length>8) {
    		console.setText(t.substring(t.indexOf("\n")+1, t.length()));
    	}
    	console.append(Html.fromHtml(s+"<br />"));
    }
    
    
}