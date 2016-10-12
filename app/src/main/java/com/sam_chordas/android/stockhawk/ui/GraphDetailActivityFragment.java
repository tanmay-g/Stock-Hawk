package com.sam_chordas.android.stockhawk.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.service.HistoricalDataLoader;

import java.util.Arrays;

/**
 * A placeholder fragment containing a simple view.
 */
public class GraphDetailActivityFragment extends Fragment {

    private static final String LOG_TAG = GraphDetailActivityFragment.class.getName();

    public GraphDetailActivityFragment() {
    }

    private String mSymbol;
    private float[] mYearData;
    private static final int HISTORIC_DATA_LOADER_ID = 1;

    private LoaderManager.LoaderCallbacks dataLoaderCallback = new LoaderManager.LoaderCallbacks<float[]>() {
        @Override
        public HistoricalDataLoader onCreateLoader(int id, Bundle args) {
            Log.i(LOG_TAG, "onCreateLoader: ");
            args = new Bundle(1);
            args.putString(HistoricalDataLoader.SYMBOL_KEY, mSymbol);
            return new HistoricalDataLoader(getActivity(), args);
        }

        @Override
        public void onLoadFinished(Loader<float[]> loader, float[] data) {
            mYearData = data;
            if (mYearData == null){
                Toast.makeText(getActivity(), "No data found for this stock", Toast.LENGTH_LONG).show();
//                getActivity().onBackPressed();
            }

            Log.i(LOG_TAG, Arrays.toString(mYearData));

            //TODO update UI now
        }


        @Override
        public void onLoaderReset(Loader loader) {
            loader.reset();
        }
    };

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
                    throw new Exception("No data for this stock");

            mSymbol = currentDataCursor.getString(currentDataCursor.getColumnIndex("symbol"));

            //TODO possibly get other stock details to show on screen

            currentDataCursor.close();
            return inflater.inflate(R.layout.fragment_graph_detail, container, false);
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
        getLoaderManager().initLoader(HISTORIC_DATA_LOADER_ID, null, dataLoaderCallback);
    }
}
