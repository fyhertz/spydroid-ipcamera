package net.majorkernelpanic.spydroid.ui;

import net.majorkernelpanic.spydroid.R;
import net.majorkernelpanic.spydroid.SpydroidApplication;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

public class AboutFragment extends Fragment {

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.about,container,false);

		Button mButtonVisit = (Button) rootView.findViewById(R.id.visit);
		Button mButtonRate = (Button) rootView.findViewById(R.id.rate);
		Button mButtonLike = (Button) rootView.findViewById(R.id.like);

		mButtonVisit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse("https://code.google.com/p/spydroid-ipcamera/"));
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				startActivity(intent);
			}
		});

		mButtonRate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String appPackageName=SpydroidApplication.getInstance().getApplicationContext().getPackageName();
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appPackageName));
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				startActivity(intent);
			}
		});

		mButtonLike.setOnClickListener(new OnClickListener() {
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
