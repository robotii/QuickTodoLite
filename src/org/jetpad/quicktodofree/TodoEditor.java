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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.jetpad.quicktodofree.QuickTodo.Todo;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

/**
 * A generic activity for editing an item in a database. This can be used either
 * to simply view an item {@link Intent#ACTION_VIEW}, view and edit an item
 * {@link Intent#ACTION_EDIT}, or create a new item {@link Intent#ACTION_INSERT}
 * .
 */
public class TodoEditor extends Activity {

	// the callback received when the user "sets" the date in the dialog
	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			cDuedate.set(Calendar.YEAR, year);
			cDuedate.set(Calendar.MONTH, monthOfYear);
			cDuedate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			duedate = cDuedate.getTimeInMillis();
			updateDisplay();
		}
	};

	private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			cDuedate.set(Calendar.HOUR_OF_DAY, hourOfDay);
			cDuedate.set(Calendar.MINUTE, minute);
			cDuedate.set(Calendar.SECOND, 0);
			cDuedate.set(Calendar.MILLISECOND, 0);
			duedate = cDuedate.getTimeInMillis();
			updateDisplay();
		}
	};

	/**
	 * Standard projection for the interesting columns of a normal todo item.
	 */
	private static final String[] PROJECTION = new String[] { Todo._ID, // 0
			Todo.NOTE, // 1
			Todo.DUE_DATE, // 2
			Todo.TITLE, // 3
			Todo.COMPLETED, // 4
			Todo.HAS_DUE_DATE, // 5
			Todo.FOLDER, // 6
			Todo.HAS_REMINDER, // 7
			Todo.NOTIFY_DATE, // 8
			Todo.IS_SCHEDULED, // 9
			Todo.SCHEDULE_DATE, // 10
			Todo.PRIORITY, // 11
			Todo.CONTEXT, // 12
	};
	/** The index of the note column */
	private static final int COLUMN_INDEX_NOTE = 1;
	private static final int COLUMN_INDEX_DATE = 2;
	private static final int COLUMN_INDEX_TITLE = 3;
	private static final int COLUMN_INDEX_CHECKED = 4;
	private static final int COLUMN_INDEX_HASDATE = 5;


	private static final int DISCARD_ID = Menu.FIRST;
	private static final int DELETE_ID = Menu.FIRST + 1;

	// Dialogs
	private static final int TIME_DIALOG_ID = 0;
	private static final int DATE_DIALOG_ID = 1;

	private Uri mUri;
	private Cursor mCursor;
	private EditText mText;
	private EditText mTitle;

	private Button mTimeButton;
	private Button mDateButton;
	private CheckBox mHasDueDate;

	private long duedate = 0;
	private long reminder = 0;
	private Calendar cDuedate = Calendar.getInstance();
	private Calendar cReminddate = Calendar.getInstance();
	private Boolean checked;
	
	/**
	 * A custom EditText that draws lines between each line of text that is
	 * displayed.
	 */
	public static class LinedEditText extends EditText {
		private Rect mRect;
		private Paint mPaint;

		// we need this constructor for LayoutInflater
		public LinedEditText(Context context, AttributeSet attrs) {
			super(context, attrs);

			mRect = new Rect();
			mPaint = new Paint();
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setColor(0x80AAAAFF);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			int count = getLineCount();
			Rect r = mRect;
			Paint paint = mPaint;

			for (int i = 0; i < count; i++) {
				int baseline = getLineBounds(i, r) + 1;

				canvas.drawLine(r.left, baseline, r.right, baseline,
						paint);
			}

			super.onDraw(canvas);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(duedate);
		switch (id) {
		case TIME_DIALOG_ID:
			return new TimePickerDialog(this, mTimeSetListener, 
					c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
		case DATE_DIALOG_ID:
			return new DatePickerDialog(this, mDateSetListener, 
					c.get(Calendar.YEAR), c.get(Calendar.MONTH), 
					c.get(Calendar.DAY_OF_MONTH));
		}
		return null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();

		final Intent intent = getIntent();
		final String action = intent.getAction();
		
		// Do some setup based on the action being performed.
		if(Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
            ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(TodoEditor.this, R.drawable.app_todo);
            Intent contents = new Intent(Intent.ACTION_INSERT, QuickTodo.Todo.CONTENT_URI);
            contents.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Intent sc = new Intent();
            
            sc.putExtra(Intent.EXTRA_SHORTCUT_INTENT, contents)
            	.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Add Todo")
            	.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

            setResult(RESULT_OK, sc);
            finish();
            return;
		}

		final String mUriType = getContentResolver().getType(intent.getData());
		if (mUriType == Todo.CONTENT_ITEM_TYPE) {

			if ("org.jetpad.quicktodo.VIEW_REMINDER".equals(action)) {
			}
			// Requested to edit
			mUri = intent.getData();
			setTitle(getText(R.string.title_edit));
		} else if (mUriType == Todo.CONTENT_TYPE) {
			// Requested to insert
			mUri = getContentResolver().insert(intent.getData(), null);
			
			intent.setData(mUri);
			setTitle(getText(R.string.title_create));

			// If we were unable to create a new note, then just finish
			if (mUri == null) {
				Toast.makeText(this, "Failed to insert new todo",
						Toast.LENGTH_LONG);
				finish();
				return;
			}

			// The new entry was created, so assume all will end well and
			// set the result to be returned.
			setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

		} else {
			Toast.makeText(this, "Unknown action, exiting", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		// Set the layout for this activity.
		setContentView(R.layout.note_editor);

		// Get the views required by this Activity
		mText = (EditText) findViewById(R.id.note);
		mTitle = (EditText) findViewById(R.id.title);
		mDateButton = (Button) findViewById(R.id.DateButton);
		mTimeButton = (Button) findViewById(R.id.TimeButton);
		mHasDueDate = (CheckBox) findViewById(R.id.HasDueDate);

		mDateButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				showDialog(DATE_DIALOG_ID);
			}
		});

		mTimeButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				showDialog(TIME_DIALOG_ID);
			}
		});

		// Get the data!
		mCursor = getContentResolver().query(mUri, PROJECTION, null, null, null);

		// If we didn't have any trouble retrieving the data, it is now
		// time to get at the stuff.
		if (mCursor != null) {
			if(mCursor.isAfterLast()) {
				Toast.makeText(this, "Could not find Todo", Toast.LENGTH_LONG).show();
				finish();
				return;
			}
			// Make sure we are at the one and only row in the cursor.
			mCursor.moveToFirst();

			// Set title and note
			String note = mCursor.getString(COLUMN_INDEX_NOTE);
			String title = mCursor.getString(COLUMN_INDEX_TITLE);
			boolean bChecked = mCursor.getInt(COLUMN_INDEX_CHECKED) != 0;
			duedate = mCursor.getLong(COLUMN_INDEX_DATE);
			boolean bHasDueDate = mCursor.getInt(COLUMN_INDEX_HASDATE) != 0;

			// Set completed status
			CheckBox completed = (CheckBox) findViewById(R.id.completed);
			completed.setChecked(bChecked);

			// Set has due date flag
			mHasDueDate.setChecked(bHasDueDate);

			// Set due date
			cDuedate = Calendar.getInstance();
			if (duedate != 0) {
				cDuedate.setTimeInMillis(duedate);
			} else {
				duedate = cDuedate.getTimeInMillis();
			}
			
			updateDisplay();

			mText.setTextKeepState(note);
			mTitle.setText(title);

		} else {
			setTitle(getText(R.string.error_title));
			mText.setText(getText(R.string.error_message));
		}

	}

	private void updateDisplay() {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(duedate);
		mTimeButton.setText(SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(c.getTime()));
		mDateButton.setText(SimpleDateFormat.getDateInstance().format(c.getTime()));
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		// Save the changes
		if (mCursor != null) {

			String text = mText.getText().toString();
			String title = mTitle.getText().toString();
			int length = title.length();

			// Delete if no text in todo
			if (isFinishing() && (length == 0)) {
				setResult(RESULT_CANCELED);
				deleteNote();

				// Get out updates into the provider.
			} else {
				// Did the user complete the todo?
				checked = ((CheckBox) findViewById(R.id.completed)).isChecked();
				
				ContentValues values = new ContentValues();

				// Bump the modification time to now.
				values.put(Todo.MODIFIED_DATE, System.currentTimeMillis());
				values.put(Todo.DUE_DATE, duedate);
				values.put(Todo.TITLE, title);
				values.put(Todo.COMPLETED, checked);
				values.put(Todo.HAS_DUE_DATE, (mHasDueDate.isChecked()) ? 1 : 0);

				// Write our text back into the provider.
				values.put(Todo.NOTE, text);

				// Commit all of our changes
				getContentResolver().update(mUri, values, null, null);

			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, DELETE_ID, 0, R.string.menu_delete).setShortcut('1', 'd')
				.setIcon(android.R.drawable.ic_menu_delete);

		Intent intent = new Intent(null, getIntent().getData());
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
				new ComponentName(this, TodoEditor.class), null, intent, 0,
				null);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		switch (item.getItemId()) {
		case DELETE_ID:
		case DISCARD_ID:
			deleteNote();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Take care of deleting a note. Simply deletes the entry.
	 */
	private final void deleteNote() {
		if (mCursor != null) {
			mCursor.close();
			getContentResolver().delete(mUri, null, null);
			mText.setText("");
			finish();
		}
	}
}
