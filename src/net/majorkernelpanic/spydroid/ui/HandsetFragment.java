package net.majorkernelpanic.spydroid.ui;

import java.util.Locale;

import net.majorkernelpanic.spydroid.R;
import net.majorkernelpanic.spydroid.SpydroidApplication;
import net.majorkernelpanic.spydroid.Utilities;
import android.content.Context;
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

    private TextView line1, line2, version, signWifi, signStreaming;
    private LinearLayout signInformation;
    private Animation pulseAnimation;
	
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.main,container,false);
        line1 = (TextView)rootView.findViewById(R.id.line1);
        line2 = (TextView)rootView.findViewById(R.id.line2);
        version = (TextView)rootView.findViewById(R.id.version);
        signWifi = (TextView)rootView.findViewById(R.id.advice);
        signStreaming = (TextView)rootView.findViewById(R.id.streaming);
        signInformation = (LinearLayout)rootView.findViewById(R.id.information);
        pulseAnimation = AnimationUtils.loadAnimation(SpydroidApplication.getContext(), R.anim.pulse);
        return rootView ;
    }
	
	@Override
    public void onStart() {
    	super.onStart();
    	
    	// Remove the ads if this is the donate version of the app.
        if (SpydroidApplication.DONATE_VERSION) {
        	((LinearLayout)getActivity().findViewById(R.id.adcontainer)).removeAllViews();
        }
    	
    	// Print version number
    	Context mContext = SpydroidApplication.getContext();
        try {
			version.setText("v"+mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0 ).versionName);
		} catch (Exception e) {
			version.setText("v???");
		}
    	
    	displayIpAddress();
    	
    }
    
	public void streamingState(int state) {
		// Not streaming
		if (state==0) {
			signStreaming.clearAnimation();
			signWifi.clearAnimation();
			signStreaming.setVisibility(View.GONE);
			signInformation.setVisibility(View.VISIBLE);
			signWifi.setVisibility(View.GONE);
		} else if (state==1) {
			// Streaming
			signWifi.clearAnimation();
			signStreaming.setVisibility(View.VISIBLE);
			signStreaming.startAnimation(pulseAnimation);
			signInformation.setVisibility(View.INVISIBLE);
			signWifi.setVisibility(View.GONE);
		} else if (state==2) {
			// No wifi !
			signStreaming.clearAnimation();
			signStreaming.setVisibility(View.GONE);
			signInformation.setVisibility(View.INVISIBLE);
			signWifi.setVisibility(View.VISIBLE);
			signWifi.startAnimation(pulseAnimation);
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
	    	line1.setText("http://");
	    	line1.append(ip);
	    	line1.append(":"+SpydroidApplication.HttpPort);
	    	line2.setText("rtsp://");
	    	line2.append(ip);
	    	line2.append(":"+SpydroidApplication.RtspPort);
	    	streamingState(0);
    	} else if((ipaddress = Utilities.getLocalIpAddress(true)) != null) {
    		line1.setText("http://");
	    	line1.append(ipaddress);
	    	line1.append(":"+SpydroidApplication.HttpPort);
	    	line2.setText("rtsp://");
	    	line2.append(ipaddress);
	    	line2.append(":"+SpydroidApplication.RtspPort);
	    	streamingState(0);
    	} else {
      		line1.setText("HTTP://xxx.xxx.xxx.xxx:"+SpydroidApplication.HttpPort);
    		line2.setText("RTSP://xxx.xxx.xxx.xxx:"+SpydroidApplication.HttpPort);
    		streamingState(2);
    	}
    	
    }
	
}
