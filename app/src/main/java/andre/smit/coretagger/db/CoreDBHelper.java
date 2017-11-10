package andre.smit.coretagger.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by grassy on 10/17/17.
 */

public class CoreDBHelper extends SQLiteOpenHelper {

    public CoreDBHelper(Context context) {
        super(context, CoreDB.DB_NAME, null, CoreDB.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + CoreDB.CoreEntry.TABLE + " ( " +
                CoreDB.CoreEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                CoreDB.CoreEntry.COL_CORE_NAME + " TEXT NOT NULL, " +
                CoreDB.CoreEntry.COL_CORE_TAGGER + " TEXT NOT NULL, " +
                CoreDB.CoreEntry.COL_CORE_OFFICE + " TEXT NOT NULL, " +
                CoreDB.CoreEntry.COL_CORE_STAMP + " TEXT NOT NULL, " +
                CoreDB.CoreEntry.COL_CORE_LON + " TEXT NOT NULL, " +
                CoreDB.CoreEntry.COL_CORE_LAT + " TEXT NOT NULL, " +
                CoreDB.CoreEntry.COL_CORE_RFID + " TEXT NOT NULL);";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + CoreDB.CoreEntry.TABLE);
        onCreate(db);
    }

}
