package com.klouddata.push.pushdemoandroid.callback;

/**
 * Created by mridul.shrivastava on 6/15/2016.
 */

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Get the responses from the server in the background treads and * send notifications to the activities through the main thread.
 */
public class ODataBaseListener {
    private static final String TAG = ODataBaseListener.class.getSimpleName();
    protected UIListener uiListener;
    protected int operation;
    private final int SUCCESS = 0;
    private final int ERROR = -1;
    private Handler uiHandler = new myHandler(this);

    private static class myHandler extends Handler {
        private WeakReference<ODataBaseListener> baseListenerWeakReference;

        public myHandler(ODataBaseListener oDataBaseListener) {
            super(Looper.getMainLooper());
            baseListenerWeakReference = new
                    WeakReference<ODataBaseListener>(oDataBaseListener);
        }

        @Override
        public void handleMessage(Message msg) {
            ODataBaseListener baseListener = baseListenerWeakReference.get();
            if (msg.what == baseListener.SUCCESS) { // Notify the Activity the is complete
                String key = (String) msg.obj;
                baseListener.uiListener.onODataRequestSuccess(key);
            } else if (msg.what == baseListener.ERROR) {
                Exception e = (Exception) msg.obj;
                baseListener.uiListener.onODataRequestError(e);
            }
        }
    }

    public ODataBaseListener(int operation, UIListener uiListener) {
        super();
        this.operation = operation;
        this.uiListener = uiListener;
    }

    /**
     * Notify the UIListener that the request was successful. * @param info an Exception that denotes the error that occurred.
     */
    protected void notifySuccessToListener(String info) {
        Message msg = uiHandler.obtainMessage();
        msg.what = SUCCESS;
        msg.obj = info;
        uiHandler.sendMessage(msg);
        Log.d(TAG, "notifySuccessToListener: " + info);
    }

    /**
     * Notify the UIListener that the request has an error. * @param exception an Exception that denotes the error that occurred.
     */
    protected void notifyErrorToListener(Exception exception) {
        Message msg = uiHandler.obtainMessage();
        msg.what = ERROR;
        msg.obj = exception;
        uiHandler.sendMessage(msg);
        Log.e(TAG, "notifyErrorToListener", exception);
    }
}