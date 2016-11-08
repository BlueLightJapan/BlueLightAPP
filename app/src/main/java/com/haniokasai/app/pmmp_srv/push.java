package com.haniokasai.app.pmmp_srv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by hani on 2016/10/06.
 */
public class push extends BroadcastReceiver {
    private FirebaseAnalytics mFirebaseAnalytics;
        @Override
        public void onReceive(final Context context, Intent intent) {
            // ここに起動時の処理を書く
            System.out.print("hello !!");

            Handler mHandler = new Handler();
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    // Obtain the FirebaseAnalytics instance.
                    mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);

                    Bundle fireLogBundle = new Bundle();
                    fireLogBundle.putString("TEST", "FireSample app MainActivity.onCreate() is called.");
                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, fireLogBundle);
                }
            };
            mHandler.post(r);
        }
    }

