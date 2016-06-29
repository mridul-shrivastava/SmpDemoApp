package com.klouddata.push.pushdemoandroid.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.klouddata.push.pushdemoandroid.SmpPushAcitivty;
import com.klouddata.push.pushdemoandroid.util.Util;
import com.sap.maf.tools.logon.core.LogonCore;
import com.sap.maf.tools.logon.core.LogonCoreException;
import com.sap.maf.tools.logon.logonui.api.LogonListener;
import com.sap.maf.tools.logon.logonui.api.LogonUIFacade;
import com.sap.maf.tools.logon.manager.LogonContext;
import com.sap.maf.tools.logon.manager.LogonManager;
import com.sap.maf.utilities.logger.MAFLogger;

//Reference link - http://scn.sap.com/community/developer-center/mobility-platform/blog/2015/04/28/customizing-maf-logon-component-in-android
public class MAFLogonActivity extends SmpPushAcitivty implements LogonListener {

    private LogonUIFacade mLogonUIFacade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);// set context reference
        doLogon();
    }

    @Override
    public void onLogonFinished(String s, boolean b, LogonContext logonContext) {
        String TAG = MAFLogonActivity.class.getSimpleName();
        Log.d(TAG, "onLogonFinished: " + s);
//Check if it finished successfully
        if (b) {
            try {
//For debugging purposes will log the app connection id and
// the end point url.
// In a productive app, remember to remove these logs
                String appConnID = LogonCore.getInstance().getLogonContext()
                        .getConnId();
                Log.d(TAG, "onLogonFinished: appcid:" + appConnID);
                Log.d(TAG, "onLogonFinished: endpointurl:" +
                        logonContext.getEndPointUrl());
            } catch (LogonManager.LogonManagerException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            } catch (LogonCoreException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
// Navigate to the Main menu screen
            Intent goToNextActivity = new Intent(this, MainActivity.class);
            startActivity(goToNextActivity);
            finish();
        }
    }

    @Override
    public void onSecureStorePasswordChanged(boolean b, String s) {

    }

    @Override
    public void onBackendPasswordChanged(boolean b) {

    }

    @Override
    public void onUserDeleted() {

    }

    @Override
    public void onApplicationSettingsUpdated() {

    }

    @Override
    public void registrationInfo() {

    }

    @Override
    public void objectFromSecureStoreForKey() {

    }

    @Override
    public void onRefreshCertificate(boolean b, String s) {

    }

    private void doLogout() {
        //delete registered user
        mLogonUIFacade.deleteUser();
        //restart logon process
        doLogon();
    }

    private void doLogon() {

        // Hide MobilePlace window
        hideMobilePlaceWindow();
        // get an instance of the LogonUIFacade
        if (mLogonUIFacade == null) {
            mLogonUIFacade = LogonUIFacade.getInstance();
        }
        // init LogonUIFacede
        mLogonUIFacade.init(this, this, Util.APP_ID);
        //Set Default values
        setDefaultValuesForLogon();

        // Hide Login details
        hideLoginDetails();
        // set the resulting view as the content view for this activity
        setContentView(mLogonUIFacade.logon());
        // Hide splash screen
        mLogonUIFacade.showSplashScreen(false);
    }

    private void setDefaultValuesForLogon() {
        mLogonUIFacade.setDefaultValue(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_SUPSERVERURL, Util.HOST_NAME);
        mLogonUIFacade.setDefaultValue(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_SUPSERVERPORT, "8080");
//type "true" if the mobile client is using HTTPS, type "false" otherwise
        mLogonUIFacade.setDefaultValue(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_HTTPSSTATUS, "false");
//        MAFLogger.clearLogData();
        //MAFLogger.logToAndroid(true);
    }

    private void hideMobilePlaceWindow() {
        SharedPreferences prefs = getSharedPreferences(LogonCore.PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor pEditor = prefs.edit();
        pEditor.putBoolean(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_MOBILEPLACE.toString(), false);
        pEditor.commit();
    }

    private void hideLoginDetails() {
        mLogonUIFacade.isFieldHidden(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_SUPSERVERURL, true);
        mLogonUIFacade.isFieldHidden(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_SUPSERVERPORT, true);
        mLogonUIFacade.isFieldHidden(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_SECCONFIG, true);
        mLogonUIFacade.isFieldHidden(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_SUPSERVERFARMID, true);
        mLogonUIFacade.isFieldHidden(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_URLSUFFIX, true);
        mLogonUIFacade.isFieldHidden(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_MOBILEUSER, true);
        mLogonUIFacade.isFieldHidden(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_ACTIVATIONCODE, true);
        mLogonUIFacade.isFieldHidden(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_HTTPSSTATUS, true);
        mLogonUIFacade.isFieldHidden(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_GATEWAYCLIENT, true);
        mLogonUIFacade.isFieldHidden(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_SUPSERVERDOMAIN, true);
        mLogonUIFacade.isFieldHidden(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_PINGPATH, true);
        mLogonUIFacade.isFieldHidden(LogonCore.SharedPreferenceKeys.PREFERENCE_ID_GWONLY, true);
    }
}
