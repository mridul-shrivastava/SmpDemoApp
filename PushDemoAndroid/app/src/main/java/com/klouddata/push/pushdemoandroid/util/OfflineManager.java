package com.klouddata.push.pushdemoandroid.util;

/**
 * Created by mridul.shrivastava on 6/15/2016.
 */

import android.content.Context;
import android.util.Log;

import com.klouddata.push.pushdemoandroid.exception.GcmException;
import com.klouddata.push.pushdemoandroid.model.Agency;
import com.sap.maf.tools.logon.core.LogonCore;
import com.sap.maf.tools.logon.core.LogonCoreContext;
import com.sap.smp.client.httpc.HttpConversationManager;
import com.sap.smp.client.httpc.authflows.CommonAuthFlowsConfigurator;
import com.sap.smp.client.odata.ODataEntity;
import com.sap.smp.client.odata.ODataEntitySet;
import com.sap.smp.client.odata.ODataPayload;
import com.sap.smp.client.odata.ODataPropMap;
import com.sap.smp.client.odata.ODataProperty;
import com.sap.smp.client.odata.impl.ODataErrorDefaultImpl;
import com.sap.smp.client.odata.offline.ODataOfflineStore;
import com.sap.smp.client.odata.offline.ODataOfflineStoreOptions;
import com.sap.smp.client.odata.store.ODataRequestParamSingle;
import com.sap.smp.client.odata.store.ODataResponseSingle;
import com.sap.smp.client.odata.store.impl.ODataRequestParamSingleDefaultImpl;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class OfflineManager {
    public static final String TAG = OfflineManager.class.getSimpleName();
    private static ODataOfflineStore offlineStore;
    private static final String TRAVEL_AGENCY_COLLECTION = "TravelAgencies_DQ";
    public static final String TRAVEL_AGENCY_ENTRY_ID = "agencynum";

    public static boolean openOfflineStore(Context context) throws GcmException {
        if (offlineStore == null) {
            try {
                //This instantiate the native UDB libraries which are located in the libodataofflinejni.so file
                ODataOfflineStore.globalInit();
                LogonCoreContext lgCtx = LogonCore.getInstance().getLogonContext();
                String endPointURL = lgCtx.getAppEndPointUrl();
                URL url = new URL(endPointURL);
                Log.d(TAG, "openOfflineStore: appcid:" + lgCtx.getConnId());
                Log.d(TAG, "openOfflineStore: endpointurl:" + endPointURL);
                // Define the offline store options. Connection parameter and credentials and the application connection id we got at the registration
                ODataOfflineStoreOptions options = new ODataOfflineStoreOptions();
                options.host = url.getHost();
                options.port = String.valueOf(url.getPort());
                options.enableHTTPS = lgCtx.isHttps();
                // the serviceRoot is the backend connector name, which is usually the same as the application configuration name enter in the SMP management cockpit
                options.serviceRoot = Util.APP_ID;
                CredentialsProvider credProvider = CredentialsProvider.getInstance(lgCtx);
                //Without MAF Logon
                HttpConversationManager manager = new CommonAuthFlowsConfigurator(context).supportBasicAuthUsing(credProvider).configure(new HttpConversationManager(context));
                options.conversationManager = manager;
                //put the APPCID in the HTTP Header
                options.enableRepeatableRequests = false;
                options.storeName = "flight";
                //This defines the oData collections which will be stored in the offline store
                options.addDefiningRequest("reg1", TRAVEL_AGENCY_COLLECTION, false);
                //Open offline store
                offlineStore = new ODataOfflineStore(context);
                offlineStore.openStoreSync(options);
                Log.d(TAG, "openOfflineStore: library version" + ODataOfflineStore.libraryVersion());
                return true;
            } catch (Exception e) {
                throw new GcmException(e);
            }
        } else {
            return true;
        }
    }

    public static List<Agency> getAgencies() throws GcmException {
        ArrayList<Agency> agencyList = new ArrayList<Agency>();
        if (offlineStore != null) {
            Agency agency;
            ODataProperty property;
            ODataPropMap properties;
            try {
                String resourcePath = TRAVEL_AGENCY_COLLECTION + "?$orderby=" + TRAVEL_AGENCY_ENTRY_ID + "%20desc";
                ODataRequestParamSingle request = new ODataRequestParamSingleDefaultImpl();
                request.setMode(ODataRequestParamSingle.Mode.Read);
                request.setResourcePath(resourcePath);
                ODataResponseSingle response = (ODataResponseSingle) offlineStore.executeRequest(request);
                if (response.getPayloadType() == ODataPayload.Type.Error) {
                    ODataErrorDefaultImpl error = (ODataErrorDefaultImpl) response.getPayload();
                    throw new GcmException(error.getMessage());
                } else if (response.getPayloadType() == ODataPayload.Type.EntitySet) {
                    ODataEntitySet feed = (ODataEntitySet) response.getPayload();
                    List<ODataEntity> entities = feed.getEntities();
                    for (ODataEntity entity : entities) {
                        properties = entity.getProperties();
                        property = properties.get(TRAVEL_AGENCY_ENTRY_ID);
                        agency = new Agency((String) property.getValue());
                        agency.setEditResourceURL(entity.getEditResourcePath());
                        agencyList.add(agency);
                    }
                } else {
                    throw new GcmException("Invalid payload: EntitySet expected but got " + response.getPayloadType().name());
                }
            } catch (Exception e) {
                throw new GcmException(e);
            }
        }
        return agencyList;
    }
}