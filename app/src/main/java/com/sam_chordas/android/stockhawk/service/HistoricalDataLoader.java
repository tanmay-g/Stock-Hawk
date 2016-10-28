package com.sam_chordas.android.stockhawk.service;

import android.accounts.NetworkErrorException;
import android.annotation.SuppressLint;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.misc.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by tanmay.godbole on 11-10-2016
 */

public class HistoricalDataLoader extends AsyncTaskLoader<float[]> {

    private static final String LOG_TAG = HistoricalDataLoader.class.getName();
    private boolean hasResult = false;
    private final String mSymbol;
    public static final String SYMBOL_KEY = "SYMBOL_KEY";

    private float[] mResults;

    private OkHttpClient client = new OkHttpClient();

    private Handler mHandler;
    private Context mContext;

    public HistoricalDataLoader(Context context, Bundle args) {
        super(context);
        mContext = context;
        mSymbol = args.getString(SYMBOL_KEY);
        mHandler = new Handler();
    }

    @Override
    protected void onStartLoading() {
        if (hasResult) {
            //this instance has already finished loading
            deliverResult(mResults);
        }
        else {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(float[] data) {
        mResults = data;
        hasResult = true;
        super.deliverResult(data);
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        if (hasResult){
            mResults = null;
            hasResult = false;
        }
    }

    @Override
    public float[] loadInBackground() {

        Log.i(LOG_TAG, "loadInBackground: Starting");

        final String BASE_YQL_ADDRESS = "https://query.yahooapis.com/v1/public/yql?q=";
        final String YQL_SUFFIX = "&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
        String toastText = null;

        try {
            Calendar calendarToday = new GregorianCalendar();

            Calendar calendarYearAgo = new GregorianCalendar();
            calendarYearAgo.add(Calendar.YEAR, -1);
            @SuppressLint("SimpleDateFormat") SimpleDateFormat tf = new SimpleDateFormat("yyyy-MM-dd");

            String queryUri = BASE_YQL_ADDRESS;
            Log.i(LOG_TAG, "loadInBackground: " + tf.format(calendarToday.getTime()) + " : " + tf.format(calendarYearAgo.getTime()));
            try {
                queryUri = queryUri +
                        URLEncoder.encode(
                                "select * from yahoo.finance.historicaldata where symbol = \"" +
                                        mSymbol +
                                        "\" and startDate = \"" +
                                        tf.format(calendarYearAgo.getTime()) +
                                        "\" and endDate = \"" +
                                        tf.format(calendarToday.getTime()) +
                                        "\"",
                                "UTF-8")
                ;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String request = queryUri + YQL_SUFFIX;
            Log.i(LOG_TAG, "Requesting: " + request);
            String response = fetchData(request);
            Log.i(LOG_TAG, "Response: " + response);

            mResults = Utils.historicalDataToFloatArr(response);
            if (mResults == null)
                toastText = mContext.getString(R.string.graph_no_data_toast_message);
            else
                return mResults;

        } catch (IOException | NetworkErrorException e) {
            Log.e(LOG_TAG, "loadInBackground: ", e);
//            e.printStackTrace();
            toastText = mContext.getString(R.string.graph_error_toast_message);
        }

        if (toastText != null) {
            final String finalToastText = toastText;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, finalToastText, Toast.LENGTH_LONG).show();
                }
            });
        }
        return null;
    }

    private String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }
}
