package org.jetpad.quicktodofree;

import org.jetpad.quicktodofree.QuickTodo.Todo;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;

public class AlarmService extends Service {
    private static final String[] PROJECTION = new String[] {
        Todo._ID, // 0
        Todo.TITLE, // 1
        Todo.DUE_DATE, //2
        Todo.COMPLETED, //3
        Todo.NOTIFY_DATE, //4
        Todo.HAS_DUE_DATE //5
    };
    
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onStart(Intent i, int startId) {
		super.onStart(i, startId);
		if(i.getAction().equals("org.jetpad.quicktodofree.UPDATE_ALARM")) {
			scheduleAlarms(i.getData(),false);
		}
		else if (i.getAction().equals("org.jetpad.quicktodofree.DELETE_ALARM")) {
			scheduleAlarms(i.getData(),true);
		}
		stopSelf();
	  }

	public void scheduleAlarms(Uri mUri,boolean cancel) {
		Cursor cursor = getContentResolver().query(mUri, PROJECTION, null, null,
                Todo.DEFAULT_SORT_ORDER);
		cursor.moveToFirst();
		while(!cursor.isAfterLast()) {
			String title = cursor.getString(1);
			Long duedate = cursor.getLong(2);
			int completed = cursor.getInt(3);
			int hasduedate =cursor.getInt(5);
			Uri mUriLocal;
			if(getContentResolver().getType(mUri).equals(Todo.CONTENT_ITEM_TYPE)) {
				mUriLocal = mUri;
			} else {
				mUriLocal = ContentUris.withAppendedId(mUri, cursor.getInt(0));
			}
			//Long reminder = cursor.getLong(4);
			updateDueAlarm(mUriLocal,title,"Due Now!",duedate,true);
			if(duedate != 0 && hasduedate != 0 && !cancel && completed == 0) {
				updateDueAlarm(mUriLocal,title,"Due Now!",duedate,false);
			}
			cursor.moveToNext();
		}
	}
    
	private void updateDueAlarm(Uri mUri,String title, String notifyText, long millis,boolean cancel) {
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent alarm = new Intent(this,TodoAlarm.class);
        alarm.setData(mUri);
        alarm.setAction(Intent.ACTION_RUN);
        alarm.putExtra("title", title);
        alarm.putExtra("notifyText", notifyText);
        alarm.putExtra("duedate", millis);
        PendingIntent p = PendingIntent.getActivity(this, 0, alarm, 0);
        if(cancel) {
        	am.cancel(p);
        } else {
        	am.set(AlarmManager.RTC_WAKEUP,millis , p);
        }
        NotificationManager nman = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nman.cancel(mUri.hashCode());
    }

}
