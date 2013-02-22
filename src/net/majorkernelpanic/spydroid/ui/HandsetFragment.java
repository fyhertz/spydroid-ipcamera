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

import java.util.Locale;

import net.majorkernelpanic.spydroid.R;
import net.majorkernelpanic.spydroid.SpydroidApplication;
import net.majorkernelpanic.spydroid.Utilities;
import net.majorkernelpanic.streaming.SessionManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HandsetFragment extends Fragment {

    private TextView mLine1, mLine2, mVersion, mSignWifi, mSignStreaming;
    private LinearLayout mSignInformation;
    private Animation mPulseAnimation;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    }
    
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.main,container,false);
        mLine1 = (TextView)rootView.findViewById(R.id.line1);
        mLine2 = (TextView)rootView.findViewById(R.id.line2);
        mVersion = (TextView)rootView.findViewById(R.id.version);
        mSignWifi = (TextView)rootView.findViewById(R.id.advice);
        mSignStreaming = (TextView)rootView.findViewById(R.id.streaming);
        mSignInformation = (LinearLayout)rootView.findViewById(R.id.information);
        mPulseAnimation = AnimationUtils.loadAnimation(SpydroidApplication.getContext(), R.anim.pulse);
        
        return rootView ;
    }
	
	@Override
    public void onStart() {
    	super.onStart();
    	
    	// Print version number
    	Context mContext = SpydroidApplication.getContext();
        try {
			mVersion.setText("v"+mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0 ).versionName);
		} catch (Exception e) {
			mVersion.setText("v???");
		}
    	
    	displayIpAddress();
    	
    }
    
	@Override
    public void onPause() {
    	super.onPause();
    	if (!SessionManager.getManager().isStreaming()) displayIpAddress(); else streamingState(1); 
    	getActivity().unregisterReceiver(mWifiStateReceiver);
    }
	
	@Override
    public void onResume() {
    	super.onResume();
    	if (!SessionManager.getManager().isStreaming()) displayIpAddress(); else streamingState(1); 
    	getActivity().registerReceiver(mWifiStateReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }
	
	public void streamingState(int state) {
		if (state==0) {
			// Not streaming
			mSignStreaming.clearAnimation();
			mSignWifi.clearAnimation();
			mSignStreaming.setVisibility(View.GONE);
			mSignInformation.setVisibility(View.VISIBLE);
			mSignWifi.setVisibility(View.GONE);
		} else if (state==1) {
			// Streaming
			mSignWifi.clearAnimation();
			mSignStreaming.setVisibility(View.VISIBLE);
			mSignStreaming.startAnimation(mPulseAnimation);
			mSignInformation.setVisibility(View.INVISIBLE);
			mSignWifi.setVisibility(View.GONE);
		} else if (state==2) {
			// No wifi !
			mSignStreaming.clearAnimation();
			mSignStreaming.setVisibility(View.GONE);
			mSignInformation.setVisibility(View.INVISIBLE);
			mSignWifi.setVisibility(View.VISIBLE);
			mSignWifi.startAnimation(mPulseAnimation);
		}
	}
	
    public void displayIpAddress() {
		WifiManager wifiManager = (WifiManager) SpydroidApplication.getContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
		String ipaddress = null;
		Log.d("SpydroidActivity","getNetworkId "+info.getNetworkId());
    	if (info!=null && info.getNetworkId()>-1) {
	    	int i = info.getIpAddress();
	        String ip = String.format(Locale.ENGLISH,"%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff);
	    	mLine1.setText("http://");
	    	mLine1.append(ip);
	    	mLine1.append(":"+SpydroidApplication.sHttpPort);
	    	mLine2.setText("rtsp://");
	    	mLine2.append(ip);
	    	mLine2.append(":"+SpydroidApplication.sRtspPort);
	    	streamingState(0);
    	} else if((ipaddress = Utilities.getLocalIpAddress(true)) != null) {
    		mLine1.setText("http://");
	    	mLine1.append(ipaddress);
	    	mLine1.append(":"+SpydroidApplication.sHttpPort);
	    	mLine2.setText("rtsp://");
	    	mLine2.append(ipaddress);
	    	mLine2.append(":"+SpydroidApplication.sRtspPort);
	    	streamingState(0);
    	} else {
      		mLine1.setText("HTTP://xxx.xxx.xxx.xxx:"+SpydroidApplication.sHttpPort);
    		mLine2.setText("RTSP://xxx.xxx.xxx.xxx:"+SpydroidApplication.sHttpPort);
    		streamingState(2);
    	}
    	
    }
    
    // BroadcastReceiver that detects wifi state changements
    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	// This intent is also received when app resumes even if wifi state hasn't changed :/
        	if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        		if (!SessionManager.getManager().isStreaming()) displayIpAddress();
        	}
        } 
    };
	
}
