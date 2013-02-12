package net.majorkernelpanic.spydroid.ui;

import net.majorkernelpanic.spydroid.R;
import net.majorkernelpanic.spydroid.SpydroidApplication;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class AboutFragment extends Fragment {

	private Button buttonVisit;
	private Button buttonRate;
	private Button buttonLike;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.about,container,false);
    	
        buttonVisit = (Button)rootView.findViewById(R.id.visit);
        buttonRate = (Button)rootView.findViewById(R.id.rate);
        buttonLike = (Button)rootView.findViewById(R.id.like);
        
        buttonVisit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse("https://code.google.com/p/spydroid-ipcamera/"));
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				startActivity(intent);
			}
		});
        
        buttonRate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String appPackageName=SpydroidApplication.getContext().getPackageName();
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appPackageName));
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				startActivity(intent);
			}
		});
        
        buttonLike.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com/spydroidipcamera"));
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				startActivity(intent);
			}
		}); 
    	
        return rootView ;
    }
	
}
