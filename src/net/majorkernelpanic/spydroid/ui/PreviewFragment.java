package net.majorkernelpanic.spydroid.ui;

import net.majorkernelpanic.spydroid.R;
import net.majorkernelpanic.streaming.Session;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

public class PreviewFragment extends Fragment {

	public final static String TAG = "PreviewFragment";
	
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.preview,container,false);
    	mSurfaceView = (SurfaceView)rootView.findViewById(R.id.cameraview);
    	return rootView;
    }

    @Override
    public void onStart() {
    	super.onStart();
    	mSurfaceHolder = mSurfaceView.getHolder();
        Session.setSurfaceHolder(mSurfaceHolder, true);
    }
    
}
