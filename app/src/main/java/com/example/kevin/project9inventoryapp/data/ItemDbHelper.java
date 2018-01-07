package com.example.kevin.project9inventoryapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.kevin.project9inventoryapp.data.ItemContract.ItemEntry;

/**
 * Created by Kevin on 12/3/2017.
 */

public class ItemDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "inventory.db";

    public ItemDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_ITEMS_TABLE =
                "CREATE TABLE " + ItemEntry.TABLE_NAME + " (" +
                        ItemEntry._ID + " INTEGER PRIMARY KEY," +
                        ItemEntry.COLUMN_ITEM_NAME + " TEXT," +
                        ItemEntry.COLUMN_ITEM_PRICE + " REAL," +
                        ItemEntry.COLUMN_ITEM_STOCK + " INTEGER," +
                        ItemEntry.COLUMN_ITEM_DESCRIPTION + " TEXT," +
                        ItemEntry.COLUMN_ITEM_IMAGE + " TEXT);";
        db.execSQL(SQL_CREATE_ITEMS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String SQL_DELETE_ITEMS_TABLE =
                "DROP TABLE IF EXISTS " + ItemEntry.TABLE_NAME;
        db.execSQL(SQL_DELETE_ITEMS_TABLE);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
