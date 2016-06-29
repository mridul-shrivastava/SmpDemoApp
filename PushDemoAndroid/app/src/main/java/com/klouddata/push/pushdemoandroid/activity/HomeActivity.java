package com.klouddata.push.pushdemoandroid.activity;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.klouddata.push.pushdemoandroid.R;
import com.klouddata.push.pushdemoandroid.SmpPushAcitivty;
import com.klouddata.push.pushdemoandroid.callback.ResponseCallBack;
import com.klouddata.push.pushdemoandroid.fragment.dialog.RegisterDialogFragment;
import com.klouddata.push.pushdemoandroid.services.GcmManager;
import com.klouddata.push.pushdemoandroid.util.Util;
import com.sap.maf.tools.logon.core.LogonCore;
import com.sap.maf.tools.logon.core.LogonCoreContext;
import com.sap.maf.tools.logon.core.LogonCoreException;
import com.sap.maf.tools.logon.core.LogonCoreListener;
import com.sap.mobile.lib.request.IResponse;
import com.sap.smp.rest.ClientConnection;
import com.sap.smp.rest.SMPClientListeners;
import com.sap.smp.rest.SMPException;
import com.sap.smp.rest.UserManager;
import com.sybase.persistence.DataVault;


public class HomeActivity extends SmpPushAcitivty implements LogonCoreListener, SMPClientListeners.ISMPUserRegistrationListener, View.OnClickListener, RegisterDialogFragment.OnFragmentInteractionListener {
    private Button registerButton;
    private Button registerGcm;
    private Button createEntity;
    private Button registerViaSdk;
    private TextView mGcmIdText;
    private LogonCore logonCore;
    private boolean isHttpsEnabled = false;
    private ResponseCallBack mCallBackListener;
    private GcmManager mManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        init();
        try {
            initLogonCore();
        } catch (LogonCoreException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        initViews();
        initListener();
        initGcmManager();
    }

    private void initViews() {
        registerButton = (Button) findViewById(R.id.register_button);
        registerGcm = (Button) findViewById(R.id.register_gcm);
        createEntity = (Button) findViewById(R.id.create_entity);
        mGcmIdText = (TextView) findViewById(R.id.gcm_id);
        registerViaSdk = (Button) findViewById(R.id.register_through_sdk_button);
    }

