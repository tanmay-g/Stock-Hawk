package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.TaskParams;

/**
 * Created by sam_chordas on 10/1/15.
 */
public class StockIntentService extends IntentService {

    private Handler mHandler;

    public StockIntentService(){
        super(StockIntentService.class.getName());
    }

    public StockIntentService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
    }

    @Override protected void onHandleIntent(Intent intent) {
        Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");
        StockTaskService stockTaskService = new StockTaskService(this);
        Bundle args = new Bundle();
        if (intent.getStringExtra("tag").equals("add")){
            args.putString("symbol", intent.getStringExtra("symbol"));
        }
        // We can call OnRunTask from the intent service to force it to run immediately instead of
        // scheduling a task.
        int result = stockTaskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args));

        String toastText = null;
        if (result == StockTaskService.INVALID_STOCK_NAME)
            toastText = "Invalid stock name";
        else if (result == StockTaskService.NETWORK_FAILURE)
            toastText = "Network failure. Try again soon";

        if (toastText != null) {
            final String finalToastText = toastText;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(StockIntentService.this, finalToastText, Toast.LENGTH_LONG).show();
                }
            });
        }

    }
}
