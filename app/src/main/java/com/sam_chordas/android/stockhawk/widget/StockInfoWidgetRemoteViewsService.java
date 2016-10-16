package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.misc.Utils;

public class StockInfoWidgetRemoteViewsService extends RemoteViewsService {

    public final String LOG_TAG = StockInfoWidgetRemoteViewsService.class.getSimpleName();

    private static final String[] STOCK_INFO_COLUMNS = {
            QuoteColumns._ID,
            QuoteColumns.SYMBOL,
            QuoteColumns.PERCENT_CHANGE,
            QuoteColumns.CHANGE,
            QuoteColumns.BIDPRICE,
            QuoteColumns.ISUP,
//            QuoteColumns.ISCURRENT,
    };
    // these indices must match the projection
    static final int INDEX_ID = 0;
    static final int INDEX_SYMBOL = 1;
    static final int INDEX_PERCENT_CHANGE = 2;
    static final int INDEX_CHANGE = 3;
    static final int INDEX_BID = 4;
    static final int INDEX_ISUP = 5;
//    static final int INDEX_ISCURR = 6;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {

            private Cursor data = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }

                final long identityToken = Binder.clearCallingIdentity();
                Uri stocksUri =
                        QuoteProvider.Quotes.CONTENT_URI;
                data = getContentResolver().query(stocksUri,
                        STOCK_INFO_COLUMNS,
                        QuoteColumns.ISCURRENT + "= ?",
                        new String[]{"1"},
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    Log.i(LOG_TAG, "getViewAt: " + position + " was invalid");
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.appwidget_list_item_quote);
                String symbol = data.getString(INDEX_SYMBOL);
                String bid = data.getString(INDEX_BID);
                String percentChange = data.getString(INDEX_PERCENT_CHANGE);
                String change = data.getString(INDEX_CHANGE);
                views.setTextViewText(R.id.stock_symbol, symbol);
                views.setTextViewText(R.id.bid_price, bid);

//                int sdk = Build.VERSION.SDK_INT;
                if (data.getInt(INDEX_ISUP) == 1){
                        views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
//                    if (sdk < Build.VERSION_CODES.JELLY_BEAN){
//                        views.setInt(R.id.change, "setBackgroundDrawable", R.drawable.percent_change_pill_green);
//                    }else {
//                        views.setInt(R.id.change, "setBackground", R.drawable.percent_change_pill_green);
//                        views.setInt(R.id.change, "setBackground", R.drawable.percent_change_pill_green);
//                    }
                } else{
                    views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
//                    if (sdk < Build.VERSION_CODES.JELLY_BEAN){
//                        views.setInt(R.id.change, "setBackgroundDrawable", R.drawable.percent_change_pill_red);
//                    }else {
//                        views.setInt(R.id.change, "setBackground", R.drawable.percent_change_pill_red);
//                    }
                }

                if (Utils.showPercent){
                    views.setTextViewText(R.id.change, percentChange);
                } else{
                    views.setTextViewText(R.id.change, change);
                }

                final Intent fillInIntent = new Intent();
                Uri stockUri = QuoteProvider.Quotes.withSymbol(symbol);
                fillInIntent.setData(stockUri);
                views.setOnClickFillInIntent(R.id.stock_widget_item, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.appwidget_list_item_quote);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(INDEX_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
