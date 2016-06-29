package com.klouddata.push.pushdemoandroid;

import android.app.Application;
import android.util.Log;

import com.klouddata.push.pushdemoandroid.exception.GcmException;
import com.klouddata.push.pushdemoandroid.util.Util;
import com.sap.maf.tools.logon.core.LogonCore;
import com.sap.maf.tools.logon.core.LogonCoreContext;
import com.sap.maf.tools.logon.core.LogonCoreException;
import com.sap.mobile.lib.configuration.IPreferences;
import com.sap.mobile.lib.configuration.Preferences;
import com.sap.mobile.lib.configuration.PreferencesException;
import com.sap.mobile.lib.parser.IODataSchema;
import com.sap.mobile.lib.parser.Parser;
import com.sap.mobile.lib.parser.ParserException;
import com.sap.mobile.lib.request.ConnectivityParameters;
import com.sap.mobile.lib.request.RequestManager;
import com.sap.mobile.lib.supportability.Logger;
import com.sap.smp.rest.AppSettings;
import com.sap.smp.rest.ClientConnection;

public class SmpPushApplication extends Application {
    private static final String TAG = SmpPushApplication.class.getSimpleName();
    private RequestManager mRequestManager = null;
    private Preferences mPreferences = null;
    private Logger mLogger = null;
    private ConnectivityParameters mConnectivityParameters = null;
    private Parser mParser = null;
    private IODataSchema mSchema = null;

    public LogonCore getLgCore() {
        return lgCore;
    }

    private LogonCore lgCore;
    private static final String VK_APPCID = "appcid";

    public AppSettings getmAppSettings() {
        return mAppSettings;
    }

    private AppSettings mAppSettings;

    private boolean mUseJSONFormat = false;

    public String getmAppToken() {
        return mAppToken;
    }

    public void setmAppToken(String mAppToken) {
        this.mAppToken = mAppToken;
    }

    private String mAppToken;

    private final static int NUMBER_OF_THREADS = 3;

    @Override
    public void onCreate() {
        super.onCreate();
        init();

        //Create Logger
        mLogger = new Logger();
//        try {
            initializeLogonCore();
//            initializeAppSettings();
//        } catch (GcmException e) {
//            e.printStackTrace();
//        }
        //Create Connectivity Parameters
//        mConnectivityParameters = new ConnectivityParameters();
//        mConnectivityParameters.setLanguage(this.getResources()
//                .getConfiguration().locale.getLanguage());
//        mConnectivityParameters.enableXsrf(true);

//        //Create Preferences
        try {
            mPreferences = new Preferences(this, mLogger);
            mPreferences.setBooleanPreference(IPreferences.PERSISTENCE_SECUREMODE, false);
            mPreferences.setIntPreference(IPreferences.CONNECTIVITY_HTTPS_PORT, 443);
            mPreferences.setIntPreference(IPreferences.CONNECTIVITY_CONNTIMEOUT, 70000);
            mPreferences.setIntPreference(IPreferences.CONNECTIVITY_SCONNTIMEOUT, 70000);
        } catch (PreferencesException e) {
            Log.d(TAG, e.getLocalizedMessage());
        }
//
//        try {
//            mParser = new Parser(mPreferences, getLogger());
//        } catch (ParserException e) {
//            mLogger.e(TAG, "Error in initializing the parser!", e);
//        }
    }


    /**
     * It returns the only instance of Logger for the lifetime of the application
     *
     * @return Logger
     */
    public Logger getLogger() {
        return mLogger;
    }

    /**
     * It creates only one instance of RequestManager for the lifetime of the application
     *
     * @return RequestManager
     */
    public RequestManager getRequestManager() {
        if (mRequestManager == null) {
            mRequestManager = new RequestManager(mLogger, mPreferences,
                    mConnectivityParameters, NUMBER_OF_THREADS);
        }
        return mRequestManager;
    }

    /**
     * @return
     */
    public Parser getParser() {
        try {
            mParser = new Parser(mPreferences, getLogger());
        } catch (ParserException e) {
            mLogger.e(TAG, "Error in initializing the parser!", e);
        }
        return this.mParser;
    }

    /**
     * It returns the only  instance of Preferences for the lifetime of the application
     *
     * @return Preferences
     */
    private void init() {
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
    /**
     * Get AppSettings
     *
     * @return
     * @throws GcmException
     */
    public void initializeAppSettings() throws GcmException {
        String appConnID = null;
        if (mAppSettings == null) {
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
            if (mRequestManager == null) {
                Logger logger = new Logger();
//Create Connectivity Parameters
                ConnectivityParameters connectivityParameters = new ConnectivityParameters();
                connectivityParameters.setLanguage(getResources()
                        .getConfiguration().locale.getLanguage());
                connectivityParameters.enableXsrf(true);
                try {
                    connectivityParameters.setUserName(lgCtx.getBackendUser());
                    connectivityParameters.setUserPassword(lgCtx.getBackendPassword());
                } catch (LogonCoreException e) {
                    throw new GcmException(e);
                }
//Create Preferences
                Preferences preferences = new Preferences(this, logger);
                try {
                    preferences.setBooleanPreference(IPreferences.PERSISTENCE_SECUREMODE, false);
                    preferences.setIntPreference(IPreferences.CONNECTIVITY_HTTPS_PORT, 443);
                    preferences.setIntPreference(IPreferences.CONNECTIVITY_CONNTIMEOUT, 70000);
                    preferences.setIntPreference(IPreferences.CONNECTIVITY_SCONNTIMEOUT, 70000);
                } catch (PreferencesException e) {
                    Log.d(TAG, e.getLocalizedMessage());
                    throw new GcmException(e);
                }
                mRequestManager = new RequestManager(logger, preferences,
                        connectivityParameters, NUMBER_OF_THREADS);
            }
            ClientConnection clientConnection =
                    new ClientConnection(this, Util.APP_ID,
                            null, null, mRequestManager);
            clientConnection.setConnectionProfile(true, host, port,
                    null, null);
            clientConnection.setApplicationConnectionID(appConnID);
            mAppSettings = new AppSettings(clientConnection);
        }
    }


    /**
     * Initialize LogonCore component
     */
    private void initializeLogonCore() {
        //Get LogonCore instance
        lgCore = LogonCore.getInstance();

        //Initialize LogonCore with application configuraton name
        lgCore.init(this, Util.APP_ID);
        //Check if application connection exists
        try {
            //check if secure store is available
            if (lgCore.isStoreAvailable()) {
                //Unlock the store
                lgCore.unlockStore(null);
                //Get application connection id
                String appConnId = lgCore.getObjectFromStore(VK_APPCID);
            }
        } catch (LogonCoreException e) {
            Log.e(TAG, "error initializing logon core", e);
        }
    }
}

