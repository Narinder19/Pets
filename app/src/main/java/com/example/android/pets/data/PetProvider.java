package com.example.android.pets.data;

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

import com.example.android.pets.data.PetContract.PetEntry;

public class PetProvider extends ContentProvider {

    private static final String LOG_TAG = PetProvider.class.getSimpleName();
    private PetDBHelper mDbHelper;

    private static final int PETS = 100;
    private static final int PET_ID = 101;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    //Static initializer. This is run the first time anything is called from this class.
    static {
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS + "/#", PET_ID);
    }

    //Initialize the provider and the database helper object.
    @Override
    public boolean onCreate() {
        mDbHelper = new PetDBHelper(getContext());
        return true;
    }

    //Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        int match = sUriMatcher.match(uri);
        Cursor cursor;

        switch (match) {
            case PETS:
                cursor = database.query(PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case PET_ID:

/*                Log.e(LOG_TAG,"Pet_id " +  selection +" - " + selectionArgs[0] );
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                Log.e(LOG_TAG,"Pet_id " +  selection +" - " + String.valueOf(ContentUris.parseId(uri)) );*/
                cursor = database.query(PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    //Returns the MIME type of data for the content URI.
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unkonwn URI " + uri + " with match " + match);
        }
    }

    //Insert new data into the provider with the given ContentValues.
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        final int match = sUriMatcher.match(uri);

        //Notify all listeners that the data has changed for the pet content URI.
        getContext().getContentResolver().notifyChange(uri, null);
        switch (match) {
            case PETS:
                return insertPet(uri, values);
            default:
                throw new IllegalArgumentException("Insert is not supported for " + uri);
        }

    }

    private Uri insertPet(Uri uri, ContentValues values) {
        checkContentValues(values);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long newRowId = db.insert(PetEntry.TABLE_NAME, null, values);
        if (newRowId == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }
        return ContentUris.withAppendedId(uri, newRowId);

    }

    // Check if Content values are valid.
    private void checkContentValues(ContentValues values) {

        if (values.containsKey(PetEntry.COLUMN_PET_NAME)) {
            String name = values.getAsString(PetEntry.COLUMN_PET_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Pet name is required.");
            }
        }
        if (values.containsKey(PetEntry.COLUMN_PET_GENDER)) {
            Integer gender = values.getAsInteger(PetEntry.COLUMN_PET_GENDER);
            if (gender == null || !PetEntry.isValidGender(gender)) {
                throw new IllegalArgumentException("Pet gender cannot be null.");
            }
        }
        if (values.containsKey(PetEntry.COLUMN_PET_WEIGHT)) {
            Integer weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
            if (weight != null && weight < 0) {
                throw new IllegalArgumentException("Pet weight is invalid.");
            }
        }
    }

    //Delete the data at the given selection and selection arguments.
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case PETS:
                return deletePet(uri, selection, selectionArgs);
            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return deletePet(uri, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);

        }
    }

    private int deletePet(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowDeleted = database.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
        if(rowDeleted != 0){
            //Notify all listeners that the data has changed for the pet content URI.
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowDeleted;
    }

    //Updates teh data at the given selection and selection arguments, with the new ContentValues.
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case PETS:
                return updatePet(uri, values, selection, selectionArgs);
            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updatePet(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }

    }

    private int updatePet(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        checkContentValues(values);
        //Check if there are values to update.
        if (values.size() == 0) {
            return 0;
        }
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        int rowUpdated = database.update(PetEntry.TABLE_NAME, values, selection, selectionArgs);
        if (rowUpdated != 0) {
            //Notify all listeners that the data has changed for the pet content URI.
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowUpdated;
    }
}
