package net.majorkernelpanic.spydroid.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import net.majorkernelpanic.http.TinyHttpServer;
import net.majorkernelpanic.spydroid.R;
import net.majorkernelpanic.spydroid.api.CustomHttpServer;
import net.majorkernelpanic.spydroid.api.CustomRtspServer;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

public class PreviewFragment extends Fragment {

    public final static String TAG = "PreviewFragment";

    private TextView mTextView;
    private CustomHttpServer mHttpServer;
    private RtspServer mRtspServer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().unbindService(mHttpServiceConnection);
        requireActivity().unbindService(mRtspServiceConnection);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().bindService(new Intent(requireActivity(), CustomHttpServer.class), mHttpServiceConnection, Context.BIND_AUTO_CREATE);
        requireActivity().bindService(new Intent(requireActivity(), CustomRtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.preview, container, false);

        mTextView = (TextView) rootView.findViewById(R.id.tooltip);

        if (((SpydroidActivity) requireActivity()).device == ((SpydroidActivity) requireActivity()).TABLET) {

            SurfaceView mSurfaceView = (SurfaceView) rootView.findViewById(R.id.handset_camera_view);
            SessionBuilder.getInstance().setSurfaceView(mSurfaceView);
        }

        return rootView;
    }

    public void update() {
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mTextView != null) {
                    if ((mRtspServer != null && mRtspServer.isStreaming()) || (mHttpServer != null && mHttpServer.isStreaming()))
                        mTextView.setVisibility(View.INVISIBLE);
                    else
                        mTextView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private final ServiceConnection mRtspServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRtspServer = (RtspServer) ((RtspServer.LocalBinder) service).getService();
            update();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private final ServiceConnection mHttpServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mHttpServer = (CustomHttpServer) ((TinyHttpServer.LocalBinder) service).getService();
            update();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

}
