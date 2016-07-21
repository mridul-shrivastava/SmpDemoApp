package com.klouddata.push.pushdemoandroid.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.klouddata.push.pushdemoandroid.R;
import com.klouddata.push.pushdemoandroid.SmpPushAcitivty;
import com.klouddata.push.pushdemoandroid.SmpPushApplication;
import com.klouddata.push.pushdemoandroid.exception.GcmException;
import com.klouddata.push.pushdemoandroid.model.Agency;
import com.klouddata.push.pushdemoandroid.model.AsyncResult;
import com.klouddata.push.pushdemoandroid.offline.OfflineManager;
import com.klouddata.push.pushdemoandroid.offline.OfflineODataStoreException;
import com.klouddata.push.pushdemoandroid.online.AgencyOpenListener;
import com.klouddata.push.pushdemoandroid.online.OnlineManager;
import com.klouddata.push.pushdemoandroid.online.OnlineODataStoreException;
import com.klouddata.push.pushdemoandroid.util.Collections;
import com.klouddata.push.pushdemoandroid.util.Operation;
import com.klouddata.push.pushdemoandroid.util.UIListener;
import com.klouddata.push.pushdemoandroid.util.Util;
import com.sap.maf.tools.logon.core.LogonCoreContext;
import com.sap.maf.tools.logon.core.LogonCoreException;
import com.sap.mobile.lib.parser.IODataEntry;
import com.sap.mobile.lib.parser.IODataError;
import com.sap.mobile.lib.parser.IODataSchema;
import com.sap.mobile.lib.parser.IODataServiceDocument;
import com.sap.mobile.lib.parser.Parser;
import com.sap.mobile.lib.parser.ParserException;
import com.sap.mobile.lib.request.BaseRequest;
import com.sap.mobile.lib.request.INetListener;
import com.sap.mobile.lib.request.IRequest;
import com.sap.mobile.lib.request.IRequestStateElement;
import com.sap.mobile.lib.request.IResponse;
import com.sap.smp.client.odata.ODataEntity;
import com.sap.smp.client.odata.ODataProperty;
import com.sap.smp.client.odata.exception.ODataException;
import com.sap.smp.client.odata.online.OnlineODataStore;
import com.sap.smp.rest.SMPException;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends SmpPushAcitivty implements INetListener, UIListener, AdapterView.OnItemSelectedListener, View.OnClickListener {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    private LogonCoreContext lgCtx;
    private IODataServiceDocument ioDataServiceDocument;
    private IODataSchema schema;
    private ODataEntity mCurrentSelectedEntity;
    private Spinner mSpinner;
    private EditText mAgencyName;
    private EditText mAgencyCity;
    private EditText mAgencyCountry;
    private EditText mAgencyStreet;
    private EditText mAgencyWebsite;
    private TextView mAgencyId;
    private TextView mAgencyCount;
    private Button mUpdateButton;
    private Button mDeleteButon;
    private Button mCreateButton;
    private TextView mLogout;
    private boolean mIsModified;

    public static List<ODataEntity> getEntities() {
        return entities;
    }

    public static void setEntities(List<ODataEntity> entities) {
        MainActivity.entities = entities;
    }

    private static List<ODataEntity> entities;
    private List<Agency> mAgencies;
    private BroadcastReceiver mNetworkReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        lgCtx = mApp.getLgCore().getLogonContext();
        try {
            mApp.initializeAppSettings();
        } catch (GcmException e) {
            e.printStackTrace();
        }
        SharedPreferences sharedPreferences = Util.getSharedPreferences(this);
        boolean isMetadataReady = sharedPreferences.getBoolean("isMetadataReady", false);
        showLoading();
        if (isMetadataReady)
            new GetTask().execute();
        else
            getServiceDocument();
        mNetworkReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                NetworkInfo info = (NetworkInfo) extras
                        .getParcelable("networkInfo");
                NetworkInfo.State state = info.getState();
                Log.d("TEST Internet", info.toString() + " "
                        + state.toString());
                if (state == NetworkInfo.State.CONNECTED) {
                    if (OfflineManager.getOfflineStore() != null) {
                        try {
                            OfflineManager.flushQueuedRequests(MainActivity.this);
                            OfflineManager.refresh(MainActivity.this);
                        } catch (OfflineODataStoreException e) {
                            e.printStackTrace();
                        }
                    }
                    mLogout.setVisibility(View.VISIBLE);
                } else {
                    mLogout.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), "Internet connection is Off", Toast.LENGTH_LONG).show();
                }
            }
        };
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, intentFilter);
    }


    private void init() {
        initViews();
        initListener();
    }

    private void initViews() {
        mSpinner = (Spinner) findViewById(R.id.spinner);
        mSpinner.setOnItemSelectedListener(this);
        mAgencyId = (TextView) findViewById(R.id.agency_id);
        mLogout = (TextView) findViewById(R.id.logout);
        mAgencyCount = (TextView) findViewById(R.id.count);
        mAgencyName = (EditText) findViewById(R.id.agency_name);
        mAgencyCity = (EditText) findViewById(R.id.agency_city);
        mAgencyStreet = (EditText) findViewById(R.id.agency_street);
        mAgencyCountry = (EditText) findViewById(R.id.agency_country);
        mAgencyWebsite = (EditText) findViewById(R.id.agency_website);
        mDeleteButon = (Button) findViewById(R.id.delete);
        mUpdateButton = (Button) findViewById(R.id.update);
        mCreateButton = (Button) findViewById(R.id.create);
    }

    private void initListener() {
        mDeleteButon.setOnClickListener(this);
        mCreateButton.setOnClickListener(this);
        mUpdateButton.setOnClickListener(this);
        mLogout.setOnClickListener(this);
        if(Util.isOnline(MainActivity.this))
            mLogout.setVisibility(View.VISIBLE);
        else
            mLogout.setVisibility(View.GONE);
    }

    @Override
    public void onRequestError(int operation, Exception e) {
        Toast.makeText(this, "error", Toast.LENGTH_LONG).show();
        hideLoading();
//        finish();
    }

    @Override
    public void onRequestSuccess(int operation, String key) {
        String message = "";
        if (operation == Operation.CreateAgency.getValue()) {
            message = getString(R.string.action_settings, key);
        } else if (operation == Operation.UpdateAgency.getValue()) {
            if (TextUtils.isEmpty(key))
                message = getString(R.string.app_name);
            else
                message = getString(R.string.app_pass_confirmpasscode_title_hint, key);
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        showLoading();
        new GetTask().execute();
        if (mIsModified && Util.isOnline(MainActivity.this)) {
            try {
                OfflineManager.refresh(MainActivity.this);
                mIsModified = false;
            } catch (OfflineODataStoreException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();
        Agency selectedAgency = mAgencies.get(position);
        mCurrentSelectedEntity = entities.get(position);
        mAgencyId.setText(selectedAgency.getAgencyId());
        mAgencyName.setText(selectedAgency.getAgencyName());
        mAgencyWebsite.setText(selectedAgency.getWebsite());
        mAgencyCountry.setText(selectedAgency.getCountry());
        mAgencyCity.setText(selectedAgency.getCity());
        mAgencyStreet.setText(selectedAgency.getStreet());
        // Showing selected spinner item
        Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_LONG).show();
    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.update:
                showLoading();
                mIsModified = true;
                try {
                    if (Util.isOnline(MainActivity.this))
                        OnlineManager.updateAgency(getUpdatedAgencyObject(), MainActivity.this);
                    else
                        OfflineManager.updateAgency(getUpdatedAgencyObject(), MainActivity.this);
                } catch (OnlineODataStoreException e) {
                    e.printStackTrace();
                } catch (OfflineODataStoreException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.delete:
                showLoading();
                mIsModified = true;
                try {
                    if (Util.isOnline(MainActivity.this))
                        OnlineManager.deleteAgency(getDeletedAgencyObject(), MainActivity.this);
                    else
                        OfflineManager.deleteAgency(getDeletedAgencyObject(), MainActivity.this);
                } catch (OnlineODataStoreException e) {
                    e.printStackTrace();
                } catch (OfflineODataStoreException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.create:
                showLoading();
                mIsModified = true;
                try {
                    if (Util.isOnline(MainActivity.this))
                        OnlineManager.createAgency(makeAgencyObject(), MainActivity.this);
                    else
                        OfflineManager.createAgency(makeAgencyObject(), MainActivity.this);
                } catch (OnlineODataStoreException e) {
                    e.printStackTrace();
                } catch (OfflineODataStoreException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.logout:
                showLoading();
                mApp.getmLogonUIFacade().deleteUser();
                SharedPreferences sharedPreferences = Util.getSharedPreferences(MainActivity.this);
                sharedPreferences.edit().clear();
//                try {
//                    // close online, if you opened it
//                    AgencyOpenListener openListener = AgencyOpenListener.getInstance();
//                    OnlineODataStore onlineStore = openListener.getStore();
//                    onlineStore.close();
//
//                    // clear technical cache for offline store
//                    // so there's nothing in the cache for the next user
//                    // prevents confusion if you're using the requestCacheResponse method
//                    onlineStore.resetCache();
//
//                    // close offline store
//                    // but not clear the data in it
//                    OfflineManager.getOfflineStore().closeStore();
//                } catch (ODataException e) {
//                    e.printStackTrace();
//                }

                // back to login activity
                Intent intent = new Intent(this, MAFLogonActivity.class);
                startActivity(intent);

                // finish this!
                finish();
                break;
            default:
                break;
        }
    }

    private class GetTask extends AsyncTask<Void, Void, AsyncResult<List<Agency>>> {
        @Override
        protected AsyncResult<List<Agency>> doInBackground(Void... voids) {
            try {
                OnlineManager.openOnlineStore(MainActivity.this);
                OfflineManager.openOfflineStore(MainActivity.this);
                if (Util.isOnline(MainActivity.this))
                    return new AsyncResult<>(OnlineManager.getAgencies());
                else
                    return new AsyncResult<>(OfflineManager.getAgencies());
            } catch (Exception e) {
                return new AsyncResult<>(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncResult<List<Agency>> listAsyncResult) {
            hideLoading();
            if (listAsyncResult.getException() != null || listAsyncResult.getData() == null) {
                String message = String.format(getString(R.string.msg_offline_fail), listAsyncResult.getException());
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading agencies", listAsyncResult.getException());
            } else {
                mAgencies = listAsyncResult.getData();
                List<String> agencyNameList = new ArrayList<>();
                if (mAgencies.size() > 0) {
                    for (Agency agency : mAgencies) {
                        agencyNameList.add(agency.getAgencyName());
                    }
                    mCurrentSelectedEntity = entities.get(0);
                    Agency defaultAgency = mAgencies.get(0);
                    mAgencyId.setText(defaultAgency.getAgencyId());
                    mAgencyName.setText(defaultAgency.getAgencyName());
                    mAgencyWebsite.setText(defaultAgency.getWebsite());
                    mAgencyCountry.setText(defaultAgency.getCountry());
                    mAgencyCity.setText(defaultAgency.getCity());
                    mAgencyStreet.setText(defaultAgency.getStreet());
                    mAgencyCount.setText(mAgencies.size() + "");
                }
                String message = String.format(getString(R.string.msg_offline_success), mAgencies.size());
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                Log.d(TAG, message);// Creating adapter for spinner
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, agencyNameList);
                // Drop down layout style - list view with radio button
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                // attaching data adapter to spinner
                mSpinner.setAdapter(dataAdapter);

//                try {
//                    //OnlineManager.createAgency(makeAgencyObject(), MainActivity.this);
////                    OnlineManager.updateAgency(getUpdatedAgencyObject(), MainActivity.this);
//                    OnlineManager.deleteAgency(getDeletedAgencyObject(), MainActivity.this);
//                } catch (OnlineODataStoreException e) {
//                    e.printStackTrace();
//                }
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
            if (res != null)
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
                SharedPreferences sharedPreferences = Util.getSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isMetadataReady", true);
                editor.commit();
                Log.e("error", schema.toString());
            } catch (ParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                new GetTask().execute();
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
            } catch (ParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
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
        headers.put("X-CSRF-Token", mApp.getmAppToken());
        headers.put("Content-Type", "application/atom+xml");
        request.setHeaders(headers);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return request;
    }

    private Agency makeAgencyObject() {
        Agency agency = new Agency("30022034");
        agency.setAgencyName("Klouddata1");
        agency.setStreet("Mihan Road");
        agency.setCity("Nagpur");
        agency.setCountry("Ind");
        agency.setWebsite("www.klouddata.com");
        return agency;
    }

    private Agency getUpdatedAgencyObject() {
        ODataProperty property = mCurrentSelectedEntity.getProperties().get(Collections.TRAVEL_AGENCY_ENTRY_ID);
        Agency agency = new Agency((String) property.getValue());
        agency.setAgencyName(mAgencyName.getText().toString().trim());
        agency.setStreet(mAgencyStreet.getText().toString().trim());
        agency.setCity(mAgencyCity.getText().toString().trim());
        agency.setCountry(mAgencyCountry.getText().toString().trim());
        agency.setWebsite(mAgencyWebsite.getText().toString().trim());
        agency.setEditResourceURL(mCurrentSelectedEntity.getEditResourcePath());
        return agency;
    }

    private Agency getDeletedAgencyObject() {
        ODataProperty property = mCurrentSelectedEntity.getProperties().get(Collections.TRAVEL_AGENCY_ENTRY_ID);
        Agency agency = new Agency((String) property.getValue());
        agency.setEditResourceURL(mCurrentSelectedEntity.getEditResourcePath());
        return agency;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mNetworkReceiver);
    }
}