    private void initListener() {
        registerButton.setOnClickListener(this);
        registerGcm.setOnClickListener(this);
        createEntity.setOnClickListener(this);
        registerViaSdk.setOnClickListener(this);
        mCallBackListener = new ResponseCallBack() {
            @Override
            public void onGcmRegistered(String regisId) {
                hideLoading();
                registerGcm.setText("Unregister GCM");
                mGcmIdText.setVisibility(View.VISIBLE);
                mGcmIdText.setText(regisId);
                createEntity.setVisibility(View.VISIBLE);
            }

            @Override
            public void onGcmRegistratioFailed(String msg) {
                hideLoading();
                Toast.makeText(HomeActivity.this, msg,
                        Toast.LENGTH_LONG).show();

            }

            @Override
            public void onEntityCreated(IResponse response) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        hideLoading();
                        createEntity.setText("Entity Created");
                        createEntity.setEnabled(false);
                        createEntity.setClickable(false);
                        AlertDialog alertDialog = new AlertDialog.Builder(HomeActivity.this).create();
                        alertDialog.setTitle("Success");
                        alertDialog.setMessage("A new subscription has created successfully.");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                    }
                });
            }

            @Override
            public void onEntityCreationFailed(IResponse response) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        hideLoading();
                        AlertDialog alertDialog = new AlertDialog.Builder(HomeActivity.this).create();
                        alertDialog.setTitle("Fail");
                        alertDialog.setMessage("A new subscription has not created, Try again.");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                    }
                });
            }
        };
    }

    private void initGcmManager() {
        mManager = new GcmManager(this, mCallBackListener);
        if (GcmManager.checkPlayServices()) {
            String regid = GcmManager.getRegistrationId(this);
            if (!regid.isEmpty()) {
                registerGcm.setVisibility(View.VISIBLE);
                mGcmIdText.setVisibility(View.VISIBLE);
                registerGcm.setText("Unregister GCM");
                mGcmIdText.setText(regid);
                createEntity.setVisibility(View.VISIBLE);
            }
        }
    }


    private void initLogonCore() throws LogonCoreException {
        logonCore = LogonCore.getInstance();
        logonCore.init(this, Util.APP_ID);
        LogonCoreContext lgCtx = logonCore.getLogonContext();
        String appConnID = lgCtx.getConnId();
        if (appConnID != null && appConnID.length() > 0) {
            registerButton.setText("Unregister");
        } else {
            logonCore.init(this, Util.APP_ID);
            logonCore.setLogonCoreListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_button:
                if (registerButton.getText().toString().equalsIgnoreCase("Register User"))
                    showRegistrationFragent();
                else
                    unRegisterUser();
                break;
            case R.id.register_gcm:
                if (registerGcm.getText().toString().equalsIgnoreCase("Register GCM"))
                    registerDeviceWithGcm();
                else
                    unRegisterDeviceWithGcm();
                break;
            case R.id.create_entity:
                showLoading();
                createEntityAtServerDb();
                break;
            case R.id.register_through_sdk_button:
                showLoading();
                registerThroughSdk();
                break;
            default:
                break;
        }
    }

    private void showRegistrationFragent() {
        FragmentManager fragmentManager = getFragmentManager();
        RegisterDialogFragment registerDialogFragment = new RegisterDialogFragment();
        registerDialogFragment.show(fragmentManager, "RegisterDialogFragment");
    }

    @Override
    public void onRegisterPressed(String userName, String password) {
        showLoading();
        logonCore = LogonCore.getInstance();
        LogonCoreContext logonCoreContext = logonCore.getLogonContext();
        logonCoreContext.setHost(Util.HOST_NAME); //please input the correct IP address here
        int port = 8080;
        logonCoreContext.setPort(port);
        logonCoreContext.setHttps(isHttpsEnabled);
        LogonCore.UserCreationPolicy ucPolicy = LogonCore.UserCreationPolicy.automatic;
        logonCoreContext.setUserCreationPolicy(ucPolicy);
        try {
            logonCoreContext.setBackendUser(userName); // please note: here we input SMP cockpit logon username, NOT backend username
            logonCoreContext.setBackendPassword(password); // please note: here we input SMP cockpit logon pwd, NOT backend pwd
        } catch (LogonCoreException e) {
            Log.e(TAG, "error entering user credentials", e);
        }
        logonCore.register(logonCoreContext);
        Log.i(TAG, "after register");
    }

    @Override
    public void registrationFinished(boolean success, String message, int errorCode,
                                     DataVault.DVPasswordPolicy arg3) {
        if (success) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    registerButton.invalidate();
                    registerButton.setText("Unregister");
                    enableDisableControls(true);
                }
            });
        }
        hideLoading();
        Log.i(TAG, "registrationFinished, success? " + success); //If you get “success” in console output, go to cockpit/applictions/click on your application id, you will see if a new registration already created!
        Log.i(TAG, "registrationFinished, message= " + message);
        Log.i(TAG, "registrationFinished, errorCode= " + errorCode);
    }

    @Override
    public void deregistrationFinished(boolean b) {
        Toast.makeText(this, "deregistrationFinished", Toast.LENGTH_LONG).show();
    }

    @Override
    public void backendPasswordChanged(boolean b) {
        Toast.makeText(this, "backendPasswordChanged", Toast.LENGTH_LONG).show();
    }

    @Override
    public void applicationSettingsUpdated() {
        Toast.makeText(this, "applicationSettingsUpdated", Toast.LENGTH_LONG).show();

    }

    @Override
    public void traceUploaded() {

    }

    private void unRegisterUser() {

    }

    private void enableDisableControls(boolean isEnnable) {
        if (isEnnable) {
            registerGcm.setVisibility(View.VISIBLE);
        }

    }

    private void registerDeviceWithGcm() {
        try {
            // Check device for Play Services APK. If check succeeds,
            // proceed with GCM registration.
            Context context = getApplicationContext();
            if (GcmManager.checkPlayServices()) {
                String regid = GcmManager.getRegistrationId(context);
                if (regid.isEmpty()) {
                    showLoading();
                    mManager.registerInBackground();
                } else {
                    Log.d("MainActivity", "No valid Google Play Services APK found.");
                }
            }
        } catch (Exception e) {
        }
    }

    private void unRegisterDeviceWithGcm() {
        try {

        } catch (Exception e) {
        }
    }

    private void createEntityAtServerDb() {
        mManager.createServerEntity();
    }

    private void registerThroughSdk(){
        LogonCoreContext lgCtx = LogonCore.getInstance().getLogonContext();
        String host = lgCtx.getHost();
        String port = "8080";
        try {
            port = Integer.valueOf(lgCtx.getPort()).toString();
        } catch (NumberFormatException e) {
            Log.e(TAG, "sendRegistrationIdToBackend: Invalid port value, "
                    + "default (8080) is being used.", e);
        } //Create client connection
        ClientConnection clientConnection =
                new ClientConnection(getApplicationContext(),
                        Util.APP_ID,
                        null, //Domain (only for backward compatibility)
                        null, //Security Configuration (only for backward compatibility)
                        mManager.getReqManager());
        clientConnection.setConnectionProfile(true, host, port, null, null);
        //Create user manager and start the register request
        UserManager userManager = new UserManager(clientConnection);
        userManager.setUserRegistrationListener(this);
        try {
            userManager.registerUser(false); //false for async request
        } catch (SMPException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAsyncRegistrationResult(State state, ClientConnection clientConnection, int i, String s) {
        Log.i("result", s);
        hideLoading();
    }
}
