package com.klouddata.push.pushdemoandroid;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

public class SmpPushFragment extends Fragment {
    protected String TAG;
    protected View view;
    protected SmpPushAcitivty mActivity;
    protected SmpPushApplication mApp;
    protected FragmentManager mFragmentManager;

    public SmpPushFragment() {
        TAG = this.getClass().getSimpleName();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (SmpPushAcitivty) getActivity();
        mApp = (SmpPushApplication) mActivity.getApplication();
        mFragmentManager = getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
