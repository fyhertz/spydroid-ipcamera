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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.VideoView;

/** 
 * Read the stream from another phone running spydroid !
 * Not ready yet, obvioulsy :) 
 **/
public class ClientActivity extends Activity implements OnCompletionListener, OnPreparedListener {

	private final static String TAG = "ClientActivity";

	private SharedPreferences settings;
	
	private EditText editTextIP; 
	private Button buttonConnection;
	private MyVideoView videoView;
	private RelativeLayout form; 
	private FrameLayout container;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.client);
     
        editTextIP = (EditText)findViewById(R.id.server_ip);
        buttonConnection = (Button)findViewById(R.id.button_connect);
        form = (RelativeLayout)findViewById(R.id.control);
        container = (FrameLayout)findViewById(R.id.video_container);

        videoView = new MyVideoView(this);
        videoView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.FILL_PARENT));
        container.addView(videoView);
        
        //MediaController mediaController = new MediaController(ClientActivity.this);
        
        //	videoView.setMediaController(mediaController);
        videoView.setOnPreparedListener(this);
        videoView.setOnCompletionListener(this);
        
        buttonConnection.setOnClickListener(new OnClickListener() {
        	@Override
			public void onClick(View v) {
        		connectToServer();
			}
		});

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        editTextIP.setText(settings.getString("last_server_ip", "192.168.0.107"));
        
    }
    
	public void connectToServer() {
		Editor editor = settings.edit();
		editor.putString("last_server_ip", editTextIP.getText().toString());
		editor.commit();
		videoView.setVideoURI(Uri.parse("rtsp://"+editTextIP.getText().toString()+":8086"));
		videoView.requestFocus();
	}
	
    @Override
    public void onStart() {
    	super.onStart();
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    }

	@Override
	public void onCompletion(MediaPlayer mp) {
		form.setVisibility(View.VISIBLE);
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		form.setVisibility(View.GONE);
		videoView.start();
	}
	
	static class MyVideoView extends VideoView {
		public MyVideoView(Context context) {
			super(context);
		}
		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			int width = getDefaultSize(0, widthMeasureSpec);
			int height = getDefaultSize(0, heightMeasureSpec);
			setMeasuredDimension(width, height);
		}
	}
	
}

