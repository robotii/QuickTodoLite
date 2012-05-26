/**
 * ------------------------------------------------------------
 *                       QuickTodo Lite
 * ------------------------------------------------------------
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetpad.quicktodofree;

import java.util.HashMap;

import org.jetpad.quicktodofree.QuickTodo.Todo;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Provides access to a database of notes. Each note has a title, the note
 * itself, a creation date and a modified data.
 */
public class TodoProvider extends ContentProvider {

    private static final String DATABASE_NAME = "QuickTodo.db";
    private static final int DATABASE_VERSION = 4;
    private static final String ITEMS_TABLE_NAME = "items";

    private static HashMap<String, String> sNotesProjectionMap;

    private static final int NOTES = 1;
    private static final int NOTE_ID = 2;
    private static final int RESET_CODE = 3;

    private static final UriMatcher sUriMatcher;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, /*Environment.getExternalStorageDirectory
            		().getAbsolutePath() + "/" +*/ DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	String s = 
            "CREATE TABLE " + ITEMS_TABLE_NAME + " ("
                    + Todo._ID + " INTEGER PRIMARY KEY,"
                    + Todo.TITLE + " TEXT,"
                    + Todo.NOTE + " TEXT,"
                    + Todo.CREATED_DATE + " INTEGER,"
                    + Todo.MODIFIED_DATE + " INTEGER,"
                    + Todo.DUE_DATE + " INTEGER,"
                    + Todo.COMPLETED + " INTEGER,"
                    + Todo.FOLDER + " INTEGER,"
                    + Todo.NOTIFY_DATE + " INTEGER,"
                    + Todo.CONTEXT + " INTEGER,"
                    + Todo.ICON + " INTEGER,"
            		+ Todo.HAS_DUE_DATE + " INTEGER,"
            		+ Todo.HAS_REMINDER + " INTEGER,"
            		+ Todo.IS_SCHEDULED + " INTEGER,"
            		+ Todo.PRIORITY + " INTEGER,"
            		+ Todo.SCHEDULE_DATE + " INTEGER,"
            		+ Todo.INBOX + " INTEGER"
                    + ");";
                    db.execSQL(s);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
            //        + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS items;");
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
        case NOTES:
            qb.setTables(ITEMS_TABLE_NAME);
            qb.setProjectionMap(sNotesProjectionMap);
            break;

        case NOTE_ID:
            qb.setTables(ITEMS_TABLE_NAME);
            qb.setProjectionMap(sNotesProjectionMap);
            qb.appendWhere(Todo._ID + "=" + uri.getPathSegments().get(1));
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = QuickTodo.Todo.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case NOTES:
            return Todo.CONTENT_TYPE;

        case NOTE_ID:
            return Todo.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
    	
        if (sUriMatcher.match(uri) == RESET_CODE) {
            mOpenHelper.close();
            mOpenHelper.getWritableDatabase();
            return uri;
        }
        // Validate the requested uri
        if (sUriMatcher.match(uri) != NOTES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        Long now = Long.valueOf(System.currentTimeMillis());

        // Make sure that the fields are all set
        if (values.containsKey(QuickTodo.Todo.CREATED_DATE) == false) {
            values.put(QuickTodo.Todo.CREATED_DATE, now);
        }

        if (values.containsKey(QuickTodo.Todo.MODIFIED_DATE) == false) {
            values.put(QuickTodo.Todo.MODIFIED_DATE, now);
        }
        
        if (values.containsKey(QuickTodo.Todo.DUE_DATE) == false) {
            values.put(QuickTodo.Todo.DUE_DATE, now+3600000);
        }

        if (values.containsKey(QuickTodo.Todo.TITLE) == false) {
            values.put(QuickTodo.Todo.TITLE, "");
        }

        if (values.containsKey(QuickTodo.Todo.NOTE) == false) {
            values.put(QuickTodo.Todo.NOTE, "");
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(ITEMS_TABLE_NAME, Todo.NOTE, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(QuickTodo.Todo.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        
        getContext().startService(new Intent("org.jetpad.quicktodofree.DELETE_ALARM",uri));
        
        switch (sUriMatcher.match(uri)) {
        case NOTES:
            count = db.delete(ITEMS_TABLE_NAME, where, whereArgs);
            break;

        case NOTE_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.delete(ITEMS_TABLE_NAME, Todo._ID + "=" + noteId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case NOTES:
            count = db.update(ITEMS_TABLE_NAME, values, where, whereArgs);
            break;

        case NOTE_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.update(ITEMS_TABLE_NAME, values, Todo._ID + "=" + noteId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        getContext().startService(new Intent("org.jetpad.quicktodofree.UPDATE_ALARM",uri));
        return count;
    }
    
    public void resetDatabase() {
        mOpenHelper.close();
        mOpenHelper.getReadableDatabase();
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(QuickTodo.AUTHORITY, "todos", NOTES);
        sUriMatcher.addURI(QuickTodo.AUTHORITY, "todos/#", NOTE_ID);
        sUriMatcher.addURI(QuickTodo.AUTHORITY, "reset", RESET_CODE);

        sNotesProjectionMap = new HashMap<String, String>();
        sNotesProjectionMap.put(Todo._ID, Todo._ID);
        sNotesProjectionMap.put(Todo.TITLE, Todo.TITLE);
        sNotesProjectionMap.put(Todo.NOTE, Todo.NOTE);
        sNotesProjectionMap.put(Todo.CREATED_DATE, Todo.CREATED_DATE);
        sNotesProjectionMap.put(Todo.MODIFIED_DATE, Todo.MODIFIED_DATE);
        sNotesProjectionMap.put(Todo.DUE_DATE, Todo.DUE_DATE);
        sNotesProjectionMap.put(Todo.COMPLETED, Todo.COMPLETED);
        sNotesProjectionMap.put(Todo.FOLDER, Todo.FOLDER);
        sNotesProjectionMap.put(Todo.NOTIFY_DATE, Todo.NOTIFY_DATE);
        sNotesProjectionMap.put(Todo.ICON, Todo.ICON);
        sNotesProjectionMap.put(Todo.CONTEXT, Todo.CONTEXT);
        sNotesProjectionMap.put(Todo.HAS_DUE_DATE, Todo.HAS_DUE_DATE);
        sNotesProjectionMap.put(Todo.HAS_REMINDER, Todo.HAS_REMINDER);
        sNotesProjectionMap.put(Todo.IS_SCHEDULED, Todo.IS_SCHEDULED);
        sNotesProjectionMap.put(Todo.PRIORITY,Todo.PRIORITY);
        sNotesProjectionMap.put(Todo.SCHEDULE_DATE,Todo.SCHEDULE_DATE);
        sNotesProjectionMap.put(Todo.INBOX,Todo.INBOX);
    }
}
