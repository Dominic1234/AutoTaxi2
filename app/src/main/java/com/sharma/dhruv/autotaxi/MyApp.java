package com.sharma.dhruv.autotaxi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class MyApp extends android.app.Application
{
    private static MyApp instance;

    public MyApp() {
        super();
        instance = this;
    }

    public void onCreate() {
        super.onCreate();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
    /**
     * Get the singleton instance of the Application subclass
     * @return
     */
    public static MyApp getInstance() {
        return instance;
    }

    /**
     * Get this app's application context
     * @return The Apps Context
     */
    public static Context getContext() {
        return instance.getApplicationContext();
    }

}