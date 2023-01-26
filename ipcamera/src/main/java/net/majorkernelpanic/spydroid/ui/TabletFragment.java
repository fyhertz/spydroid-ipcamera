package net.majorkernelpanic.spydroid.ui;

import net.majorkernelpanic.spydroid.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

public class TabletFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tablet, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
