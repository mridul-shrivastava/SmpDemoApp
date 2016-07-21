package com.klouddata.push.pushdemoandroid.offline;

/**
 * Created by mridul.shrivastava on 6/15/2016.
 */

//http://sapassets.edgesuite.net/sapcom/docs/2014/12/8e7398b5-5b7c-0010-82c7-eda71af511fa.pdf
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.klouddata.push.pushdemoandroid.exception.GcmException;
import com.klouddata.push.pushdemoandroid.model.Agency;
import com.klouddata.push.pushdemoandroid.online.CredentialsProvider;
import com.klouddata.push.pushdemoandroid.util.AgencyRequestListener;
import com.klouddata.push.pushdemoandroid.util.Collections;
import com.klouddata.push.pushdemoandroid.util.Operation;
import com.klouddata.push.pushdemoandroid.util.UIListener;
import com.klouddata.push.pushdemoandroid.util.Util;
import com.sap.maf.tools.logon.core.LogonCore;
import com.sap.maf.tools.logon.core.LogonCoreContext;
import com.sap.smp.client.httpc.HttpConversationManager;
import com.sap.smp.client.httpc.authflows.CommonAuthFlowsConfigurator;
import com.sap.smp.client.odata.ODataEntity;
import com.sap.smp.client.odata.ODataEntitySet;
import com.sap.smp.client.odata.ODataPayload;
import com.sap.smp.client.odata.ODataPropMap;
import com.sap.smp.client.odata.ODataProperty;
import com.sap.smp.client.odata.exception.ODataException;
import com.sap.smp.client.odata.exception.ODataParserException;
import com.sap.smp.client.odata.impl.ODataEntityDefaultImpl;
import com.sap.smp.client.odata.impl.ODataErrorDefaultImpl;
import com.sap.smp.client.odata.impl.ODataPropertyDefaultImpl;
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

    public static ODataOfflineStore getOfflineStore() {
        return offlineStore;
    }

    private static ODataOfflineStore offlineStore;

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
                options.storeName = "flight" ;
                //This defines the oData collections which will be stored in the offline store
                options.addDefiningRequest("reg1", Collections.TRAVEL_AGENCY_COLLECTION, false);
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
                String resourcePath = Collections.TRAVEL_AGENCY_COLLECTION + "?$orderby=" + Collections.TRAVEL_AGENCY_ENTRY_ID + "%20desc" ;
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
                        property = properties.get(Collections.TRAVEL_AGENCY_ENTRY_ID);
                        agency = new Agency((String) property.getValue());
                        property = properties.get(Collections.TRAVEL_AGENCY_ENTRY_NAME);
                        agency.setAgencyName((String) property.getValue());
                        property = properties.get(Collections.TRAVEL_AGENCY_ENTRY_STREET);
                        agency.setStreet((String) property.getValue());
                        property = properties.get(Collections.TRAVEL_AGENCY_ENTRY_CITY);
                        agency.setCity((String) property.getValue());
                        property = properties.get(Collections.TRAVEL_AGENCY_ENTRY_COUNTRY);
                        agency.setCountry((String) property.getValue());
                        property = properties.get(Collections.TRAVEL_AGENCY_ENTRY_URL);
                        agency.setWebsite((String) property.getValue());

                        //Obtain the edit resource path from the ODataEntity
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

    /**
     * Create Travel agency entity type (payload) with the corresponding value
     *
     * @param agency agency information
     * @return ODataEntity with the agency information
     * @throws ODataParserException
     */
    private static ODataEntity createAgencyEntity(Agency agency) throws ODataParserException {
        ODataEntity newEntity = null;
        if (agency != null && agency.isInitialized()) {
            newEntity = new ODataEntityDefaultImpl(Collections.TRAVEL_AGENCY_ENTITY_TYPE);

            String agencyId = agency.getAgencyId();
            newEntity.getProperties().put(Collections.TRAVEL_AGENCY_ENTRY_ID,
                    new ODataPropertyDefaultImpl(Collections.TRAVEL_AGENCY_ENTRY_ID, agencyId));
            newEntity.getProperties().put(Collections.TRAVEL_AGENCY_ENTRY_NAME,
                    new ODataPropertyDefaultImpl(Collections.TRAVEL_AGENCY_ENTRY_NAME, agency.getAgencyName()));
            newEntity.getProperties().put(Collections.TRAVEL_AGENCY_ENTRY_STREET,
                    new ODataPropertyDefaultImpl(Collections.TRAVEL_AGENCY_ENTRY_STREET, agency.getStreet()));
            newEntity.getProperties().put(Collections.TRAVEL_AGENCY_ENTRY_CITY,
                    new ODataPropertyDefaultImpl(Collections.TRAVEL_AGENCY_ENTRY_CITY, agency.getCity()));
            newEntity.getProperties().put(Collections.TRAVEL_AGENCY_ENTRY_COUNTRY,
                    new ODataPropertyDefaultImpl(Collections.TRAVEL_AGENCY_ENTRY_COUNTRY, agency.getCountry()));
            newEntity.getProperties().put(Collections.TRAVEL_AGENCY_ENTRY_URL,
                    new ODataPropertyDefaultImpl(Collections.TRAVEL_AGENCY_ENTRY_URL, agency.getWebsite()));

            if (!TextUtils.isEmpty(agencyId)) {
                String resourcePath = Collections.getEditResourcePath(Collections.TRAVEL_AGENCY_COLLECTION, agencyId);
                newEntity.setResourcePath(resourcePath, resourcePath);
                newEntity.setEtag(agencyId);
            }
        }
        return newEntity;
    }

    public static void createAgency(Agency agency, UIListener uiListener) throws OfflineODataStoreException {
        if (offlineStore == null)
            return;
        try {
//Creates the entity payload
            ODataEntity newEntity = createAgencyEntity(agency);
//Send the request to create the new agency in the local database
            offlineStore
                    .scheduleCreateEntity(newEntity, Collections.
                            TRAVEL_AGENCY_COLLECTION, new AgencyRequestListener(Operation.CreateAgency.getValue(), uiListener), null);
        } catch (Exception e) {
            throw new OfflineODataStoreException(e);
        }
//END
    }

    public static void updateAgency(Agency agency, UIListener uiListener) throws OfflineODataStoreException {
        if (offlineStore == null)
            return;
        try {
//Creates the entity payload
            ODataEntity newEntity = createAgencyEntity(agency);
//Send the request to create the new agency in the local database
            offlineStore.schedulePatchEntity(newEntity, new AgencyRequestListener(Operation.UpdateAgency.getValue(), uiListener), null);
        } catch (Exception e) {
            throw new OfflineODataStoreException(e);
        }
//END
    }

    public static void deleteAgency(Agency agency, UIListener uiListener) throws OfflineODataStoreException {
        if (offlineStore == null)
            return;
        try {
//get resource path required to send DELETE requests
            String resourcePath =
                    agency.getEditResourceURL();
            if (!TextUtils.isEmpty(resourcePath)) {
//AgencyRequestListener implements ODataRequestListener,
// which receives the response from the server and notify the activity
// that shows the message to the user
                AgencyRequestListener agencyListener = new AgencyRequestListener(Operation.DeleteAgency.getValue(), uiListener);
//Scheduling method for deleting an Entity asynchronously
                offlineStore.scheduleDeleteEntity(resourcePath, null, agencyListener, null);
            } else {
                throw new OfflineODataStoreException("Resource path is null");
            }
        } catch (Exception e) {
            throw new OfflineODataStoreException(e);
        }
//END
    }

    public static void flushQueuedRequests(UIListener uiListener) throws OfflineODataStoreException {
        if (offlineStore == null)
            return;
        try {
//AgencyFlushListener implements ODataOfflineStoreFlushListener
//used to get progress updates of a flush operation
            AgencyFlushListener flushListener = new AgencyFlushListener(uiListener);
//Asynchronously starts sending pending modification request to the server
            offlineStore.scheduleFlushQueuedRequests(flushListener);
        } catch (ODataException e) {
            throw new OfflineODataStoreException(e);
        }
//END
    }

    public static void refresh(UIListener uiListener) throws OfflineODataStoreException {
        if (
                offlineStore == null)
            return;
        try {
//AgencyRefreshListener implements ODataOfflineStoreRefreshListener
//used to get progress updates of a refresh operation
            AgencyRefreshListener refreshListener = new AgencyRefreshListener(uiListener);
//Asynchronously refreshes the store with the OData service
            offlineStore
                    .scheduleRefresh(refreshListener);
        } catch (ODataException e) {
            throw new OfflineODataStoreException(e);
        }
//END
    }
}