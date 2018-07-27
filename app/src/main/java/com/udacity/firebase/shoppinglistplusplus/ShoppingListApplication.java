package com.udacity.firebase.shoppinglistplusplus;

import com.facebook.stetho.Stetho;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;

import timber.log.Timber;

/**
 * Includes one-time initialization of Firebase related code
 */
public class ShoppingListApplication extends android.app.Application {

    // private static Context context;

    /*public static Context getAppContext() {
        return ShoppingListApplication.context;
    }*/

    @Override
    public void onCreate() {
        super.onCreate();

        /*
         * set Firebase database disk persistence to TRUE
         */
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // ShoppingListApplication.context = getApplicationContext();


        if (BuildConfig.DEBUG) {

            //Including Jake Wharton's Timber logging library
            Timber.plant(new Timber.DebugTree());
            Timber.v("Timber.plant(new Timber.DebugTree());");

            /*
             * set Firebase database LOGGER level to DEBUG
            */
            FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);

        } else {
            // Timber.plant(new CrashReportingTree());
        }

        // Facebook Stetho
        Stetho.initializeWithDefaults(this);
    }
}