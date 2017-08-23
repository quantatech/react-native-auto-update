package com.xzper.updater;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import ezy.boost.update.UpdateManager;

public class UpdaterModule extends ReactContextBaseJavaModule {

    private ReactApplicationContext context;

    public UpdaterModule(ReactApplicationContext reactContext) {
        super(reactContext);
        context = reactContext;
    }

    @Override
    public String getName() {
        return "Updater";
    }

    @ReactMethod
    public void setUrl(String url, String channel) {
        UpdateManager.setUrl(url, channel);
    }

    @ReactMethod
    public void check() {
        UpdateManager.check(context);
    }

    @ReactMethod
    public void checkManual() {
        UpdateManager.checkManual(context);
    }
}