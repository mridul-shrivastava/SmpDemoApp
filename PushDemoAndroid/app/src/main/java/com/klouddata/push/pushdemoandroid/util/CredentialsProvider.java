package com.klouddata.push.pushdemoandroid.util;

/**
 * Created by mridul.shrivastava on 6/15/2016.
 */

import com.sap.maf.tools.logon.core.LogonCoreContext;
import com.sap.maf.tools.logon.core.LogonCoreException;
import com.sap.smp.client.httpc.authflows.UsernamePasswordProvider;
import com.sap.smp.client.httpc.authflows.UsernamePasswordToken;
import com.sap.smp.client.httpc.events.IReceiveEvent;
import com.sap.smp.client.httpc.events.ISendEvent;

public class CredentialsProvider implements UsernamePasswordProvider {
    private static CredentialsProvider instance;
    private LogonCoreContext lgCtx;

    private CredentialsProvider(LogonCoreContext logonContext) {
        lgCtx = logonContext;
    }

    public static CredentialsProvider getInstance(LogonCoreContext logonContext) {
        if (instance == null) {
            instance = new CredentialsProvider(logonContext);
        }
        return instance;
    }

    @Override
    public Object onCredentialsNeededUpfront(ISendEvent iSendEvent) {
        try {
            String username = lgCtx.getBackendUser();
            String password = lgCtx.getBackendPassword();
            return new UsernamePasswordToken(username, password);
        } catch (LogonCoreException e) {
            return null;
        }
    }

    @Override
    public Object onCredentialsNeededForChallenge(IReceiveEvent iReceiveEvent) {
        try {
            String username = lgCtx.getBackendUser();
            String password = lgCtx.getBackendPassword();
            return new UsernamePasswordToken(username, password);
        } catch (LogonCoreException e) {
            return null;
        }
    }
}
