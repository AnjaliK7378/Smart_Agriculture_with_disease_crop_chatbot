package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SyncWorker extends Worker {

    // Constructor
    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    // doWork() method is where background work is done
    @NonNull
    @Override
    public Result doWork() {
        // This is where you perform your background task (syncing)
        Log.d("SyncWorker", "Syncing data...");

        // Simulate work (for example, syncing data with the server)
        try {
            // Simulate a network call or some background work
            Thread.sleep(2000);  // Simulate 2 seconds of work
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Result.failure();  // Return failure if interrupted
        }

        // If successful, return Result.success()
        return Result.success();
    }
}