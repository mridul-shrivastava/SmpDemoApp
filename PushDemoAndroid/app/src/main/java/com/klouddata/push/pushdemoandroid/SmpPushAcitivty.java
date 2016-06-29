package com.klouddata.push.pushdemoandroid;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.klouddata.push.pushdemoandroid.util.Util;

public class SmpPushAcitivty extends AppCompatActivity {
    protected static final String TAG = SmpPushAcitivty.class.getSimpleName();
    protected SmpPushApplication mApp;
    private ProgressDialog dialog;
    private InputMethodManager mKeyboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMembers();
    }

    private void initMembers() {
        mApp = (SmpPushApplication) getApplication();
        mKeyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    public void hideLoading() {
        try {
            if (dialog != null && dialog.isShowing())
                dialog.dismiss();
        } catch (final IllegalArgumentException e) {
            // Handle or log or ignore
        } catch (final Exception e) {
            // Handle or log or ignore
        }
    }

    public void showLoading() {
        try {
            if (dialog == null) {
                dialog = new ProgressDialog(this);
                dialog.setCancelable(true);
            }
            dialog.setMessage("Please Wait.");
            dialog.setTitle("Loading Data...");
            if (!dialog.isShowing())
                dialog.show();
        } catch (final IllegalArgumentException e) {
            // Handle or log or ignore
        } catch (final Exception e) {
            // Handle or log or ignore
        }
    }

    public void showLoading(String title) {
        if (dialog == null) {
            dialog = new ProgressDialog(this);
            dialog.setCancelable(false);
        }
        dialog.setMessage("Please Wait..");
        if (!Util.isNullOrBlank(title))
            dialog.setTitle(title);
        if (!dialog.isShowing())
            dialog.show();
    }

    public void hideKeyBoard() {
        View view = getCurrentFocus();
        if (view != null) {
            view.clearFocus();
            mKeyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}
