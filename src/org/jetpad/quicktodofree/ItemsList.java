/*
 * Copyright (C) 2009 Peter Arthur
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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.jetpad.quicktodofree.QuickTodo.Todo;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the intent if there is one, otherwise defaults to displaying the
 * contents of the {@link TodoProvider}
 */
public class ItemsList extends ListActivity {
    private static final String TAG = "TodoList";
    private static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm";

    // Menu item ids
    public static final int MENU_ITEM_DELETE = Menu.FIRST;
    public static final int MENU_ITEM_INSERT = Menu.FIRST + 1;
	private static final int MENU_ITEM_SETTINGS = Menu.FIRST + 2;
	private static final int MENU_ITEM_DATA = Menu.FIRST + 3;

    /**
     * The columns we are interested in from the database
     */
    private static final String[] PROJECTION = new String[] {
            Todo._ID, // 0
            Todo.TITLE, // 1
            Todo.DUE_DATE, //2
            Todo.COMPLETED, //3
            Todo.HAS_DUE_DATE, //4
    };

    /** The index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_DUEDATE = 2;
    private static final int COLUMN_INDEX_COMPLETED = 3;
    private static final int COLUMN_INDEX_HASDATE = 4;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        // If no data was given in the intent (because we were started
        // as a MAIN activity), then use our default content provider.
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(Todo.CONTENT_URI);
        }

        // Inform the list we provide context menus for items
        getListView().setOnCreateContextMenuListener(this);
        
        // Perform a managed query. The Activity will handle closing and requerying the cursor
        // when needed.
        Cursor cursor = managedQuery(getIntent().getData(), PROJECTION, null, null,
                Todo.DEFAULT_SORT_ORDER);

        // Used to map notes entries from the database to views
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.noteslist_item, cursor,
                new String[] { Todo.TITLE,Todo.DUE_DATE,Todo.COMPLETED }, new int[] { android.R.id.text1,R.id.duedate,R.id.cb1 });
        
        final OnClickListener c = new OnClickListener() {

			public void onClick(View v) {
				int id = Integer.parseInt((String)v.getTag());
				Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
				completeTask(uri);
			}
        };
        
        adapter.setViewBinder( new SimpleCursorAdapter.ViewBinder() {
        	
                public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                        if(columnIndex == COLUMN_INDEX_COMPLETED) {
                                CheckBox cb = (CheckBox) view;
                                boolean bChecked = (cursor.getInt(COLUMN_INDEX_COMPLETED) != 0);
                                cb.setChecked(bChecked);
                                cb.setTag(String.valueOf(cursor.getInt(0)));
                                cb.setOnClickListener(c);
                                return true;
                        } else
                        if(columnIndex == COLUMN_INDEX_DUEDATE) {
                        	TextView dt = (TextView)view;
                        	dt.setTextColor(Color.WHITE);
                        	if(cursor.getInt(COLUMN_INDEX_HASDATE) != 0 && cursor.getInt(COLUMN_INDEX_COMPLETED) == 0) {
                            	Calendar c = Calendar.getInstance();
                            	c.setTimeInMillis(cursor.getLong(COLUMN_INDEX_DUEDATE));
                        		dt.setText((new SimpleDateFormat(DATE_FORMAT)).format(c.getTime()));
                        		if(c.before(Calendar.getInstance())) {
                        			dt.setTextColor(Color.RED);
                        		}
                        	} else if(cursor.getInt(COLUMN_INDEX_COMPLETED) != 0) {
                        		dt.setText("Completed");
                        	} else {
                        		dt.setText("No due date");
                        	}
                        	return true;
                        }
                        return false;
                }
        });

        setListAdapter(adapter);
    }

    private void completeTask(Uri mUri) {

        // Get a cursor to access the note
        Cursor mCursor = managedQuery(mUri, PROJECTION, null, null, null);
        if (mCursor != null) {
        	mCursor.moveToFirst();
        	int cur_completed = mCursor.getInt(COLUMN_INDEX_COMPLETED);
        
        	cur_completed = (cur_completed == 0) ? 1:0;

            // Write the title back to the item 
            ContentValues values = new ContentValues();
            values.put(Todo.COMPLETED, cur_completed);
            getContentResolver().update(mUri, values, null, null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Insert the "insert" menu into the list 
        menu.add(0, MENU_ITEM_INSERT, 0, R.string.menu_insert)
                .setShortcut('3', 'a')
                .setIcon(android.R.drawable.ic_menu_add);

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, ItemsList.class), null, intent, 0, null);
		// Insert the "Settings" menu into the list 

        menu.add(0, MENU_ITEM_SETTINGS, 0, "Settings")
                .setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, MENU_ITEM_DATA, 0, "Manage Data")
        .setIcon(android.R.drawable.ic_menu_manage);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final boolean haveItems = getListAdapter().getCount() > 0;

        if (haveItems) {
            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Build menu...  always starts with the EDIT action...
            Intent[] specifics = new Intent[1];
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);
            MenuItem[] items = new MenuItem[1];

            // ... is followed by whatever other actions are available...
            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0, null, specifics, intent, 0,
                    items);

            // Give a shortcut to the edit action.
            if (items[0] != null) {
                items[0].setShortcut('1', 'e');
            }
        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_INSERT:
            // Launch activity to insert a new item
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
		case MENU_ITEM_SETTINGS:
    		startActivity(new Intent(this,Preferences.class));  
    		return true; 
		case MENU_ITEM_DATA:
    		startActivity(new Intent(this,DataManager.class));
    		return true; 
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Setup the menu header
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Add a menu item to delete the note
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.menu_delete);
    }
        
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
            case MENU_ITEM_DELETE: {
                // Delete the note that the context menu is for
                Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);
                getContentResolver().delete(noteUri, null, null);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
        
        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            // The caller is waiting for us to return a note selected by
            // the user.  The have clicked on one, so return it now.
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {
            // Launch activity to view/edit the currently selected item
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}
