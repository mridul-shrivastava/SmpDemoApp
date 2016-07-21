package com.klouddata.push.pushdemoandroid.util;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Util {

    public static String HOST_NAME = "192.168.0.77"; //smp-3.klouddata.com //114.143.201.83
    public static final String APP_ID = "com.sap.pushnotification";//com.sap.flight
    public final static String PROPERTY_GCM_REGISTRATION_ID = "d:AndroidGcmRegistrationId";
    public final static String PROPERTY_APPLICATION_ID = "d:ApplicationConnectionId";
    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String PROPERTY_APP_VERSION = "appVersion";
    public static String USER_NAME = "P1794163851"; //smpAdmin //P1794163851
    public static String PASSWORD = "Anjali1#"; //s3pAdmin //Anjali1#

    public static boolean isNullOrBlank(String text) {
        return (text == null || text.trim().equalsIgnoreCase("") || text.trim()
                .equalsIgnoreCase("null")) ? true : false;
    }

    public static void showAlert(Context context, String msg) {
        if (isNullOrBlank(msg))
            return;
        showAlert(context, "ALERT", msg);
    }

    public static void showAlert(Context context, String tittle, String msg) {
        if (isNullOrBlank(msg) || isNullOrBlank(tittle))
            return;
        Builder alertBuilder = new Builder(context);
        alertBuilder.setTitle(tittle);
        alertBuilder.setMessage(msg);
        alertBuilder.setCancelable(false);
        alertBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        alertBuilder.create();
        alertBuilder.show();
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    public static SharedPreferences getSharedPreferences(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences(
                "PushDemo", Context.MODE_PRIVATE);
        return sharedPref;
    }

}
