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

import org.jetpad.quicktodo.R;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class TodoAlarm extends Activity {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final Intent intent = getIntent();
        final String title = intent.getStringExtra("title");
        final String notifyText = intent.getStringExtra("notifyText");
        final long duedate = intent.getLongExtra("duedate", System.currentTimeMillis());
        Notification notification = new Notification(R.drawable.app_todo, title,
            duedate);
        Intent i = new Intent("org.jetpad.quicktodofree.VIEW_REMINDER",intent.getData());
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, 0);
        notification.setLatestEventInfo(this, title, notifyText, contentIntent);
        NotificationManager nman = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notification.flags |= Notification.FLAG_AUTO_CANCEL|Notification.FLAG_SHOW_LIGHTS;
		notification.ledARGB = (Build.DEVICE.equals("dream")) ? 0xff080800 : 0xfffb2a0c;;
		notification.ledOffMS = 1000;
		notification.ledOnMS = 1000;
		
    	SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		if(mPrefs.getBoolean("notifySound", false)) notification.defaults |= Notification.DEFAULT_SOUND;
		if(mPrefs.getBoolean("notifyVibrate",false)) notification.defaults |= Notification.DEFAULT_VIBRATE;
		
        nman.notify(intent.getData().hashCode(), notification);
        this.finish();
    }

}
