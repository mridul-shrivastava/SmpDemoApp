package com.klouddata.push.pushdemoandroid.services;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.klouddata.push.pushdemoandroid.activity.HomeActivity;
import com.klouddata.push.pushdemoandroid.callback.ResponseCallBack;
import com.klouddata.push.pushdemoandroid.exception.GcmException;
import com.klouddata.push.pushdemoandroid.util.RequestBuilder;
import com.klouddata.push.pushdemoandroid.util.Util;
import com.sap.maf.tools.logon.core.LogonCore;
import com.sap.maf.tools.logon.core.LogonCoreContext;
import com.sap.maf.tools.logon.core.LogonCoreException;
import com.sap.mobile.lib.configuration.IPreferences;
import com.sap.mobile.lib.configuration.Preferences;
import com.sap.mobile.lib.configuration.PreferencesException;
import com.sap.mobile.lib.parser.ParserException;
import com.sap.mobile.lib.request.ConnectivityParameters;
import com.sap.mobile.lib.request.IRequest;
import com.sap.mobile.lib.request.RequestManager;
import com.sap.mobile.lib.supportability.Logger;
import com.sap.smp.rest.AppSettings;
import com.sap.smp.rest.ClientConnection;
import com.sap.smp.rest.SMPException;

import java.util.HashMap;

public class GcmManager {
    private static final String TAG = GcmManager.class.getSimpleName();
    //GCM Connection Properties
    //With online store
    private static Context context;
    private final static int NUMBER_OF_THREADS = 3;

    public static RequestManager getReqManager() {
        return reqManager;
    }

    private static RequestManager reqManager;

    public static AppSettings getAppSettings() {
        return appSettings;
    }

    private static AppSettings appSettings;
    private static String regid;

    public static String getAppConnID() {
        return appConnID;
    }

    public static void setAppConnID(String appConnID) {
        GcmManager.appConnID = appConnID;
    }

    private static String appConnID;
    private ResponseCallBack mCallBack;

    public GcmManager(Context context) {
        this.context = context;
    }

    public GcmManager(Context context, ResponseCallBack responseCallBack) {
        this.context = context;
        mCallBack = responseCallBack;
//        try {
//            initializeAppSettings();
//        } catch (GcmException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public static boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            return false;
        }
        return true;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId   registration ID
     */
    private static void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Util.PROPERTY_REG_ID, regId);
        editor.putInt(Util.PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p/>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     * registration ID.
     */
    public static String getRegistrationId(Context ctx) {
        context = ctx;
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(Util.PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
// Check if app was updated; if so, it must clear the registration ID
// since the existing regID is not guaranteed to work with the new
// app version.
        int registeredVersion = prefs.getInt(Util.PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
// should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    public static SharedPreferences getGcmPreferences(Context context) {
// This sample app persists the registration ID in shared preferences, but
// how you store the regID in your app is up to you.
        return context.getSharedPreferences(HomeActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p/>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    public void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
                    initializeAppSettings();
                    String senderID = (String) appSettings.getConfigProperty("d:AndroidGcmSenderId");
                    regid = gcm.register(senderID);
                    msg = "Device registered, registration ID=" + regid;
// You should send the registration ID to your server over HTTP, so it
// can use GCM/HTTP or CCS to send messages to your app. sendRegistrationIdToBackend();
// Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (Exception ex) {
                    msg = "Error :" + ex.getMessage();
                    Log.e(TAG, "registerInBackground", ex);
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Toast.makeText(context, msg,
                        Toast.LENGTH_LONG).show();
                if (regid != null && regid.length() > 0) {
                    try {
                        mCallBack.onGcmRegistered(regid);
                        sendRegistrationIdToBackend();
                        //createRegistrationEntity(appEndPoint);
                    } catch (GcmException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.execute(null, null, null);
    }

    /**
     * Get AppSettings
     *
     * @return
     * @throws GcmException
     */
    public void initializeAppSettings() throws GcmException {
        if (appSettings == null) {
//            LogonCore logonCore = LogonCore.getInstance();
//            logonCore.init(context, APP_ID);
//            LogonCoreContext lgCtx = logonCore.getLogonContext();
            LogonCoreContext lgCtx = LogonCore.getInstance().getLogonContext();
            //String host = lgCtx.getHost();
            String host = Util.HOST_NAME;
            String port = "8080";
            try {
                port = Integer.valueOf(lgCtx.getPort()).toString();
            } catch (NumberFormatException e) {
                Log.e(TAG, "sendRegistrationIdToBackend: Invalid port value, "
                        + "default (8080) is being used.", e);
            }
            try {
                appConnID = lgCtx.getConnId();
            } catch (LogonCoreException e) {
                Log.d(TAG, "XCSRFTokenRequestFilter error "
                        + "getting connection id", e);
                throw new GcmException(e);
            }
            if (reqManager == null) {
                Logger logger = new Logger();
//Create Connectivity Parameters
                ConnectivityParameters connectivityParameters = new ConnectivityParameters();
                connectivityParameters.setLanguage(context.getResources()
                        .getConfiguration().locale.getLanguage());
                connectivityParameters.enableXsrf(true);
                try {
                    connectivityParameters.setUserName(lgCtx.getBackendUser());
                    connectivityParameters.setUserPassword(lgCtx.getBackendPassword());
                } catch (LogonCoreException e) {
                    throw new GcmException(e);
                }
//Create Preferences
                Preferences preferences = new Preferences(context, logger);
                try {
                    preferences.setBooleanPreference(IPreferences.PERSISTENCE_SECUREMODE, false);
                    preferences.setIntPreference(IPreferences.CONNECTIVITY_HTTPS_PORT, 443);
                    preferences.setIntPreference(IPreferences.CONNECTIVITY_CONNTIMEOUT, 70000);
                    preferences.setIntPreference(IPreferences.CONNECTIVITY_SCONNTIMEOUT, 70000);
                } catch (PreferencesException e) {
                    Log.d(TAG, e.getLocalizedMessage());
                    throw new GcmException(e);
                }
                reqManager = new RequestManager(logger, preferences,
                        connectivityParameters, NUMBER_OF_THREADS);
            }
            ClientConnection clientConnection =
                    new ClientConnection(context, Util.APP_ID,
                            null, null, reqManager);
            clientConnection.setConnectionProfile(true, host, port,
                    null, null);
            clientConnection.setApplicationConnectionID(appConnID);
            appSettings = new AppSettings(clientConnection);
        }
    }

    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     *
     * @throws GcmException
     */
    private void sendRegistrationIdToBackend() throws GcmException {
        initializeAppSettings();
        HashMap<String, String> fullConfig = new HashMap<String, String>();
        fullConfig.put(Util.PROPERTY_GCM_REGISTRATION_ID, regid);
        try {
            appSettings.setConfigProperty(fullConfig);
//            createRegistrationAtServer();
        } catch (SMPException e) {
            Log.e(TAG, "sendRegistrationIdToBackendOld", e);
            throw new GcmException(e);
        }
    }

    public void createServerEntity(){

        String appEndPoint = null;
        try {
            initializeAppSettings();
            appEndPoint = appSettings.getApplicationEndPoint();
            RequestBuilder requestBuilder = RequestBuilder.getInstance(reqManager, appConnID, mCallBack);
            requestBuilder.getAppToken(appEndPoint);
        } catch (SMPException e) {
            e.printStackTrace();
        } catch (GcmException e) {
            e.printStackTrace();
        }
    }
}