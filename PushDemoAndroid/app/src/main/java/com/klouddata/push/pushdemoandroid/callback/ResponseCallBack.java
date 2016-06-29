package com.klouddata.push.pushdemoandroid.callback;

import com.sap.mobile.lib.request.IResponse;

/**
 * Created by mridul.shrivastava on 6/1/2016.
 */
public interface ResponseCallBack {
    public void onGcmRegistered(String regisId);
    public void onGcmRegistratioFailed(String msg);
    public void onEntityCreated(IResponse iResponse);
    public void onEntityCreationFailed(IResponse iResponse);
}
