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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Convenience definitions for TodoProvider
 */
public final class QuickTodo {
    public static final String AUTHORITY = "org.jetpad.provider.QuickTodoFree";

    // This class cannot be instantiated
    private QuickTodo() {}
    
    /**
     * Todo table
     */
    public static final class Todo implements BaseColumns {
        // This class cannot be instantiated
        private Todo() {}

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/todos");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.jetpad.todof";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.jetpad.todof";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "completed,hasduedate DESC,duedate";

        /**
         * The title of the note
         * <P>Type: TEXT</P>
         */
        public static final String TITLE = "title";

        /**
         * The note itself
         * <P>Type: TEXT</P>
         */
        public static final String NOTE = "note";

        /**
         * The timestamp for when the note was created
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the note was last modified
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String MODIFIED_DATE = "modified";
        public static final String DUE_DATE = "duedate";
		public static final String COMPLETED = "completed";
		public static final String FOLDER = "folder";
		public static final String NOTIFY_DATE = "notify_date";
		public static final String ICON = "icon";
		public static final String CONTEXT = "context";

		public static final String HAS_DUE_DATE = "hasduedate";
		public static final String HAS_REMINDER = "hasreminder";
		public static final String IS_SCHEDULED = "isscheduled";
		public static final String PRIORITY = "priority";
		public static final String SCHEDULE_DATE = "scheduledate";
		public static final String INBOX = "inbox";
		
    }
}
