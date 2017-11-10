package andre.smit.coretagger.db;

import android.provider.BaseColumns;

/**
 * Created by grassy on 10/17/17.
 */

public class CoreDB {
    public static final String DB_NAME = "andre.smit.coretagger.db";
    public static final int DB_VERSION = 2;
    public class CoreEntry implements BaseColumns {
        public static final String TABLE = "cores";
        public static final String COL_CORE_NAME = "name";
        public static final String COL_CORE_TAGGER = "tagger";
        public static final String COL_CORE_OFFICE = "office";
        public static final String COL_CORE_STAMP = "stamp";
        public static final String COL_CORE_LON = "lon";
        public static final String COL_CORE_LAT = "lat";
        public static final String COL_CORE_RFID = "rfid";
    }

}
