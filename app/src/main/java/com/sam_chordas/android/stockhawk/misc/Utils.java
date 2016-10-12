package com.sam_chordas.android.stockhawk.misc;

import android.accounts.NetworkErrorException;
import android.content.ContentProviderOperation;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();

    public static boolean showPercent = true;

    public static ArrayList quoteJsonToContentVals(String JSON) throws NetworkErrorException {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        try{
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0){
                if (jsonObject.has("error")){
                    throw new NetworkErrorException();
                    //In case of random error response, it will show a toast
                }
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1){
                    //need to check for null responses in here
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    ContentProviderOperation c = buildBatchOperation(jsonObject);
                    if (c == null)
                        return null;
                    batchOperations.add(c);
                } else{
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                    if (resultsArray != null && resultsArray.length() != 0){
                        ContentProviderOperation c;
                        for (int i = 0; i < resultsArray.length(); i++){
                            jsonObject = resultsArray.getJSONObject(i);
                            try {
                                c = buildBatchOperation(jsonObject);
                                if (c != null)
                                    batchOperations.add(c);
                            }
                            catch (NetworkErrorException n){
                                Log.v(LOG_TAG, "Got a network error in bulk update. Ignored");
                            }
                        }
                    }
                }
            }
        } catch (JSONException e){
            Log.e(LOG_TAG, "String to JSON failed: " + e);
            throw new NetworkErrorException();
        }
        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice){
        if (bidPrice.equals("null"))
            bidPrice = "0";
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange){
        String weight = change.substring(0,1);
        String ampersand = "";
        if (isPercentChange){
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) throws NetworkErrorException {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            if (jsonObject.getString("Name").equals("null"))
                return null;
            String change = jsonObject.getString("Change");
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString("ChangeinPercent"), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-'){
                builder.withValue(QuoteColumns.ISUP, 0);
            }else{
                builder.withValue(QuoteColumns.ISUP, 1);
            }

        } catch (Exception e){
            e.printStackTrace();
            throw new NetworkErrorException();
        }
        return builder.build();
    }

    public static float[] historicalDataToFloatArr (String response) throws NetworkErrorException{

        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        float[] results = null;
        try{
            jsonObject = new JSONObject(response);
            if (jsonObject != null && jsonObject.length() != 0){
                if (jsonObject.has("error")){
                    throw new NetworkErrorException();
                    //In case of random error response, it will show a toast and return
                }
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count < 1)
                    return null;
                resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                if (resultsArray != null && resultsArray.length() != 0){
                    results = new float[resultsArray.length()];
                    for (int i = 0; i < resultsArray.length(); i++){
                        jsonObject = resultsArray.getJSONObject(i);
                        results[i] = (float) jsonObject.getDouble("Open");

                    }
                }
            }
        }
         catch (JSONException e){
            Log.e(LOG_TAG, "String to JSON failed: " + e);
            throw new NetworkErrorException();
        }
        return results;
    }
}
