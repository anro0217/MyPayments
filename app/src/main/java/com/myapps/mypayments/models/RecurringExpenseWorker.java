package com.myapps.mypayments.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class RecurringExpenseWorker extends Worker {

    public RecurringExpenseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Kinyerjük a feladathoz szükséges adatokat
        double amount = getInputData().getDouble("amount", 0.0);
        String description = getInputData().getString("description");
        int dayOfMonth = getInputData().getInt("dayOfMonth", 1);

        // Adatbázis frissítése vagy más szükséges tevékenység
        updateDatabase(amount, description);

        // Újraütemezés a következő hónapra
        scheduleNextRun(dayOfMonth, amount, description);

        return Result.success();
    }

    private void updateDatabase(double amount, String description) {
        // Itt kezeld az adatbázis frissítését
    }

    private void scheduleNextRun(int dayOfMonth, double amount, String description) {
        Calendar nextRun = Calendar.getInstance();
        nextRun.add(Calendar.MONTH, 1);
        nextRun.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        long delay = nextRun.getTimeInMillis() - System.currentTimeMillis();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(RecurringExpenseWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder()
                        .putDouble("amount", amount)
                        .putString("description", description)
                        .putInt("dayOfMonth", dayOfMonth)
                        .build())
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(request);
    }
}
