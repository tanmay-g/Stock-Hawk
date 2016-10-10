package com.sam_chordas.android.stockhawk.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sam_chordas.android.stockhawk.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class GraphDetailActivityFragment extends Fragment {

    public GraphDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20where%20symbol%20%3D%20%22YHOO%22%20and%20startDate%20%3D%20%222009-09-11%22%20and%20endDate%20%3D%20%222010-03-10%22&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys

        return inflater.inflate(R.layout.fragment_graph_detail, container, false);
    }
}
