package com.klouddata.push.pushdemoandroid.util;


public interface UIListener {
	void onRequestError(int operation, Exception e);
	void onRequestSuccess(int operation, String key);
}
