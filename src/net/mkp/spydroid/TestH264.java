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

package net.mkp.spydroid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import net.mkp.libmp4.MP4Parser;
import net.mkp.libmp4.StsdBox;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;
import android.view.SurfaceHolder;

/* 
 * The purpose of this class is to test H.264 support on the phone
 * and to find H.264 pps ans sps parameters. They are needed to 
 * correctly decode the stream.
 * 
 */

public class TestH264 extends AsyncTask<SurfaceHolder, Integer, String> {

	/* Launches the test */
	public static void RunTest(File cacheDir,SurfaceHolder holder, Callbacks cbs) {

		test.cbs = cbs;
		test.cacheDir = cacheDir;
		
    	holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    	holder.addCallback(new SurfaceHolder.Callback() {

    		@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
    			
			}

    		@Override
			public void surfaceCreated(SurfaceHolder holder) {
    			test.execute(holder);
			}

    		@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
    			test.cancel(true);
			}
    		
    	});
		
	}
	
	/* If test succesful, onSuccess is called and provides H264 settings on the phone  */
	public interface Callbacks {
		public void onError(String error);
		public void onSuccess(String result);
	}
	
	private final String TESTFILE = "spydroid-test.mp4";
	
	private static TestH264 test = new TestH264();
	private MediaRecorder mr = new MediaRecorder();
	private Callbacks cbs;
	private File cacheDir;
	
	private TestH264() { }

	@Override
	protected String doInBackground(SurfaceHolder... holder) {
		
		/* 1 - Set up MediaRecorder */
		mr.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mr.setVideoFrameRate(15);
		mr.setVideoSize(640, 480);
		mr.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		mr.setPreviewDisplay(holder[0].getSurface());

		mr.setOutputFile(cacheDir.getPath()+'/'+TESTFILE);
		
		try {
			mr.prepare();
		} catch (IOException e) {
			return "Can't record video, H.264 not supported ?";
		}
		
		/* 2 - Record dummy video for 2 secs */
		mr.start();
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			return "Interrupted exception";
		}
		
		mr.stop();
		
		/* 3 - Parse video with MP4Parser */
		File file = new File(cacheDir.getPath()+'/'+TESTFILE);
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException e1) {
			return "Can't load dummy video";
		}
		
		MP4Parser parser = null;
		try {
			parser = new MP4Parser(raf);
		} catch (IOException e2) {
			return e2.getMessage();
		}
		
		/* 4 - Get stsd box (contains h.264 parameters) */
		StsdBox stsd = null;
		try {
			stsd = parser.getStsdBox();
		} catch (IOException e1) {
			return e1.getMessage();
		}
		
		try {
			raf.close();
		} catch (IOException e) {
			return "Error :(";
		}

		if (!file.delete()) Log.e(SpydroidActivity.LOG_TAG,"Temp file not erased");
		
		return "Success:"+stsd.getProfileLevel()+":"+stsd.getB64PPS()+":"+stsd.getB64SPS();
		
	}
	
	protected void onPostExecute(String result) {
		if (result.startsWith("Success")) {
			cbs.onSuccess(result);
		}
		else cbs.onError(result);
	}
	
	protected void onCancelled(Object obj) {
		cbs.onError("Test cancelled :(");
	}
	
	
}
