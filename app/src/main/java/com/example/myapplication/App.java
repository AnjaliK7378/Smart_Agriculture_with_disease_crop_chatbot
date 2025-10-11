package com.example.myapplication;

import android.app.Application;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        setupWorkManager();
        NotificationHelper.createNotificationChannel(this);
    }

    private void setupWorkManager() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest =
                new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "dataSyncWork",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }
}