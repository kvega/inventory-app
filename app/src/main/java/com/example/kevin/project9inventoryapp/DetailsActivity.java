package com.example.kevin.project9inventoryapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kevin.project9inventoryapp.data.ItemContract.ItemEntry;

public class DetailsActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>{

    // Identifier for the item data loader
    private static final int EXISTING_ITEM_LOADER = 0;

    // Flag to check whether user has made changes to an existing item
    private boolean mItemHasChanged = false;

    // Listener for checking whether changes have been made
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mItemHasChanged = true;
            return false;
        }
    };

    // EditText field to enter item name
    private EditText mNameEditText;

    // EditText field to enter item description
    private EditText mDescEditText;

    // EditText field to enter item price
    private EditText mPriceEditText;

    // TextView which displays stock of current item
    private TextView mStockTextView;

    // ImageView which displays image of current item
    private ImageView mItemImageView;

    // Temporary value for the stock
    private int mStock = 0;

    // Current item URI
    private Uri mCurrentItemUri;

    // Current item image uri string
    private String itemImageUriString;

    static final int REQUEST_IMAGE_GET = 1;

    private int STORAGE_PERMISSION_CODE = 23;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.MANAGE_DOCUMENTS},STORAGE_PERMISSION_CODE);

        // Get the associated URI
        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_item_name);
        mPriceEditText = (EditText) findViewById(R.id.edit_item_price);
        mDescEditText = (EditText) findViewById(R.id.edit_item_description);
        mStockTextView = (TextView) findViewById(R.id.text_item_stock);
        mItemImageView = (ImageView) findViewById(R.id.item_image);

        ImageButton addStock = (ImageButton) findViewById(R.id.action_add_stock);
        ImageButton subStock = (ImageButton) findViewById(R.id.action_sub_stock);

        Button orderStock = (Button) findViewById(R.id.order_button);

        /**
         * Set title for DetailsActivity based on which situation we have
         * If opened using ListView item, user will be shown details and have options
         * to edit the current item.
         * Otherwise, if this is a new item, uri is null and so user will have option to
         * add a new item.
         */
        if (mCurrentItemUri == null) {
            // New item, set title to say "Add new Item"
            setTitle("Add new Item");
            mStockTextView.setText(String.valueOf(mStock));

            orderStock.setVisibility(View.INVISIBLE);
        } else {
            setTitle("Item Details");
            getLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);

            orderStock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    orderItem();
                }
            });
        }

        addStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increment();
            }
        });

        subStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decrement();
            }
        });

        mItemImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageSetDialog();
            }
        });

        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mDescEditText.setOnTouchListener(mTouchListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options
        // Add menu items to the app bar
        getMenuInflater().inflate(R.menu.menu_details, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If this is a new item, hide the "Delete" menu item
        if (mCurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save item to the database
                saveItem();
                // Exit activity
                // finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Ask the user to confirm deleting the item
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link MainActivity}.
                if (!mItemHasChanged) {
                    // Navigate back to parent activity (MainActivity)
                    NavUtils.navigateUpFromSameTask(DetailsActivity.this);
                    return true;
                }
                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(DetailsActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If the item hasn't changed, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show the diaglog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Define a projection that specifies the columns from the table to pull info from
        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_ITEM_NAME,
                ItemEntry.COLUMN_ITEM_PRICE,
                ItemEntry.COLUMN_ITEM_STOCK,
                ItemEntry.COLUMN_ITEM_DESCRIPTION,
                ItemEntry.COLUMN_ITEM_IMAGE
        };

        // Return the CursorLoader
        return new CursorLoader(
                this,
                mCurrentItemUri,
                projection,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        /**
         * Proceed with moving to the first row of the cursor and reading data from it
         * (This should be the only row in the cursor)
         */
        if (cursor.moveToFirst()) {
            // Find the columns of the item attributes of interest
            int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_PRICE);
            int descColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_DESCRIPTION);
            int stockColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_STOCK);
            int imageColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_IMAGE);

            // Extract out the values from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            float price = cursor.getFloat(priceColumnIndex);
            String desc = cursor.getString(descColumnIndex);
            mStock = cursor.getInt(stockColumnIndex);
            String image = cursor.getString(imageColumnIndex);

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mPriceEditText.setText(String.valueOf(price));
            mDescEditText.setText(desc);
            mStockTextView.setText(String.valueOf(mStock));

            Uri imageUri = Uri.parse(image);
            itemImageUriString = imageUri.toString();
            imageUri = Uri.parse(itemImageUriString);
            mItemImageView.setImageURI(imageUri);
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mDescEditText.setText("");
        mStockTextView.setText("");
        mItemImageView.setImageResource(R.drawable.ic_action_add_photo);
    }

    private void displayQuantity(int stock) {
        TextView quantityTextView = (TextView) findViewById(R.id.text_item_stock);
        quantityTextView.setText(String.valueOf(stock));
    }

    private void increment() {
        mStock = mStock + 1;
        displayQuantity(mStock);
    }

    private void decrement() {
        if (mStock == 0) {
            return;
        }
        mStock = mStock - 1;
        displayQuantity(mStock);
    }

    private void saveItem() {
        String nameString = mNameEditText.getText().toString().trim();
        String descString = mDescEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String stockString = mStockTextView.getText().toString().trim();

        /**
         * Check if this is supposed to be a new item
         * and check if all the fields in the editor are blank
         */
        if (mCurrentItemUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(descString) &&
                TextUtils.isEmpty(priceString) && mStock == ItemEntry.OUT_OF_STOCK &&
                TextUtils.isEmpty(itemImageUriString)) {
            /**
             * Since no fields were modified, we can return early without creating a new item.
             * No need to create ContentValues and no need to do any ContentProvider operations.
             */
            return;
        }
        /**
         * Create a ContentValues object where column names are the keys,
         * and pet attributes from the editor are values.
         */
        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_ITEM_NAME, nameString);
        values.put(ItemEntry.COLUMN_ITEM_DESCRIPTION, descString);
        float price = 0;
        if (!TextUtils.isEmpty(priceString)) {
            price = Float.parseFloat(priceString);
        }
        values.put(ItemEntry.COLUMN_ITEM_PRICE, price);
        int stock = ItemEntry.OUT_OF_STOCK;
        if (!TextUtils.isEmpty(stockString)) {
            stock = Integer.parseInt(stockString);
        }
        values.put(ItemEntry.COLUMN_ITEM_STOCK, stock);
        values.put(ItemEntry.COLUMN_ITEM_IMAGE, itemImageUriString);

        if (mCurrentItemUri == null) {
            Uri newUri = getContentResolver().insert(ItemEntry.CONTENT_URI, values);

            if (newUri == null) {
                Toast.makeText(this, getString(R.string.details_save_item_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.details_save_item_successful), Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            int updatedUri = getContentResolver().update(mCurrentItemUri, values, null, null);

            if (updatedUri != 0) {
                Toast.makeText(this, getString(R.string.details_update_item_successful), Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, getString(R.string.details_update_item_failed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteItem() {
        // Only perform the delete if this is an existing pet
        if (mCurrentItemUri != null) {
            // Call the ContentResolver to delete the item at the given content URI.
            // Pass in null for the selection and selection args because the MCurrentItemUri
            // content URI already identifies the item that we want.
            int deleteRows = getContentResolver().delete(mCurrentItemUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (deleteRows == 0) {
                // If no rows were deleted, then there was an error with the delete
                Toast.makeText(this, getString(R.string.details_delete_item_failed), Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.details_delete_item_successful), Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog);
        builder.setPositiveButton(R.string.discard_changes, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked the "Delete" button, so delete the item.
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User clicked the "Cancel: button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void orderItem() {
        // Get item information
        String nameString = mNameEditText.getText().toString().trim();
        String descString = mDescEditText.getText().toString().trim();
        String stockString = mStockTextView.getText().toString().trim();

        String order = createOrderSummary(nameString, descString, stockString);

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.additional_stock_request) + " " + nameString);
        intent.putExtra(Intent.EXTRA_TEXT, order);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }

    }

    private String createOrderSummary(String name, String description, String stock) {
        return getString(R.string.order_summary_item_name) + " " + name + "\n" +
                getString(R.string.order_summary_item_description) + " " + description + "\n" +
                getString(R.string.order_summary_item_quantity_requested) + " " + stock + "\n";
    }

    private void showImageSetDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.add_image);
        builder.setPositiveButton(R.string.choose_image, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectImage();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // Create intent to get a photo from a gallery
    public void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_GET);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_GET && resultCode == Activity.RESULT_OK) {
            Uri imageUri = data.getData();
            this.grantUriPermission(this.getPackageName(), imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            itemImageUriString = imageUri.toString();
            mItemImageView.setImageURI(imageUri);
        }
    }
}
