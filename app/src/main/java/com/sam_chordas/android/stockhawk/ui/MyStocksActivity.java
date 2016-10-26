package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.misc.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.misc.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.misc.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;
    boolean isConnected;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private BroadcastReceiver mRefreshCompleteBroadcastReciever;
    public final static String REFRESH_COMPLETE = "com.sam_chordas.android.stockhawk.refresh_complete";
    private RecyclerView mRecyclerView;
    private TextView mEmptyView;

    private void updateConnectivityState(){
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        updateConnectivityState();
        setContentView(R.layout.activity_my_stocks);
        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(this, StockIntentService.class);
        if (savedInstanceState == null){
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra("tag", "init");
            if (isConnected){
                startService(mServiceIntent);
            } else{
                networkToast();
            }
        }
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        mRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View v, int position) {
                        String symbol = (String) ((TextView) v.findViewById(R.id.stock_symbol)).getText();
                        Intent detailIntent = new Intent(mContext, GraphDetailActivity.class);
                        detailIntent.setData(QuoteProvider.Quotes.withSymbol(symbol));
                        mContext.startActivity(detailIntent);
                    }
                }));
        mRecyclerView.setAdapter(mCursorAdapter);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE)
                    fab.show();
                else
                    fab.hide();
                super.onScrollStateChanged(recyclerView, newState);
            }

//            @Override
//            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                if (abs(dy) > 10 && fab.isShown())
//                    fab.hide();
//                super.onScrolled(recyclerView, dx, dy);
//            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                launchRefreshIntent();
            }
        });

        mRefreshCompleteBroadcastReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        };

        mEmptyView = (TextView) findViewById(R.id.empty);

//        fab.attachToRecyclerView(mRecyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (isConnected){
                    new MaterialDialog.Builder(mContext)
                            .theme(Theme.LIGHT)
                            .title(R.string.symbol_search)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override public void onInput(MaterialDialog dialog, CharSequence input) {
                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly
                                    input = input.toString().toUpperCase();
                                    Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                            new String[] { QuoteColumns.SYMBOL }, QuoteColumns.SYMBOL + "= ?",
                                            new String[] { input.toString() }, null);
                                    if (c.getCount() != 0) {
                                        Toast toast =
                                                Toast.makeText(MyStocksActivity.this, "This stock is already saved!",
                                                        Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                        toast.show();
                                        return;
                                    } else {
                                        // Add the stock to DB
                                        mServiceIntent.putExtra("tag", "add");
                                        mServiceIntent.putExtra("symbol", input.toString());
                                        startService(mServiceIntent);
                                    }
                                }
                            })
                            .show();
                } else {
                    networkToast();
                }

            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        Toolbar toolbarView = (Toolbar) findViewById(R.id.toolbar);
        if ( null != toolbarView ) {
            setSupportActionBar(toolbarView);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        mTitle = getTitle();
        if (isConnected){
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
        registerReceiver(mRefreshCompleteBroadcastReciever, new IntentFilter(REFRESH_COMPLETE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mRefreshCompleteBroadcastReciever);
        mRecyclerView.clearOnScrollListeners();
    }

    public void networkToast(){
        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        MenuItem unitToggle = menu.findItem(R.id.action_change_units);
        setToggleIcon(unitToggle);
        restoreActionBar();
        return true;
    }

    private void setToggleIcon(MenuItem toggleIcon){
        if (Utils.showPercent)
            toggleIcon.setIcon(R.drawable.ic_percentage);
        else
            toggleIcon.setIcon(R.drawable.ic_attach_money_white_24dp);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            networkToast();
            return true;
        }

        if (id == R.id.action_change_units){
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            setToggleIcon(item);
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
            Intent dataUpdatedIntent = new Intent(Utils.ACTION_DATA_UPDATED)
                    .setPackage(mContext.getPackageName());
            mContext.sendBroadcast(dataUpdatedIntent);
        }

        if (id == R.id.menu_refresh){
            launchRefreshIntent();
        }

        return super.onOptionsItemSelected(item);
    }

    private void launchRefreshIntent(){
        updateConnectivityState();
        if (isConnected){
            Intent updateIntent = new Intent(MyStocksActivity.this, StockIntentService.class);
            updateIntent.putExtra("tag", "periodic");
            startService(updateIntent);
        } else{
            mSwipeRefreshLayout.setRefreshing(false);
            networkToast();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args){
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data){
        mCursorAdapter.swapCursor(data);
        updateEmptyView();
        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader){
        mCursorAdapter.swapCursor(null);
    }

    void updateEmptyView(){
        if (mCursorAdapter.getItemCount() == 0){
            int message = R.string.empty_message_default;
            if (!isConnected)
                message = R.string.empty_message_nonet;
            mEmptyView.setText(message);
            mEmptyView.setVisibility(View.VISIBLE);
        }
        else
            mEmptyView.setVisibility(View.GONE);
    }
}
