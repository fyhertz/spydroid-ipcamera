/*
 * Copyright (C) 2011 GUIGUI Simon fyhertz@gmail.com
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

package net.mkp.spydroid;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/*
 * The activity that is displayed when clicking on the stream button
 */

public class SecondActivity  extends Activity implements SurfaceHolder.Callback {

    private CameraStreamer streamer = null;
	private PowerManager.WakeLock wl;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video);
    }
	
    public void onStart() {
    	super.onStart();
    	
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "SpydroidWakeLock");
    	
    	SurfaceView cv = (SurfaceView) findViewById(R.id.cameraview);
    	SurfaceHolder sh = cv.getHolder();
    	
    	sh.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    	sh.addCallback(this);
    	
		streamer = new CameraStreamer();
    }
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format,
			int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
		String ip = getIntent().getStringExtra("ip");
		
	    // Ensures that the screen stays on
		wl.acquire();
		
		try {
			streamer.setup(holder,ip);
		} catch (IOException e) {
			// Catch error if any and display message
		}
		streamer.start();				
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		 wl.release();
		 streamer.stop();
	}
	
}
