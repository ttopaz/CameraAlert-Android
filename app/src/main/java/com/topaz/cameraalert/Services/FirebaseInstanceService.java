package com.topaz.cameraalert.Services;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.topaz.cameraalert.Utils.RESTMgr;

public class FirebaseInstanceService extends FirebaseInstanceIdService
{
    private static final String TAG = FirebaseInstanceService.class.getSimpleName();

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String token) {

        RESTMgr service = RESTMgr.getInstance();
        service.setToken(token, null);
    }

}
