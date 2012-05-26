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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;


public class DataManager extends Activity {

   private Application application;

   private Button exportDbToSdButton;
   private Button importDbFromSdButton;
   private Button clearDbButton;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      application = (Application) getApplication();

      setContentView(R.layout.datamanager);

      exportDbToSdButton = (Button) findViewById(R.id.exportdbtosdbutton);
      exportDbToSdButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {

            new AlertDialog.Builder(DataManager.this).setMessage(
                     "Are you sure (this will overwrite any existing backup data)?").setPositiveButton("Yes",
                     new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                           if (isExternalStorageAvail()) {

                              new ExportDatabaseTask().execute();
                              //ManageData.this.startActivity(new Intent(ManageData.this, Main.class));
                           } else {
                              Toast.makeText(DataManager.this,
                                       "External storage is not available, unable to export data.", Toast.LENGTH_SHORT)
                                       .show();
                           }
                        }
                     }).setNegativeButton("No", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface arg0, int arg1) {
               }
            }).show();
         }
      });

      importDbFromSdButton = (Button) findViewById(R.id.importdbfromsdbutton);
      importDbFromSdButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new AlertDialog.Builder(DataManager.this).setMessage(
                     "Are you sure (this will overwrite existing current data)?").setPositiveButton("Yes",
                     new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                           if (isExternalStorageAvail()) {
                         
                              new ImportDatabaseTask().execute();
                              // sleep momentarily so that database reset stuff has time to take place (else Main reloads too fast)
                              SystemClock.sleep(500);
                         
                           } else {
                              Toast.makeText(DataManager.this,
                                       "External storage is not available, unable to export data.", Toast.LENGTH_SHORT)
                                       .show();
                           }
                        }
                     }).setNegativeButton("No", new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface arg0, int arg1) {
               }
            }).show();
         }
      });

      clearDbButton = (Button) findViewById(R.id.cleardbutton);
      clearDbButton.setOnClickListener(new OnClickListener() {
         public void onClick(final View v) {
            new AlertDialog.Builder(DataManager.this).setMessage("Are you sure (this will delete all data)?")
                     .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                           
                           //DataManager.this.application.getDataHelper().deleteAllDataYesIAmSure();
                           //DataManager.this.application.getDataHelper().resetDbConnection();
                           Toast.makeText(DataManager.this, "Data deleted", Toast.LENGTH_SHORT).show();
                           
                        }
                     }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                        }
                     }).show();
         }
      });
   }

   private boolean isExternalStorageAvail() {
      return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
   }

   private class ExportDatabaseTask extends AsyncTask<Void, Void, Boolean> {
      private final ProgressDialog dialog = new ProgressDialog(DataManager.this);

      // can use UI thread here
      @Override
      protected void onPreExecute() {
         dialog.setMessage("Exporting database...");
         dialog.show();
      }

      // automatically done on worker thread (separate from UI thread)
      @Override
      protected Boolean doInBackground(final Void... args) {

         File dbFile = new File(Environment.getDataDirectory() + "/data/org.jetpad.quicktodofree/databases/QuickTodo.db");

         File exportDir = new File(Environment.getExternalStorageDirectory(), "QuickTodo");
         if (!exportDir.exists()) {
            exportDir.mkdirs();
         }
         File file = new File(exportDir, dbFile.getName());

         try {
            file.createNewFile();
            FileUtil.copyFile(dbFile, file);
            return true;
         } catch (IOException e) {
            
            return false;
         }
      }

      // can use UI thread here
      @Override
      protected void onPostExecute(final Boolean success) {
         if (dialog.isShowing()) {
            dialog.dismiss();
         }
         if (success) {
            Toast.makeText(DataManager.this, "Export successful!", Toast.LENGTH_SHORT).show();
         } else {
            Toast.makeText(DataManager.this, "Export failed", Toast.LENGTH_SHORT).show();
         }
      }
   }

   private class ImportDatabaseTask extends AsyncTask<Void, Void, String> {
      private final ProgressDialog dialog = new ProgressDialog(DataManager.this);

      @Override
      protected void onPreExecute() {
         dialog.setMessage("Importing database...");
         dialog.show();
      }

      // could pass the params used here in AsyncTask<String, Void, String> - but not being re-used
      @Override
      protected String doInBackground(final Void... args) {

         File dbBackupFile = new File(Environment.getExternalStorageDirectory() + "/QuickTodo/QuickTodo.db");
         if (!dbBackupFile.exists()) {
            return "Database backup file does not exist, cannot import.";
         } else if (!dbBackupFile.canRead()) {
            return "Database backup file exists, but is not readable, cannot import.";
         }

         File dbFile = new File(Environment.getDataDirectory() + "/data/org.jetpad.quicktodofree/databases/QuickTodo.db");
         if (dbFile.exists()) {
            dbFile.delete();
         }

         try {
            dbFile.createNewFile();
            FileUtil.copyFile(dbBackupFile, dbFile);
            getContentResolver().insert(Uri.parse("content://" + QuickTodo.AUTHORITY + "/reset"), new ContentValues());
            //DataManager.this.application.getDataHelper().resetDbConnection();
            return null;
         } catch (IOException e) {
            
            return e.getMessage();
         }
      }


      @Override
      protected void onPostExecute(final String errMsg) {
         if (dialog.isShowing()) {
            dialog.dismiss();
         }
         if (errMsg == null) {
            Toast.makeText(DataManager.this, "Import successful!", Toast.LENGTH_SHORT).show();
         } else {
            Toast.makeText(DataManager.this, "Import failed - " + errMsg, Toast.LENGTH_SHORT).show();
         }
      }
   }
}

final class FileUtil {

	   private FileUtil() {
	   }

	   public static void copyFile(File src, File dst) throws IOException {
	      FileChannel inChannel = new FileInputStream(src).getChannel();
	      FileChannel outChannel = new FileOutputStream(dst).getChannel();
	      try {
	         inChannel.transferTo(0, inChannel.size(), outChannel);
	      } finally {
	         if (inChannel != null) {
	            inChannel.close();
	         }
	         if (outChannel != null) {
	            outChannel.close();
	         }
	      }
	   }
	}
