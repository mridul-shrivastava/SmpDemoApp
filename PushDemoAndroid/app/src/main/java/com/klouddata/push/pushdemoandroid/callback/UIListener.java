package com.klouddata.push.pushdemoandroid.callback;

/**
 * Created by mridul.shrivastava on 6/15/2016.
 */
public interface UIListener {
    void onODataRequestError(Exception e);

    void onODataRequestSuccess(String info);
}