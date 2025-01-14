package io.deeplink;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

public class DeeplinkPlugin extends CordovaPlugin {
    private static final String TAG = "DeeplinkPlugin";

    private JSONObject lastEvent;

    private CallbackContext handler = null;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.d(TAG, "DeepLinkPlugin: firing up...");
        handleIntent(cordova.getActivity().getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "DeepLinkPlugin: onNewIntent...");
        handleIntent(intent);
    }

    public void handleIntent(Intent intent) {
        Log.d(TAG, "DeepLinkPlugin: handling intent...");
        final String intentString = intent.getDataString();

        // read intent
        String action = intent.getAction();
        Uri url = intent.getData();
        JSONObject bundleData = this._bundleToJson(intent.getExtras());
        Log.d(TAG, "Got a new intent: " + intentString + " " + intent.getScheme() + " " + action + " " + url);

        // if app was not launched by the url - ignore
        if (!Intent.ACTION_VIEW.equals(action) || url == null) {
            return;
        }

        // store message and try to consume it
        try {
            lastEvent = new JSONObject();
            lastEvent.put("url", url.toString());
            lastEvent.put("path", url.getPath());
            lastEvent.put("queryString", url.getQuery());
            lastEvent.put("scheme", url.getScheme());
            lastEvent.put("host", url.getHost());
            lastEvent.put("fragment", url.getFragment());
            lastEvent.put("extra", bundleData);
            consumeEvents();
        } catch (JSONException ex) {
            Log.e(TAG, "Unable to process URL scheme deeplink", ex);
        }
    }

    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "DeepLinkPlugin: execute: " + action);
        switch (action) {
            case "onDeepLink":
                addHandler(args, callbackContext);
                break;
            case "canOpenApp":
                String uri = args.getString(0);
                canOpenApp(uri, callbackContext);
                break;
            case "getHardwareInfo":
                getHardwareInfo(args, callbackContext);
                break;
        }
        return true;
    }

    /**
     * Try to consume any waiting intent events by sending them to our plugin
     * handlers. We will only do this if we have active handlers so the message isn't lost.
     */
    private void consumeEvents() {
        Log.d(TAG, "DeepLinkPlugin: consumeEvents...");
        if (handler == null || lastEvent == null) {
            return;
        }
        sendToJs(lastEvent, handler);
        lastEvent = null;
    }

    private void sendToJs(JSONObject event, CallbackContext callback) {
        Log.d(TAG, "DeepLinkPlugin: sendToJs...");
        final PluginResult result = new PluginResult(PluginResult.Status.OK, event);
        result.setKeepCallback(true);
        callback.sendPluginResult(result);
    }

    private void addHandler(JSONArray args, final CallbackContext callbackContext) {
        Log.d(TAG, "DeepLinkPlugin: addHandler...");
        handler = callbackContext;
        consumeEvents();
    }

    private JSONObject _bundleToJson(Bundle bundle) {
        Log.d(TAG, "DeepLinkPlugin: _bundleToJson...");
        if (bundle == null) {
            return new JSONObject();
        }

        JSONObject j = new JSONObject();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            try {
                Class<?> jsonClass = j.getClass();
                Class[] cArg = new Class[1];
                cArg[0] = String.class;
                //Workaround for API < 19
                try {
                    jsonClass.getDeclaredMethod("wrap", cArg);
                    j.put(key, JSONObject.wrap(bundle.get(key)));
                } catch (NoSuchMethodException e) {
                    j.put(key, this._wrap(bundle.get(key)));
                }
            } catch (JSONException ex) {
                Log.e(TAG, "Unable to wrap key: " + key, ex);
            }
        }

        return j;
    }

    //Wrap method not available in JSONObject API < 19
    private Object _wrap(Object o) {
        Log.d(TAG, "DeepLinkPlugin: _wrap...");
        if (o == null) {
            return null;
        }
        if (o instanceof JSONArray || o instanceof JSONObject) {
            return o;
        }
        try {
            if (o instanceof Collection) {
                return new JSONArray((Collection) o);
            } else if (o.getClass().isArray()) {
                return new JSONArray(o);
            }
            if (o instanceof Map) {
                return new JSONObject((Map) o);
            }
            if (o instanceof Boolean ||
                    o instanceof Byte ||
                    o instanceof Character ||
                    o instanceof Double ||
                    o instanceof Float ||
                    o instanceof Integer ||
                    o instanceof Long ||
                    o instanceof Short ||
                    o instanceof String) {
                return o;
            }
            if (Objects.requireNonNull(o.getClass().getPackage()).getName().startsWith("java.")) {
                return o.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Check if we can open an app with a given URI scheme.
     * <p>
     * Thanks to <a href="https://github.com/ohh2ahh/AppAvailability/blob/master/src/android/AppAvailability.java">...</a>
     */
    private void canOpenApp(String uri, final CallbackContext callbackContext) {
        Log.d(TAG, "DeepLinkPlugin: canOpenApp...");
        Context ctx = this.cordova.getActivity().getApplicationContext();
        final PackageManager pm = ctx.getPackageManager();

        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            callbackContext.success();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found: " + uri, e);
        }

        callbackContext.error("");
    }

    private void getHardwareInfo(JSONArray args, final CallbackContext callbackContext) {
        Log.d(TAG, "DeepLinkPlugin: getHardwareInfo...");
        String uuid = Settings.Secure.getString(this.cordova.getActivity().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        JSONObject j = new JSONObject();
        try {
            j.put("uuid", uuid);
            j.put("platform", this.getPlatform());
            j.put("tz", this.getTimeZoneID());
            j.put("tz_offset", this.getTimeZoneOffset());
            j.put("os_version", this.getOSVersion());
            j.put("sdk_version", this.getSDKVersion());
        } catch (JSONException ex) {
            Log.e(TAG, "Unable to get hardware info", ex);
        }

        final PluginResult result = new PluginResult(PluginResult.Status.OK, j);
        callbackContext.sendPluginResult(result);
    }

    private boolean isAmazonDevice() {
        Log.d(TAG, "DeepLinkPlugin: isAmazonDevice...");
        return Build.MANUFACTURER.equals("Amazon");
    }

    private String getTimeZoneID() {
        Log.d(TAG, "DeepLinkPlugin: getTimeZoneID...");
        TimeZone tz = TimeZone.getDefault();
        return (tz.getID());
    }

    private int getTimeZoneOffset() {
        Log.d(TAG, "DeepLinkPlugin: getTimeZoneOffset...");
        TimeZone tz = TimeZone.getDefault();
        return tz.getOffset(new Date().getTime()) / 1000 / 60;
    }

    private String getSDKVersion() {
        Log.d(TAG, "DeepLinkPlugin: getSDKVersion...");
        return Build.VERSION.SDK_INT + "";
    }

    private String getOSVersion() {
        Log.d(TAG, "DeepLinkPlugin: getOSVersion...");
        return Build.VERSION.RELEASE;
    }

    private String getPlatform() {
        Log.d(TAG, "DeepLinkPlugin: getPlatform...");
        String platform;
        if (isAmazonDevice()) {
            platform = "amazon-fireos";
        } else {
            platform = "android";
        }
        return platform;
    }
}
