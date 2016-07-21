package com.klouddata.push.pushdemoandroid.online;

public class OnlineODataStoreException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public OnlineODataStoreException(String errorMessage) {
        super(errorMessage);
    }

	public OnlineODataStoreException(Throwable throwable) {
        super(throwable);
    }

}
