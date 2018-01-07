package com.example.kevin.project9inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.example.kevin.project9inventoryapp.R;
import com.example.kevin.project9inventoryapp.data.ItemContract.ItemEntry;

/**
 * Created by Kevin on 12/3/2017.
 */

public class ItemProvider extends ContentProvider {
    // Tag for the log messages
    public static final String LOG_TAG = ItemProvider.class.getName();

    // Database helper
    private ItemDbHelper mDbHelper;

    // URI matcher code for the content URI for the pets table
    private static final int ITEMS = 100;

    // URI matcher code for the content URI for a single item in the inventory table
    private static final int ITEM_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        /**
         * The calls to addURI() go here, for all of the content URI patterns that the provider
         * should recognize. All paths added to the UriMatcher have a corresponding code to return
         * when a match is found
         */
        sUriMatcher.addURI(ItemContract.CONTENT_AUTHORITY, ItemContract.PATH_ITEMS, ITEMS);
        sUriMatcher.addURI(ItemContract.CONTENT_AUTHORITY, ItemContract.PATH_ITEMS+"/#", ITEM_ID);
    }

    // Initialize the provider and the database helper object.
    @Override
    public boolean onCreate() {
        mDbHelper = new ItemDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection
     * arguments, and sort order.
     * @param uri
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @return
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        Cursor cursor;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                cursor = database.query(ItemEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case ITEM_ID:
                selection = ItemEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                cursor = database.query(ItemEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        /**
         * Set notification URI on the cursor,
         * so we know what content URI the cursor was created for.
         * If the data at this URI changes, then we know we need to update the Cursor.
         */
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    /**
     * Return the MIME type of data for the content URI.
     * @param uri
     * @return
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return ItemEntry.CONTENT_LIST_TYPE;
            case ITEM_ID:
                return ItemEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     * @param uri
     * @param values
     * @return
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return insertItem(uri, values);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    private Uri insertItem(Uri uri, ContentValues values) {
        int isThereSomethingMissingOrWrong = 0;
        // Check that the name is not null
        String name = values.getAsString(ItemEntry.COLUMN_ITEM_NAME);
        if (name == null || name.equals("")) {
            Toast.makeText(getContext(), R.string.text_name_required, Toast.LENGTH_SHORT).show();
            isThereSomethingMissingOrWrong = 1;
        }
        // Check that the price is not negative
        float price = values.getAsFloat(ItemEntry.COLUMN_ITEM_PRICE);
        if (price < 0) {
            Toast.makeText(getContext(), R.string.text_price_non_negative, Toast.LENGTH_SHORT).show();
            isThereSomethingMissingOrWrong = 1;
        }
        // Check that the description is not null
        String desc = values.getAsString(ItemEntry.COLUMN_ITEM_DESCRIPTION);
        if (desc == null || desc.equals("")) {
            Toast.makeText(getContext(), R.string.text_description_required, Toast.LENGTH_SHORT).show();
            isThereSomethingMissingOrWrong = 1;
        }
        // Check that the stock is not negative
        Integer stock = values.getAsInteger(ItemEntry.COLUMN_ITEM_STOCK);
        if (stock < 0) {
            Toast.makeText(getContext(), R.string.text_stock_non_negative, Toast.LENGTH_SHORT).show();
            isThereSomethingMissingOrWrong = 1;
        }
        // Check that the image is not null
        String img = values.getAsString(ItemEntry.COLUMN_ITEM_IMAGE);
        if (img == null || img.equals("")) {
            Toast.makeText(getContext(), R.string.text_item_image_required, Toast.LENGTH_SHORT).show();
            isThereSomethingMissingOrWrong = 1;
        }

        if (isThereSomethingMissingOrWrong == 1) {
            return null;
        }


        // Insert a new item into the database table with the given ContentValues
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        long id = database.insert(ItemEntry.TABLE_NAME, null, values);

        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the item content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Once we know the ID of the new row in the table, return the new URI
        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * Delete the data at the given selection and selection arguments.
     * @param uri
     * @param selection
     * @param selectionArgs
     * @return rowsDeleted, the number of rows deleted.
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(ItemEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case ITEM_ID:
                // Delete a single row given by the ID in the URI
                selection = ItemEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(ItemEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        /**
         * If 1 or more rows were deleted, then notify all listeners that the data at the given URI
         * has changed
         */
        if (rowsDeleted != 0) {
            // Notify all listeners that the data has changed for the item content URI
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return rowsDeleted
        return rowsDeleted;
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues,
     * @param uri
     * @param values
     * @param selection
     * @param selectionArgs
     * @return
     */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return updateItem(uri, values, selection, selectionArgs);
            case ITEM_ID:
                /**
                 * For the ITEM_ID code, extract out the ID from the URI,
                 * so we know which row to update. Selection will be "_id=?" and selection
                 * arguments will be a String array containing the actual ID.
                 */
                selection = ItemEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateItem(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update items in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more items).
     * @param uri
     * @param values
     * @param selection
     * @param selectionArgs
     * @return the number of rows that were successfully updated
     */
    private int updateItem(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int isThereSomethingMissingOrWrong = 0;

        // Check that the name is not null
        String name = values.getAsString(ItemEntry.COLUMN_ITEM_NAME);
        if (name == null || name.equals("")) {
            Toast.makeText(getContext(), R.string.text_name_required, Toast.LENGTH_SHORT).show();
            isThereSomethingMissingOrWrong = 1;
        }
        // Check that the price is not negative
        float price = values.getAsFloat(ItemEntry.COLUMN_ITEM_PRICE);
        if (price < 0) {
            Toast.makeText(getContext(), R.string.text_price_non_negative, Toast.LENGTH_SHORT).show();
            isThereSomethingMissingOrWrong = 1;
        }
        // Check that the description is not null
        String desc = values.getAsString(ItemEntry.COLUMN_ITEM_DESCRIPTION);
        System.out.println(desc);
        if (desc == null || desc.equals("")) {
            Toast.makeText(getContext(), R.string.text_description_required, Toast.LENGTH_SHORT).show();
            isThereSomethingMissingOrWrong = 1;
        }
        // Check that the stock is not negative
        Integer stock = values.getAsInteger(ItemEntry.COLUMN_ITEM_STOCK);
        if (stock < 0) {
            Toast.makeText(getContext(), R.string.text_stock_non_negative, Toast.LENGTH_SHORT).show();
            isThereSomethingMissingOrWrong = 1;
        }
        // Check that the image is not null
        String img = values.getAsString(ItemEntry.COLUMN_ITEM_IMAGE);
        if (img == null || img.equals("")) {
            Toast.makeText(getContext(), R.string.text_item_image_required, Toast.LENGTH_SHORT).show();
            isThereSomethingMissingOrWrong = 1;
        }


        if (values.size() == 0 || isThereSomethingMissingOrWrong == 1) {
            return 0;
        }

        // Update the selected items in the inventory database with the given ContentValues
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Return the number of rows that were affected
        int rowsUpdated = database.update(ItemEntry.TABLE_NAME, values, selection, selectionArgs);

        /**
         * If 1 or more rows were updated, notify all listeners that the data has changed for the
         * given URI
         */
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }
}
