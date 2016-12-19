package com.telerik.pushplugin;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;

import java.util.Map;
import java.util.Set;

/**
 * Push plugin extends the GCM Listener Service and has to be registered in the AndroidManifest
 * in order to receive Notification Messages.
 */
public class PushPlugin extends FirebaseMessagingService {
    static final String TAG = "PushPlugin";

    static boolean isActive = false;
    private static Bundle cachedData;
    private static PushPluginListener onMessageReceivedCallback;
    private static PushPluginListener onTokenRefreshCallback;

    /**
     * Register the application in GCM
     *
     * @param appContext
     * @param projectId
     * @param callbacks
     */
    public static void register(Context appContext, String projectId, PushPluginListener callbacks) {
        if (callbacks == null) {
            Log.d(TAG, "Registering without providing a callback!");
        }

        PushPlugin.getRegisterTokenInThread(projectId, callbacks);
    }

    /**
     * Unregister the application from GCM
     *
     * @param appContext
     * @param projectId
     * @param callbacks
     */
    public static void unregister(Context appContext, String projectId, PushPluginListener callbacks) {
        if (callbacks == null) {
            Log.d(TAG, "Unregister without providing a callback!");
        }
        try {
            UnregisterTokenThread t = new UnregisterTokenThread(projectId, callbacks);
            t.start();
        } catch (Exception ex) {
            callbacks.error("Thread failed to start: " + ex.getMessage());
        }
    }

    /**
     * Set the on message received callback
     *
     * @param callbacks
     */
    public static void setOnMessageReceivedCallback(PushPluginListener callbacks) {
        onMessageReceivedCallback = callbacks;

        if (cachedData != null) {
            executeOnMessageReceivedCallback(cachedData);
            cachedData = null;
        }
    }

    /**
     * Execute the onMessageReceivedCallback with the data passed.
     * In case the callback is not present, cache the data;
     *
     * @param data
     */
    public static void executeOnMessageReceivedCallback(Bundle data) {
        if (onMessageReceivedCallback != null) {
            Log.d(TAG, "Sending message to client: " + data.getString("message"));
            JsonObjectExtended dataAsJson = new JsonObjectExtended();
            Set<String> keys = data.keySet();
            for (String key : keys) {
                try {
                    dataAsJson.put(key, JsonObjectExtended.wrap(data.get(key)));
                } catch(JSONException e) {
                    Log.d(TAG, "Error thrown while parsing push notification data bundle to json: " + e.getMessage());
                    //Handle exception here
                }
            }
            onMessageReceivedCallback.success(data.getString("message"), dataAsJson.toString());
        } else {
            Log.d(TAG, "No callback function - caching the data for later retrieval.");
            cachedData = data;
        }
    }

    /**
     * Handles the push messages receive event.
     */
//    @Override
//    public void onMessageReceived(String from, Bundle data) {
//        Log.d(TAG, "New Push Message: " + data);
//        // If the application has the callback registered and it must be active
//        // execute the callback. Otherwise, create new notification in the notification bar of the user.
//        if (onMessageReceivedCallback != null && isActive) {
//            executeOnMessageReceivedCallback(data);
//        } else {
//            Context context = getApplicationContext();
//            NotificationBuilder.createNotification(context, data);
//        }
//    }
    @Override
    public void onMessageReceived(RemoteMessage message) {
        String from = message.getFrom();
        Map data = message.getData();
        Log.d(TAG, "New Push Message: " + data);

        // If the application has the callback registered and it must be active
        // execute the callback. Otherwise, create new notification in the notification bar of the user.
        if (onMessageReceivedCallback != null && isActive) {
            executeOnMessageReceivedCallback(null);
        } else {
            Context context = getApplicationContext();
            NotificationBuilder.createNotification(context, null);
        }
    }

    /**
     * Set the on token refresh callback
     *
     * @param callbacks
     */
    public static void setOnTokenRefreshCallback(PushPluginListener callbacks) {
        onTokenRefreshCallback = callbacks;
    }


    /**
     * Execute the onTokeRefreshCallback.
     */
    public static void executeOnTokenRefreshCallback() {
        if (onTokenRefreshCallback != null) {
            Log.d(TAG, "Executing token refresh callback.");
            onTokenRefreshCallback.success(null);
        } else {
            Log.d(TAG, "No token refresh callback");
        }
    }

    /**
     * This method always returns true. It is here only for legacy purposes.
     */
    public static Boolean areNotificationsEnabled() {
        return true;
    }

    private static void getRegisterTokenInThread(String projectId, PushPluginListener callbacks) {
        try {
            ObtainTokenThread t = new ObtainTokenThread(projectId, callbacks);
            t.start();
        } catch (Exception ex) {
            callbacks.error("Thread failed to start: " + ex.getMessage());
        }
    }
}
