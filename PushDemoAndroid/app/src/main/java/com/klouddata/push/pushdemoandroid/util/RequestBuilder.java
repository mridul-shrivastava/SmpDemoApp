package com.klouddata.push.pushdemoandroid.util;

import android.content.Context;

import com.klouddata.push.pushdemoandroid.callback.ResponseCallBack;
import com.sap.mobile.lib.parser.ParserException;
import com.sap.mobile.lib.request.BaseRequest;
import com.sap.mobile.lib.request.INetListener;
import com.sap.mobile.lib.request.IRequest;
import com.sap.mobile.lib.request.IRequestStateElement;
import com.sap.mobile.lib.request.IResponse;
import com.sap.mobile.lib.request.RequestManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class RequestBuilder implements INetListener {

    private static RequestBuilder instance = null;
    private String mAppToken;
    private String mAppEndPoint;
    private static Context mContext;
    private ResponseCallBack mCallBack;
    private RequestManager mReqManager;
    private String mConnectionId;

    protected RequestBuilder() {
        // Exists only to defeat instantiation.
    }

    protected RequestBuilder(RequestManager requestManager, String connectionId, ResponseCallBack callBack) {
        // Exists only to defeat instantiation.
        mReqManager = requestManager;
        mConnectionId = connectionId;
        mCallBack = callBack;
    }

    /**
     * @return RequestBuilder
     */
    public static RequestBuilder getInstance(RequestManager requestManager, String connectionId, ResponseCallBack callBack) {
        if (instance == null) {
            instance = new RequestBuilder(requestManager, connectionId, callBack);
        }
        return instance;
    }

    //	/**
//	 * @param listener
//	 * @param collection
//	 * @param entry
//	 * @param requestTag
//	 * @return
//	 * @throws ParserException
//	 */
    public IRequest buildPOSTRequest(String entry, String mEndPoint) throws ParserException {
        IRequest request = new BaseRequest();
        request.setListener(this);
        request.setPriority(IRequest.PRIORITY_HIGH);
        request.setRequestMethod(IRequest.REQUEST_METHOD_POST);
        request.setRequestTAG("Create Registration");
        String endPointURL = mEndPoint + "SubscriptionCollection";
        request.setRequestUrl(endPointURL);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("content-type", "application/atom+xml");
        headers.put("X-CSRF-Token", mAppToken);
        request.setHeaders(headers);
        request.setData(entry.getBytes());
        return request;
    }

    @Override
    public void onSuccess(IRequest iRequest, IResponse iResponse) {
        if (mAppToken == null) {
            mAppToken = iResponse.getHeadersMap().get("x-csrf-token");
            try {
                createRegistrationEntity(mAppEndPoint);
            } catch (ParserException e) {
                e.printStackTrace();
            }
        } else if ((iResponse.getStatusLine().getStatusCode() == 201)) {
            mCallBack.onEntityCreated(iResponse);
            mAppToken = null;
        }
    }

    @Override
    public void onError(IRequest iRequest, IResponse iResponse, IRequestStateElement iRequestStateElement) {
        mCallBack.onEntityCreationFailed(iResponse);
    }


    public void getAppToken(String appEndPoint) {
        mAppEndPoint = appEndPoint;
        IRequest request = new BaseRequest();
        request.setListener(this);
        request.setPriority(IRequest.PRIORITY_HIGH);
        request.setRequestMethod(IRequest.REQUEST_METHOD_GET);
        try {
            URL url = new URL(appEndPoint);
            String getTokenURL = "http://" + url.getHost() + ":"
                    + url.getPort() + "/" + Util.APP_ID + "/";
            request.setRequestUrl(getTokenURL);
            request.setRequestTAG("Get Token");
            mReqManager.makeRequest(request);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    public void createRegistrationEntity(String mEndPoint) throws ParserException {
        String createXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">\n" +
                "<atom:id />\n" +
                "<atom:title>Subscription for sample user</atom:title>\n" +
                "<atom:author />\n" +
                "<atom:updated />\n" +
                "<atom:content type=\"application/xml\">\n" +
                "<m:properties\n" +
                "xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"\n" +
                "xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\">\n" +
                "<d:deliveryAddress>urn:sap-com:channel:SMP_PUSH:" + mConnectionId + "</d:deliveryAddress>\n" +
                "<d:collection>SubscriptionCollection</d:collection>\n" +
                "<d:filter>carrid eq 'UA' and connid eq '0941'</d:filter>\n" +
                "<d:select>*</d:select>\n" +
                "<d:persistNotifications>false</d:persistNotifications>\n" +
                "<d:changeType>created</d:changeType>\n" +
                "</m:properties>\n" +
                "</atom:content>\n" +
                "</atom:entry>";

        IRequest request = buildPOSTRequest(createXml, mEndPoint);
        mReqManager.makeRequest(request);
    }
}
