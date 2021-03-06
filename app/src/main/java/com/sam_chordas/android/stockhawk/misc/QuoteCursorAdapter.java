package com.sam_chordas.android.stockhawk.misc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperAdapter;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperViewHolder;

/**
 * Created by sam_chordas on 10/6/15.
 *  Credit to skyfishjy gist:
 *    https://gist.github.com/skyfishjy/443b7448f59be978bc59
 * for the code structure
 */
public class QuoteCursorAdapter extends CursorRecyclerViewAdapter<QuoteCursorAdapter.ViewHolder>
        implements ItemTouchHelperAdapter{

    private Context mContext;
    private static Typeface robotoLight;
    private boolean isPercent;
    public QuoteCursorAdapter(Context context, Cursor cursor){
        super(context, cursor);
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        robotoLight = Typeface.createFromAsset(mContext.getAssets(), "fonts/Roboto-Light.ttf");
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_quote, parent, false);
        return new ViewHolder(itemView);
    }

    @SuppressLint("NewApi")
    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final Cursor cursor){
        final String symbolText = cursor.getString(cursor.getColumnIndex("symbol"));
        viewHolder.symbol.setText(symbolText);
        ((LinearLayout)viewHolder.symbol.getParent()).setContentDescription(
                mContext.getString(R.string.listitem_content_desc) +
                        symbolText +
                        mContext.getString(R.string.listitem_content_desc_end)
        );
        viewHolder.bidPrice.setText(cursor.getString(cursor.getColumnIndex("bid_price")));
//        int sdk = Build.VERSION.SDK_INT;
        if (cursor.getInt(cursor.getColumnIndex("is_up")) == 1){
//            if (sdk < Build.VERSION_CODES.JELLY_BEAN){
//                viewHolder.change.setBackgroundDrawable(
                viewHolder.change.setBackground(
                        ContextCompat.getDrawable(mContext, R.drawable.percent_change_pill_green));
//                        mContext.getResources().getDrawable(R.drawable.percent_change_pill_green));
//            }else {
//                viewHolder.change.setBackground(
////                        mContext.getResources().getDrawable(R.drawable.percent_change_pill_green));
//                        ContextCompat.getDrawable(mContext, R.drawable.percent_change_pill_green));
//            }
        } else{
//            if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
//                viewHolder.change.setBackgroundDrawable(
                viewHolder.change.setBackground(
//                        mContext.getResources().getDrawable(R.drawable.percent_change_pill_red));
                        ContextCompat.getDrawable(mContext, R.drawable.percent_change_pill_red));
//            } else{
//                viewHolder.change.setBackground(
////                        mContext.getResources().getDrawable(R.drawable.percent_change_pill_red));
//                        ContextCompat.getDrawable(mContext, R.drawable.percent_change_pill_red));
//            }
        }
        if (Utils.showPercent){
            viewHolder.change.setText(cursor.getString(cursor.getColumnIndex("percent_change")));
        } else{
            viewHolder.change.setText(cursor.getString(cursor.getColumnIndex("change")));
        }
    }

    @Override public void onItemDismiss(int position) {
        Cursor c = getCursor();
        c.moveToPosition(position);
        String symbol = c.getString(c.getColumnIndex(QuoteColumns.SYMBOL));
        mContext.getContentResolver().delete(QuoteProvider.Quotes.withSymbol(symbol), null, null);
        notifyItemRemoved(position);

        Intent dataUpdatedIntent = new Intent(Utils.ACTION_DATA_UPDATED)
                .setPackage(mContext.getPackageName());
        mContext.sendBroadcast(dataUpdatedIntent);
    }

    @Override public int getItemCount() {
        return super.getItemCount();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
            implements ItemTouchHelperViewHolder, View.OnClickListener{
        public final TextView symbol;
        public final TextView bidPrice;
        public final TextView change;
        public ViewHolder(View itemView){
            super(itemView);
//            itemView.setContentDescription("Details of stock: " + );
            symbol = (TextView) itemView.findViewById(R.id.stock_symbol);
            symbol.setTypeface(robotoLight);
            bidPrice = (TextView) itemView.findViewById(R.id.bid_price);
            change = (TextView) itemView.findViewById(R.id.change);
        }

        @Override
        public void onItemSelected(){
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear(){
            itemView.setBackgroundColor(0);
        }

        @Override
        public void onClick(View v) {
//            Log.i("Adapter", "Got a click");
//            Cursor c = getCursor();
//            c.moveToPosition(getAdapterPosition());
//            String symbol = c.getString(c.getColumnIndex("symbol"));
////            String symbol = (String) ((TextView) itemView.findViewById(R.id.stock_symbol)).getText();
//            Intent detailIntent = new Intent(mContext, GraphDetailActivity.class);
//            detailIntent.setData(QuoteProvider.Quotes.withSymbol(symbol));
//            mContext.startActivity(detailIntent);
        }
    }
}
