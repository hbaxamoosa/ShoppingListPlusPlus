package com.udacity.firebase.shoppinglistplusplus;

import android.content.Context;

import com.facebook.stetho.Stetho;

import timber.log.Timber;

/**
 * Includes one-time initialization of Firebase related code
 */
public class ShoppingListApplication extends android.app.Application {

    private static Context context;

    public static Context getAppContext() {
        return ShoppingListApplication.context;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ShoppingListApplication.context = getApplicationContext();

        //Including Jake Wharton's Timber logging library
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            Timber.v("Timber.plant(new Timber.DebugTree());");
        } else {
            // Timber.plant(new CrashReportingTree());
        }

        // Facebook Stetho
        Stetho.initializeWithDefaults(this);
    }
}