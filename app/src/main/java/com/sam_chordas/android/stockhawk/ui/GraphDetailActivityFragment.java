package com.sam_chordas.android.stockhawk.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.service.HistoricalDataLoader;

import java.util.Arrays;

/**
 * A placeholder fragment containing a simple view.
 */
public class GraphDetailActivityFragment extends Fragment {

    private static final String LOG_TAG = GraphDetailActivityFragment.class.getName();
    private final Handler mHandler;

    private LineChartView mChartView;
    private TabLayout mTabLayout;
    private static final String SELECTED_TAB_KEY = "SELECTED_TAB_KEY";
    private static final String YEAR_DATA_KEY = "YEAR_DATA_KEY";

    public GraphDetailActivityFragment() {
        mHandler = new Handler();
    }

    private String mSymbol;
    private int mSelectedTab = -1;
    private float[] mYearData;
    private static final int HISTORIC_DATA_LOADER_ID = 1;

    private LoaderManager.LoaderCallbacks dataLoaderCallback = new LoaderManager.LoaderCallbacks<float[]>() {
        @Override
        public HistoricalDataLoader onCreateLoader(int id, Bundle args) {
            Log.i(LOG_TAG, "onCreateLoader: " + mSymbol);
            args = new Bundle(1);
            args.putString(HistoricalDataLoader.SYMBOL_KEY, mSymbol);
            return new HistoricalDataLoader(getActivity(), args);
        }

        @Override
        public void onLoadFinished(Loader<float[]> loader, float[] data) {
            Log.i(LOG_TAG, "onLoadFinished: will now set mYearData");
            mYearData = data;
            if (data != null) {
                if (mSelectedTab != -1)
                    mTabLayout.getTabAt(mSelectedTab).select();
                else
                    mTabLayout.getTabAt(2).select();
            }
            else{
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        getActivity().onBackPressed();
                    }
                });
            }
        }

        @Override
        public void onLoaderReset(Loader loader) {
            loader.reset();
        }
    };

    private TabLayout.OnTabSelectedListener tabListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            updateTab(tab);
        }

        private int round (int num, int factor){
//            return(int )( factor * Math.ceil(num / factor));
            return factor * ((num + factor -1) / factor);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            mChartView.dismiss();
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
//            tab.select();
            if (tab.getPosition() == 0)
                updateTab(tab);
        }
    };

    private void updateTab(TabLayout.Tab tab){
        mSelectedTab = tab.getPosition();
        Log.i(LOG_TAG, "onTabSelected: " + mSelectedTab);
//        if (mYearData == null){
//            Log.i(LOG_TAG, "onTabSelected: Reached here with null data");
//            Toast.makeText(getActivity(), "No data found for this stock", Toast.LENGTH_LONG).show();
//            mHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    getActivity().onBackPressed();
//                }
//            });
//            return;
//        }
        float[] dataToUse;
        String[] textLabels;
        switch (tab.getPosition()){
            case 0:{
                //1 month
                dataToUse = Arrays.copyOfRange(mYearData, mYearData.length - Math.round(mYearData.length/12), mYearData.length);
                textLabels = new String[]{
                        getString(R.string.one_month_graph_max_label),
                        getString(R.string.one_month_graph_mid_label),
                        getString(R.string.one_month_graph_min_label)};
                break;
            }
            case 1:{
                //6 months
                dataToUse = Arrays.copyOfRange(mYearData, mYearData.length - Math.round(mYearData.length/4), mYearData.length);
                textLabels = new String[]{
                        getString(R.string.three_month_graph_max_label),
                        getString(R.string.three_month_graph_mid_label),
                        getString(R.string.three_month_graph_min_label)};
                break;
            }
            case 2:{
                //1 year
                dataToUse = mYearData;
                textLabels = new String[]{
                        getString(R.string.one_year_graph_max_label),
                        getString(R.string.one_year_graph_mid_label),
                        getString(R.string.one_year_graph_min_label)};
                break;
            }
            default:{
                Log.wtf(LOG_TAG, "onTabSelected: What tab is this: " + tab.getPosition() );
                //set empty view visible?
                return;
            }
        }

        float max = Float.MIN_VALUE, min = Float.MAX_VALUE;
        String[] labels = new String[dataToUse.length];
        for (int i = 0; i < dataToUse.length; i++) {

            if (dataToUse[i] < min)
                min = dataToUse[i];
            if (dataToUse[i] > max)
                max = dataToUse[i];

            if (i == 0)
                labels[i] = textLabels[0];
            else if (i== dataToUse.length /2)
                labels[i] = textLabels[1];
            else if (i== dataToUse.length-1 )
                labels[i] = textLabels[2];
            else
                labels[i] = "";
        }


//            Log.i(LOG_TAG, Arrays.toString(mYearData));
        LineSet chartData = new LineSet(labels, dataToUse);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            chartData.setColor(getResources().getColor(R.color.white, getActivity().getTheme()));
//        }
        chartData.setColor(ContextCompat.getColor(getActivity(), R.color.white));
        chartData.setThickness(2);
        mChartView.setAxisColor(ContextCompat.getColor(getActivity(), R.color.white));
        mChartView.setLabelsColor(ContextCompat.getColor(getActivity(), R.color.white));
        mChartView.dismiss();
        mChartView.addData(chartData);
//            mChartView.setAxisBorderValues(Math.round(min - ((max-min)/2)), Math.round(max + ((max-min)/2)));
        int diff = (int) ((max-min)/2);
        float bottom = min - diff, top = max + diff;
//            top = top % 2 == 0? top : top -1;
//            bottom = bottom % 2 == 0? bottom : bottom -1;
        int numLabels = 8;
        if (top - bottom < 8)
            numLabels = 4;
        int itop = (int)(numLabels * Math.ceil(top / numLabels));
        int ibottom = (int)(numLabels * Math.floor(bottom / numLabels));
//            if (top == bottom)
//                top = bottom + numLabels;
        int step = (itop - ibottom)/ numLabels;
        Log.i(LOG_TAG, "onTabSelected: Max: " + max + " Min: " + min);
        Log.i(LOG_TAG, "onTabSelected: Top: " + itop + " Bottom: " + ibottom + " step: " + step);
        mChartView.setContentDescription(getString(R.string.graph_content_desc) + mSymbol);
        mChartView.setStep(step);
        mChartView.setAxisBorderValues(ibottom, itop);
//            mChartView.setStep(diff / 2);
        mChartView.show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i(LOG_TAG, "onCreateView");
        try {
            Uri symbolUri = getActivity().getIntent().getData();

            Cursor currentDataCursor = getActivity().getContentResolver().query(
                    symbolUri,
                    new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    null);
            if (!(currentDataCursor != null && currentDataCursor.moveToFirst()))
                    throw new Exception(getString(R.string.graph_data_not_found_toast_message));

            mSymbol = currentDataCursor.getString(currentDataCursor.getColumnIndex("symbol"));

            currentDataCursor.close();
            View rootView = inflater.inflate(R.layout.fragment_graph_detail, container, false);
            mChartView = (LineChartView) rootView.findViewById(R.id.linechart);
            Toolbar toolbarView = (Toolbar) rootView.findViewById(R.id.toolbar);
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if ( null != toolbarView ) {
                activity.setSupportActionBar(toolbarView);
                ActionBar actionBar = activity.getSupportActionBar();
                actionBar.setTitle(mSymbol);
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
            if (savedInstanceState != null)
                mYearData = savedInstanceState.getFloatArray(YEAR_DATA_KEY);
            mTabLayout = (TabLayout) rootView.findViewById(R.id.tabs);
            if (mTabLayout != null){
                mTabLayout.addOnTabSelectedListener(tabListener);
                if (savedInstanceState != null) {
                    mSelectedTab = savedInstanceState.getInt(SELECTED_TAB_KEY, 2);
                    mTabLayout.getTabAt(mSelectedTab).select();
                }
//                mTabLayout.getTabAt(2).select();
            }
            return rootView;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
            getActivity().onBackPressed();
            return null;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        if (savedInstanceState == null)
        if (mYearData == null)
        getLoaderManager().initLoader(HISTORIC_DATA_LOADER_ID, null, dataLoaderCallback);
    }

//    @Override
//    public void onResume() {
//        Log.i(LOG_TAG, "onResume: Will start Loader");
//        getLoaderManager().initLoader(HISTORIC_DATA_LOADER_ID, null, dataLoaderCallback);
//        super.onResume();
//    }

    @Override
    public void onDestroy() {
        if (mTabLayout != null){
            mTabLayout.removeOnTabSelectedListener(tabListener);
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_TAB_KEY, mTabLayout.getSelectedTabPosition());
        outState.putFloatArray(YEAR_DATA_KEY, mYearData);
        super.onSaveInstanceState(outState);
    }
}
