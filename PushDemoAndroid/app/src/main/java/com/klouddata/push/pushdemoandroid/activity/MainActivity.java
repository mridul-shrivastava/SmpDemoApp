package com.klouddata.push.pushdemoandroid.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.klouddata.push.pushdemoandroid.R;
import com.klouddata.push.pushdemoandroid.SmpPushAcitivty;
import com.klouddata.push.pushdemoandroid.exception.GcmException;
import com.klouddata.push.pushdemoandroid.model.Agency;
import com.klouddata.push.pushdemoandroid.model.AsyncResult;
import com.klouddata.push.pushdemoandroid.util.OfflineManager;
import com.klouddata.push.pushdemoandroid.util.RequestBuilder;
import com.sap.maf.tools.logon.core.LogonCoreContext;
import com.sap.maf.tools.logon.core.LogonCoreException;
import com.sap.mobile.lib.parser.IODataEntry;
import com.sap.mobile.lib.parser.IODataError;
import com.sap.mobile.lib.parser.IODataSchema;
import com.sap.mobile.lib.parser.IODataServiceDocument;
import com.sap.mobile.lib.parser.ODataEntry;
import com.sap.mobile.lib.parser.Parser;
import com.sap.mobile.lib.parser.ParserException;
import com.sap.mobile.lib.request.BaseRequest;
import com.sap.mobile.lib.request.INetListener;
import com.sap.mobile.lib.request.IRequest;
import com.sap.mobile.lib.request.IRequestStateElement;
import com.sap.mobile.lib.request.IResponse;
import com.sap.smp.rest.SMPException;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends SmpPushAcitivty implements INetListener {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    private LogonCoreContext lgCtx;
    private IODataServiceDocument ioDataServiceDocument;
    private IODataSchema schema;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lgCtx = mApp.getLgCore().getLogonContext();
        //Create the Parser
//        new GetTask().execute();
        try {
            mApp.initializeAppSettings();
        } catch (GcmException e) {
            e.printStackTrace();
        }
        getServiceDocument();
    }

    private class GetTask extends AsyncTask<Void, Void, AsyncResult<List<Agency>>> {
        @Override
        protected AsyncResult<List<Agency>> doInBackground(Void... voids) {
            try {
                OfflineManager.openOfflineStore(MainActivity.this);
                return new AsyncResult<>(OfflineManager.getAgencies());
            } catch (Exception e) {
                return new AsyncResult<>(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncResult<List<Agency>> listAsyncResult) {
            if (listAsyncResult.getException() != null || listAsyncResult.getData() == null) {
                String message = String.format(getString(R.string.msg_offline_fail), listAsyncResult.getException());
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading agencies", listAsyncResult.getException());
            } else {
                final List<Agency> agencies = listAsyncResult.getData();
                String message = String.format(getString(R.string.msg_offline_success), agencies.size());
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                Log.d(TAG, message);
            }
        }
    }

    final class MySSlChallangeListener implements com.sap.mobile.lib.request.HttpChannelListeners.ISSLChallengeListener {

        @Override
        public boolean isServerTrusted(java.security.cert.X509Certificate[] arg0) {
            return true;
        }
    }


    @Override
    public void onError(IRequest arg0, IResponse res, IRequestStateElement arg2) {
        //Get String representation of the response
        String respBody = null;
        try {
            respBody = EntityUtils.toString(res.getEntity(), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
//Get status code of the response
        int code = res.getStatusLine().getStatusCode();
//Get a list of error messages from the IODataError object
        IODataError dataError = null;
        try {
            dataError = mApp.getParser().parseODataError(respBody);
        } catch (ParserException e) {
            e.printStackTrace();
        }
        List<String> mMessages =
                dataError.getValues(IODataError.ELEMENT_MESSAGE);

    }

    @Override
    public void onSuccess(IRequest req, IResponse res) {
        if (req.getRequestTAG().equalsIgnoreCase("Service_Doc")) {
            /* Parses the service document */
            try {
                String appToken = res.getHeadersMap().get("x-csrf-token");
                if (appToken != null)
                    mApp.setmAppToken(appToken);
                ioDataServiceDocument = mApp.getParser().parseODataServiceDocument(EntityUtils.toString(res.getEntity()));
            } catch (ParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                getMetaDataDocument();
            }
        } else if (req.getRequestTAG().equalsIgnoreCase("Meta_Data_Doc")) {
            /* Parses the service document */
            try {
                schema = mApp.getParser().parseODataSchema(EntityUtils.toString(res.getEntity()), ioDataServiceDocument);
                Log.e("error", schema.toString());
            } catch (ParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                createUserEntry();
//                getUserCollection();
//                getSubscriptionCollection();

            }
        } else if (req.getRequestTAG().equalsIgnoreCase("User_Collection")) {
            /* Parses the service document */
            try {
                HttpEntity responseEntity = res.getEntity();
                String responseString = EntityUtils.toString(responseEntity);
                List<IODataEntry> airlineEntries
                        = mApp.getParser().parseODataEntries(responseString,
                        "User_Collection",
                        schema);
            }
            catch (ParserException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        } else if (req.getRequestTAG().equalsIgnoreCase("Create_User_Entry")) {
            /* Parses the service document */
            HttpEntity responseEntity = res.getEntity();
        } else if (req.getRequestTAG().equalsIgnoreCase("Subscription_Collection")) {
            try {
                HttpEntity responseEntity = res.getEntity();
                String responseString = EntityUtils.toString(responseEntity);
//                List<IODataEntry> airlineEntries
//                        = mApp.getParser().parseODataEntries(responseString,
//                        "User_Collection",
//                        schema);
            }
//            catch (ParserException e) {
//                e.printStackTrace();
//            }
            catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    createNewUserEntry();
                } catch (ParserException e) {
                    e.printStackTrace();
                } catch (LogonCoreException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void getServiceDocument() {
        //Create a GET request
        IRequest request = new BaseRequest();
        request.setListener(this);
        request.setPriority(IRequest.PRIORITY_HIGH);
        request.setRequestTAG("Service_Doc");
        request.setRequestMethod(IRequest.REQUEST_METHOD_GET);
//        String endPointURL = "https://sapes1.sapdevcenter.com/sap/opu/odata/IWFND/RMTSAMPLEFLIGHT/" ;
        String endPointURL = null;
        try {
            endPointURL = mApp.getmAppSettings().getApplicationEndPoint();
        } catch (SMPException e) {
            e.printStackTrace();
        }
        request.setRequestUrl(endPointURL);
//Set Application Connection ID Header
        Map<String, String> headers = new HashMap<String, String>();
        try {
            headers.put("X-SMP-APPCID", lgCtx.getConnId());
        } catch (LogonCoreException e) {
            e.printStackTrace();
        }
        headers.put("Accept", "application/xml");
        request.setHeaders(headers);
//Make the Request
        mApp.getRequestManager().makeRequest(request);


        // ILogger logger = new Logger();

/*
       // LogonCore lcCore = LogonCore.getInstance();

        LogonCore.Channel channel = lgCtx.getChannel();

        boolean isHttpsConnection = lgCtx.isHttps();
        String host = lgCtx.getHost();
        int port = lgCtx.getPort();
        String suffix = lgCtx.getResourcePath();
        String appID = lgCtx.getAppId();
        String addr;

//        IPreferences pref = new Preferences(this, logger);
        ConnectivityParameters param = new ConnectivityParameters();
        IRequest getServiceDoc = new BaseRequest();
        getServiceDoc.setPriority(IRequest.PRIORITY_HIGH);
        getServiceDoc.setRequestTAG("Service_Doc");
        RequestManager reqMan = null;
//        try {

//            param.setUserName(lgCtx.getBackendUser());
//            param.setUserPassword(lgCtx.getBackendPassword());
            //reqMan = new RequestManager(logger, pref, param, 1);
            reqMan = mApp.getRequestManager();
            MySSlChallangeListener mscl = new MySSlChallangeListener();
            reqMan.setSSLChallengeListener(mscl);


            if (channel == LogonCore.Channel.REST) {

                if (isHttpsConnection) {
                    addr = "https://" + host + ":" + port + suffix + "/"
                            + appID;
                    MAFLogger.i(LOG_TAG, "REST getServiceDoc request sent to: "
                            + addr);
                } else {
                    addr = "http://" + host + ":" + port + suffix + "/" + appID
                            + "/";
                    MAFLogger.i(LOG_TAG, "REST getServiceDoc request sent to: "
                            + addr);
                }
                getServiceDoc.setRequestUrl(addr);

            } else if (channel == LogonCore.Channel.GATEWAY) {

                addr = "http://<GW_HOST>:<PORT>/<GW_CONTENT_URL>/";
                MAFLogger.i(LOG_TAG, "GATEWAY getServiceDoc request sent to: "
                        + addr);
                getServiceDoc.setRequestUrl(addr);

            }
//        } catch (LogonCoreException e) {
//            e.printStackTrace();
//        }
        getServiceDoc.setRequestMethod(BaseRequest.REQUEST_METHOD_GET);
        getServiceDoc.setListener(this);
        Map<String, String> headers = new HashMap<String, String>();
        try {
            headers.put("X-SMP-APPCID", lgCtx.getConnId());
//            String base64EncodedCredentials = "Basic " + Base64.encodeToString(
//                    (Util.USER_NAME + ":" + Util.PASSWORD).getBytes(),
//                    Base64.NO_WRAP);
//            headers.put("Authorization", base64EncodedCredentials);
//            headers.put("Content-Type", "application/xml");
//            headers.put("Username", Util.USER_NAME);
//            headers.put("Password", Util.PASSWORD);
//            headers.put("X-CSRF-Token", "FETCH");
        } catch (LogonCoreException e) {
            e.printStackTrace();
        }
        getServiceDoc.setHeaders(headers);
        reqMan.makeRequest(getServiceDoc);*/
    }


    private void getMetaDataDocument() {
        //Create a GET request
        IRequest request = new BaseRequest();
        request.setListener(this);
        request.setPriority(IRequest.PRIORITY_HIGH);
        request.setRequestTAG("Meta_Data_Doc");
        request.setRequestMethod(IRequest.REQUEST_METHOD_GET);
        String endPointURL = null;
        try {
            endPointURL = mApp.getmAppSettings().getApplicationEndPoint() + "$metadata";
        } catch (SMPException e) {
            e.printStackTrace();
        }
        request.setRequestUrl(endPointURL);
//Set Application Connection ID Header
        Map<String, String> headers = new HashMap<String, String>();
        try {
            headers.put("X-SMP-APPCID", lgCtx.getConnId());
        } catch (LogonCoreException e) {
            e.printStackTrace();
        }
        headers.put("Accept", "application/xml");
        request.setHeaders(headers);
//Make the Request
        mApp.getRequestManager().makeRequest(request);
    }


    private void getUserCollection() {
        //Create a GET request
        IRequest request = new BaseRequest();
        request.setListener(this);
        request.setPriority(IRequest.PRIORITY_HIGH);
        request.setRequestTAG("User_Collection");
        request.setRequestMethod(IRequest.REQUEST_METHOD_GET);
        String endPointURL = null;
        try {
            endPointURL = mApp.getmAppSettings().getApplicationEndPoint() + "UserCollection";
        } catch (SMPException e) {
            e.printStackTrace();
        }
        request.setRequestUrl(endPointURL);
//Set Application Connection ID Header
        Map<String, String> headers = new HashMap<String, String>();
        try {
            headers.put("X-SMP-APPCID", lgCtx.getConnId());
        } catch (LogonCoreException e) {
            e.printStackTrace();
        }
        headers.put("Accept", "application/json");
        request.setHeaders(headers);
//Make the Request
        mApp.getRequestManager().makeRequest(request);
    }


    private void getSubscriptionCollection() {
        //Create a GET request
        IRequest request = new BaseRequest();
        request.setListener(this);
        request.setPriority(IRequest.PRIORITY_HIGH);
        request.setRequestTAG("Subscription_Collection");
        request.setRequestMethod(IRequest.REQUEST_METHOD_GET);
        String endPointURL = null;
        try {
            endPointURL = mApp.getmAppSettings().getApplicationEndPoint() + "SubscriptionCollection";
        } catch (SMPException e) {
            e.printStackTrace();
        }
        request.setRequestUrl(endPointURL);
//Set Application Connection ID Header
        Map<String, String> headers = new HashMap<String, String>();
        try {
            headers.put("X-SMP-APPCID", lgCtx.getConnId());
        } catch (LogonCoreException e) {
            e.printStackTrace();
        }
        headers.put("Accept", "application/json");
        request.setHeaders(headers);
//Make the Request
        mApp.getRequestManager().makeRequest(request);
    }

    private void createNewUserEntry() throws ParserException, LogonCoreException {
        IRequest request = new BaseRequest();
        request.setListener(this);
        request.setPriority(IRequest.PRIORITY_HIGH);
        request.setRequestTAG("Create_User_Entry");
        request.setRequestMethod(IRequest.REQUEST_METHOD_POST);
        String endPointURL = null;
        try {
            endPointURL = mApp.getmAppSettings().getApplicationEndPoint() + "UserCollection";
        } catch (SMPException e) {
            e.printStackTrace();
        }
        request.setRequestUrl(endPointURL);
//Set Application Connection ID Header
        Map<String, String> headers = new HashMap<String, String>();
//        try {
//            headers.put("X-SMP-APPCID", lgCtx.getConnId());
        headers.put("X-CSRF-Token", mApp.getmAppToken());
//            headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/atom+xml");
//            headers.put("odata", "minimalmetadata");
//            headers.put("DataServiceVersion", "2.0");
//        } catch (LogonCoreException e) {
//            e.printStackTrace();
//        }
        request.setHeaders(headers);
//        String inputBody = "{\"UserName\":\"Mridul\",\"FirstName\":\"Mridul\",\"LastName\":\"Shrivastava\",\"FullName\":\"Mridul Shrivastava\",\"CreatedBy\":\"Admin\"}";
//        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
//        nameValuePairs.add(new BasicNameValuePair("UserName", "Mridul"));
//        nameValuePairs.add(new BasicNameValuePair("FirstName", "Mridul"));
//        nameValuePairs.add(new BasicNameValuePair("LastName", "Shrivastava"));
//        nameValuePairs.add(new BasicNameValuePair("FullName", "Mridul Shrivastava"));
//        nameValuePairs.add(new BasicNameValuePair("CreatedBy", "Admin"));
        request.setData(createUserEntity().getBytes());
//Make the Request
        mApp.getRequestManager().makeRequest(request);
    }

    public String createRegistrationEntity(String mConnectionId) throws ParserException {
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
                "<d:collection>UserCollection</d:collection>\n" +
                "<d:filter>carrid eq 'UA' and connid eq '0941'</d:filter>\n" +
                "<d:select>*</d:select>\n" +
                "<d:persistNotifications>false</d:persistNotifications>\n" +
                "<d:changeType>created</d:changeType>\n" +
                "</m:properties>\n" +
                "</atom:content>\n" +
                "</atom:entry>";
        return createXml;
    }

    public String createUserEntity() throws ParserException {
        String createXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">\n" +
                "<atom:id />\n" +
                "<atom:title>User Info for sample user</atom:title>\n" +
                "<atom:author />\n" +
                "<atom:updated />\n" +
                "<atom:content type=\"application/xml\">\n" +
                "<m:properties\n" +
                "xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"\n" +
                "xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\">\n" +
                "<d:UserName>Test User</d:UserName>\n" +
                "<d:FirstName>Mridul</d:FirstName>\n" +
                "<d:LastName>Shrivastava</d:LastName>\n" +
                "<d:FullName>Mridul Shrivastava</d:FullName>\n" +
                "<d:CreatedBy></d:CreatedBy>\n" +
                "</m:properties>\n" +
                "</atom:content>\n" +
                "</atom:entry>";
        return createXml;
    }

    public IRequest buildPOSTRequest(INetListener listener, String collection, IODataEntry entry, String requestTag) throws ParserException, SMPException, LogonCoreException {
//        int formatType = Parser.FORMAT_XML;
//        if (mIsJSONFormat) {
            int formatType = Parser.FORMAT_JSON;
        IRequest request = new BaseRequest();
//        }
        try {
            String postData = mApp.getParser().buildODataEntryRequestBody(entry, collection, schema, formatType);
            request.setListener(this);
            request.setPriority(IRequest.PRIORITY_HIGH);
            request.setRequestMethod(IRequest.REQUEST_METHOD_POST);
            request.setRequestTAG(requestTag);
            String endPointURL = mApp.getmAppSettings().getApplicationEndPoint() + collection;
            request.setRequestUrl(endPointURL);
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("X-SMP-APPCID", lgCtx.getConnId());
            headers.put("X-CSRF-Token", mApp.getmAppToken());
//            headers.put("Accept", "application/json");
            headers.put("Content-Type", "application/atom+xml");
            request.setHeaders(headers);
            request.setData(postData.getBytes());
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return request;
    }

    private void createUserEntry() {
        IODataEntry newBookingEntry = new ODataEntry();
        newBookingEntry.putPropertyValue("UserName", "Mridul");
        newBookingEntry.putPropertyValue("FirstName", "Mridul");
//        newBookingEntry.putPropertyValue(BookingEntry.FLIGHT_DATE, ODataParsingUtils.formatDate(mFlightDate, mApplication.useJSONFormat()));
        newBookingEntry.putPropertyValue("LastName", "Shrivastava");
        newBookingEntry.putPropertyValue("FullName", "Mridul Shrivastava");
        //Set booking date
//        newBookingEntry.putPropertyValue(BookingEntry.BOOKING_DATE, ODataParsingUtils.formatDate(new Date(), mApplication.useJSONFormat()));
        try {
            IRequest request = buildPOSTRequest(this, "UserCollection", newBookingEntry, "Create_User_Entry");
            mApp.getRequestManager().makeRequest(request);
        } catch (ParserException e) {
            e.printStackTrace();
        } catch (LogonCoreException e) {
            e.printStackTrace();
        } catch (SMPException e) {
            e.printStackTrace();
        }
    }
}