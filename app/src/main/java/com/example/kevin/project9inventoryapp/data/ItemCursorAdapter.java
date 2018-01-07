package com.example.kevin.project9inventoryapp.data;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.kevin.project9inventoryapp.R;
import com.example.kevin.project9inventoryapp.data.ItemContract.ItemEntry;

/**
 * Created by Kevin on 12/4/2017.
 */

public class ItemCursorAdapter extends CursorAdapter {

    public ItemCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        // Find fields to populate in inflated template
        TextView tvName = (TextView) view.findViewById(R.id.item_name);
        TextView tvPrice = (TextView) view.findViewById(R.id.item_price);
        final TextView tvStock = (TextView) view.findViewById(R.id.item_quantity);
        Button btnSale = (Button) view.findViewById(R.id.item_sale_button);

        // Extract properties from cursor
        String name = cursor.getString(cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_NAME));
        float price = cursor.getFloat(cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_PRICE));
        int stock = cursor.getInt(cursor.getColumnIndexOrThrow(ItemEntry.COLUMN_ITEM_STOCK));

        // Get position of the cursor
        final int position = cursor.getPosition();

        // Populate the fields with the extracted properties
        tvName.setText(name);
        String formattedPriceText = "$" + String.valueOf(price);
        tvPrice.setText(formattedPriceText);
        tvStock.setText(String.valueOf(stock));

        // Setup Sale button to decrement stock
        btnSale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStock(context, cursor, tvStock, position);
            }
        });


    }

    private void updateStock(Context context, Cursor cursor, TextView tvStock, int position) {
        cursor.moveToPosition(position);
        ContentValues values = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(cursor, values);
        int updatedStock = decrement(values.getAsInteger("stock"));
        values.put(ItemEntry.COLUMN_ITEM_STOCK, updatedStock);
        System.out.println(values.getAsString("name"));
        Uri itemUri = ContentUris.withAppendedId(ItemEntry.CONTENT_URI, values.getAsLong("_id"));
        int updatedStockUri = context.getContentResolver().update(itemUri, values, null, null);
        tvStock.setText(String.valueOf(updatedStock));
    }

    private int decrement(int stock) {
        if (stock == 0) {
            return stock;
        }
        return stock - 1;
    }
}
