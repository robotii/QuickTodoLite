package org.jetpad.quicktodofree;

import org.jetpad.quicktodofree.QuickTodo.Todo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootHelper extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
		  context.startService(new Intent("org.jetpad.quicktodofree.UPDATE_ALARM",Todo.CONTENT_URI));
		}
	}
}
