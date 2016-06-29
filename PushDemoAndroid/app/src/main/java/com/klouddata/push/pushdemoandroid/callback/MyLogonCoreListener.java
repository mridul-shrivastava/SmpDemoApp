package com.klouddata.push.pushdemoandroid.callback;

/**
 * Created by mridul.shrivastava on 6/15/2016.
 */

import android.util.Log;

import com.klouddata.push.pushdemoandroid.exception.GcmException;
import com.sap.maf.tools.logon.core.LogonCore;
import com.sap.maf.tools.logon.core.LogonCoreException;
import com.sap.maf.tools.logon.core.LogonCoreListener;
import com.sybase.persistence.DataVault;

/**
 * This listener implements the methods that are called when the registration finishes
 */
public class MyLogonCoreListener extends ODataBaseListener implements LogonCoreListener {
    public static final String TAG = MyLogonCoreListener.class.getSimpleName();

    public MyLogonCoreListener(int operation, UIListener uiListener) {
        super(operation, uiListener);
    }

    @Override
    public void registrationFinished(boolean b, String s, int i, DataVault.DVPasswordPolicy dvPasswordPolicy) {
        Log.d(TAG, "registrationFinished: " + b);
        if (b) {
            try {
                LogonCore lgCore = LogonCore.getInstance();
                //For testing purposes we are not using password to create the secure store
                // Consider enabling the password if the application handles sensitive
// information
                lgCore.createStore(null, false); //Persists the registration information into the secure store then clears the sensitive information (e.g. password arrays) from the
//memory
                lgCore.persistRegistration();
                notifySuccessToListener("successful: " + s);
            } catch (LogonCoreException e) {
                notifyErrorToListener(e);
            }
        } else {
            notifyErrorToListener(new GcmException("registration failed: " + s));
        }
    }

    @Override
    public void deregistrationFinished(boolean b) {
    }

    @Override
    public void backendPasswordChanged(boolean b) {
    }

    @Override
    public void applicationSettingsUpdated() {
    }

    @Override
    public void traceUploaded() {
    }
}