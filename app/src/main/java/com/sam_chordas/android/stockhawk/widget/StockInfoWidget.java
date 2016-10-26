package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.misc.Utils;
import com.sam_chordas.android.stockhawk.ui.GraphDetailActivity;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

/**
 * Implementation of App Widget functionality.
 */
public class StockInfoWidget extends AppWidgetProvider {

    private static final String LOG_TAG = StockInfoWidget.class.getSimpleName();

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                         int appWidgetId) {

        CharSequence widgetText = context.getString(R.string.appwidget_title_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stock_info_widget);

        // Create an Intent to launch MainActivity
        Intent intent = new Intent(context, MyStocksActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.appwidget_title, pendingIntent);

        views.setTextViewText(R.id.appwidget_title, widgetText);
        views.setEmptyView(R.id.appwidget_stock_list, R.id.empty);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            setRemoteAdapter(context, views);
        } else {
            setRemoteAdapterV11(context, views);
        }

        Intent clickIntentTemplate = new Intent(context, GraphDetailActivity.class);
        PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(clickIntentTemplate)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.appwidget_stock_list, clickPendingIntentTemplate);
        //TODO add empty view

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (Utils.ACTION_DATA_UPDATED.equals(intent.getAction())){
            Log.w(LOG_TAG, "onReceive: Got UPDATE SIGNAL");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, getClass()));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.appwidget_stock_list);
        }
    }

    /**
     * Sets the remote adapter used to fill in the list items
     *
     * @param views RemoteViews to set the RemoteAdapter
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setRemoteAdapter(Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(R.id.appwidget_stock_list,
                new Intent(context, StockInfoWidgetRemoteViewsService.class));
    }

    /**
     * Sets the remote adapter used to fill in the list items
     *
     * @param views RemoteViews to set the RemoteAdapter
     */
    @SuppressWarnings("deprecation")
    private void setRemoteAdapterV11(Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(0, R.id.appwidget_stock_list,
                new Intent(context, StockInfoWidgetRemoteViewsService.class));
    }
}

