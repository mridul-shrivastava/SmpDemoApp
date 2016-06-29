package com.klouddata.push.pushdemoandroid.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.klouddata.push.pushdemoandroid.R;
import com.klouddata.push.pushdemoandroid.SmpPushAcitivty;
import com.klouddata.push.pushdemoandroid.callback.MyLogonCoreListener;
import com.klouddata.push.pushdemoandroid.callback.UIListener;
import com.klouddata.push.pushdemoandroid.exception.GcmException;
import com.klouddata.push.pushdemoandroid.util.Util;
import com.sap.maf.tools.logon.core.LogonCore;
import com.sap.maf.tools.logon.core.LogonCoreContext;
import com.sap.maf.tools.logon.core.LogonCoreException;

public class LoginActivity extends SmpPushAcitivty implements View.OnClickListener, UIListener {
    private final String TAG = LoginActivity.class.getSimpleName();
    private static final String VK_APPCID = "appcid";
    private Button logonBtn, mafLoagonBtn;
    private EditText hostEdit, portEdit, usernameEdit, passwordEdit;
    private ProgressDialog progressDialog;
    private String appConnId;
    private LogonCore lgCore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //Initialize UI elements in the screen
        this.initializeViews();
        //Get application connection id
        this.initializeLogonCore();
        //If application connection id exists, then display main screen
        if (!TextUtils.isEmpty(appConnId)) {
            Intent goToNextActivity = new Intent(this, MainActivity.class);
            startActivity(goToNextActivity);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.logon_button:
                registerDevice();
                break;
            case R.id.maf_logon_button:
                Intent intent = new Intent(LoginActivity.this, MAFLogonActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    @Override
    public void onODataRequestError(Exception e) {
        progressDialog.dismiss();
        logonBtn.setEnabled(true);
        //notify the user the registration fails
        Toast.makeText(this, R.string.msg_registration_fail, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onODataRequestSuccess(String info) {
        progressDialog.dismiss();
        //Store application connection Id in the secure store This way next time the app runs, we know if the user has been registered before
        try {
            appConnId = mApp.getLgCore().getLogonContext().getConnId();
            mApp.initializeAppSettings();
            if (appConnId != null) {
                // store it
                if (!lgCore.isStoreOpen()) lgCore.unlockStore(null);
                lgCore.addObjectToStore(VK_APPCID, appConnId);
            }
            //notify the user the registration was complete successfully
            Toast.makeText(this, R.string.msg_registration_success, Toast.LENGTH_LONG).show();
            //Display the main screen
            Intent goToNextActivity = new Intent(this, MainActivity.class);
            startActivity(goToNextActivity);
        } catch (LogonCoreException e) {
            Log.e(TAG, "error getting application connection id", e);
            //notify the user the registration fails
            Toast.makeText(this, R.string.msg_registration_fail, Toast.LENGTH_LONG).show();
            logonBtn.setEnabled(true);
        } catch (GcmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize UI elements
     */
    private void initializeViews() {
        logonBtn = (Button) findViewById(R.id.logon_button);
        mafLoagonBtn = (Button) findViewById(R.id.maf_logon_button);
        logonBtn.setOnClickListener(this);
        mafLoagonBtn.setOnClickListener(this);
        hostEdit = (EditText) findViewById(R.id.txt_host);
        portEdit = (EditText) findViewById(R.id.txt_port);
        usernameEdit = (EditText) findViewById(R.id.txt_username);
        passwordEdit = (EditText) findViewById(R.id.txt_password);
        hostEdit.setText(Util.HOST_NAME);
        portEdit.setText("8080");
        usernameEdit.setText(Util.USER_NAME);
        passwordEdit.setText(Util.PASSWORD);
    }

    /**
     * Initialize LogonCore component
     */
    private void initializeLogonCore() {
        //Get LogonCore instance
        lgCore = mApp.getLgCore();
        //Create a LogonCoreListener for asynchronously registration
        MyLogonCoreListener listener = new MyLogonCoreListener(0, this);
        //Set the listener
        lgCore.setLogonCoreListener(listener);
        //Initialize LogonCore with application configuraton name
//        lgCore.init(this, Util.APP_ID);
//        //Check if application connection exists
//        try {
//            //check if secure store is available
//            if (lgCore.isStoreAvailable()) {
//                //Unlock the store
//                lgCore.unlockStore(null);
//                //Get application connection id
//                appConnId = lgCore.getObjectFromStore(VK_APPCID);
//            }
//        } catch (LogonCoreException e) {
//            Log.e(TAG, "error initializing logon core", e);
//        }
    }

    /**
     * Onboard device with Mobile services
     */
    private void registerDevice() {
        logonBtn.setEnabled(false);
        progressDialog = ProgressDialog.show(this, "", getString(R.string.msg_registration_progress), true);
        //Check if the Application Connection Id already exists
        if (TextUtils.isEmpty(appConnId)) {
            //Get LogonCoreContext instance
            LogonCoreContext lgCtx =  lgCore.getLogonContext();
            //Set host
            lgCtx.setHost(hostEdit.getText().toString());
            //Set port
            int port = 8080;
            try {
                port = Integer.valueOf(portEdit.getText().toString());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid port value, default (8080) is taken!");
            }
            lgCtx.setPort(port);
            //Set whether the registration uses secure connection or not
            lgCtx.setHttps(false);
            //set user creation policy
            LogonCore.UserCreationPolicy ucPolicy = LogonCore.UserCreationPolicy.automatic;
            lgCtx.setUserCreationPolicy(ucPolicy);
            //Set username and password
            try {
                lgCtx.setBackendUser(usernameEdit.getText().toString());
                lgCtx.setBackendPassword(passwordEdit.getText().toString());
                lgCtx.setSupUserName(usernameEdit.getText().toString());
            } catch (LogonCoreException e) {
                //Notifies the execution finished
                onODataRequestError(e);
            }//Register user
            lgCore.register(lgCtx);
        } else {
            //This means the user is already registered
            Log.d(TAG, getString(R.string.msg_already_registered));
            //notify the user the device is already regitered
            Toast.makeText(this, R.string.msg_already_registered, Toast.LENGTH_LONG).show();
        }
    }
}

