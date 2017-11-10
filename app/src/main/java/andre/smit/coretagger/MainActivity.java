package andre.smit.coretagger;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import andre.smit.coretagger.db.CoreDB;
import andre.smit.coretagger.db.CoreDBHelper;

public class MainActivity extends AppCompatActivity {

//    private Context mContext;
    public static CoreDBHelper mHelper;
    private ListView mCoreListView;
    private ArrayAdapter<String> mAdapter;
    public static final String LOG_TAG = "CoreTagger";

    public static ArrayList<String> coreList;

    private static final String TxCoreURL1 = "http://146.6.93.146:5000/addCore";
    private static final String TxCoreURL2 = "http://146.6.93.146:5000/getCores";

    static public Boolean modePref;
    static public String taggerPref;
    static public String csjPref;
    static public String officePref;
    static public String devicePref;

    private Button btnSend;
    private Button btnAdd;

    int ACCESS_FINE_LOCATION_CODE = 3310;
    int ACCESS_COARSE_LOCATION_CODE = 3410;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mHelper = new CoreDBHelper(this);
//        mContext = this;

        // Preferences
        getPrefs(this);

        mCoreListView = (ListView) findViewById(R.id.list_cores);
        btnSend = (Button) findViewById(R.id.send_button);
        btnAdd = (Button) findViewById(R.id.add_button);

        updateUI();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_about) {
            int versionCode = BuildConfig.VERSION_CODE;
            String versionName = BuildConfig.VERSION_NAME;
            AlertDialog.Builder alertDialog=new AlertDialog.Builder(this).setCancelable(false);
            alertDialog.setTitle("About");
            alertDialog.setMessage("CoreTagger\nVersion v"+versionName+"\n\nAndre Smit\nasmit@utexas.edu");
            alertDialog.setNegativeButton("OK",new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    dialog.cancel();
                }
            });
            AlertDialog alert=alertDialog.create();
            alert.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void getPrefs(Context context) {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        modePref = sharedPref.getBoolean(SettingsActivity.KEY_PREF_MODE_SWITCH, false);
        taggerPref = sharedPref.getString(SettingsActivity.KEY_PREF_TAGGER_EDIT, "Tagger");
        officePref = sharedPref.getString(SettingsActivity.KEY_PREF_OFFICE_EDIT, "Flexible Pavements");
        csjPref = sharedPref.getString(SettingsActivity.KEY_PREF_CSJ_EDIT, "000000000");
        devicePref = sharedPref.getString(SettingsActivity.KEY_PREF_DEVICE_LIST, "2");

        SharedPreferences.OnSharedPreferenceChangeListener spChanged = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("mode_switch")) {
                    // Clear internal database
                    SQLiteDatabase db = mHelper.getWritableDatabase();
                    db.delete(CoreDB.CoreEntry.TABLE, null, null);  // delete all current records
                    updateUI();
                }
            }
        };
        sharedPref.registerOnSharedPreferenceChangeListener(spChanged);
    }

    private void updateUI() {
        coreList = new ArrayList<>();
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = db.query(CoreDB.CoreEntry.TABLE,
                new String[]{CoreDB.CoreEntry._ID, CoreDB.CoreEntry.COL_CORE_NAME},
                null, null, null, null, null);
        while (cursor.moveToNext()) {
            int idx = cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_NAME);
            coreList.add(cursor.getString(idx));
        }
        if (mAdapter == null) {
            mAdapter = new ArrayAdapter<>(this,
                    R.layout.item_core,
                    R.id.core_title,
                    coreList);
            mCoreListView.setAdapter(mAdapter);
        } else {
            mAdapter.clear();
            mAdapter.addAll(coreList);
            mAdapter.notifyDataSetChanged();
        }
        cursor.close();
        db.close();
        mCoreListView.setOnItemClickListener(null);
        mCoreListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
                String coreID = coreList.get(position);
                Log.d(LOG_TAG, "onItemClick: " + coreID);
                Intent intent = new Intent(MainActivity.this, ViewActivity.class);
                intent.putExtra("CORE_ID", coreID);
                startActivity(intent);
            }
        });

        if (modePref) {
            // Lab mode
            btnSend.setText("Refresh");
            btnSend.setEnabled(true);
            btnAdd.setVisibility(View.GONE);
        } else {
            // Field mode
            btnSend.setText("Upload");
            btnAdd.setVisibility(View.VISIBLE);
            if (coreList.isEmpty()) {
                btnSend.setEnabled(false);
            } else {
                btnSend.setEnabled(true);
            }
        }
    }

    public void addCore(View view) {
        Intent intent = new Intent(this, CoreActivity.class);
        startActivity(intent);
    }

    public static Cursor getAllData(String core) {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        return db.query(CoreDB.CoreEntry.TABLE,
                new String[] {
                        CoreDB.CoreEntry.COL_CORE_NAME,
                        CoreDB.CoreEntry.COL_CORE_TAGGER,
                        CoreDB.CoreEntry.COL_CORE_OFFICE,
                        CoreDB.CoreEntry.COL_CORE_STAMP,
                        CoreDB.CoreEntry.COL_CORE_LON,
                        CoreDB.CoreEntry.COL_CORE_LAT,
                        CoreDB.CoreEntry.COL_CORE_RFID},
                CoreDB.CoreEntry.COL_CORE_NAME + " = ?",
                new String[]{core},
                null, null, null);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private class ProcessJSON extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... strings) {
            String stream = null;
            String urlString = strings[0];
            HTTPDataHandler hh = new HTTPDataHandler();
            if (!modePref) {
                String jString = strings[1];
                String core = strings[2];
                stream = hh.SendHTTPData(urlString, jString, core);
            } else {
                stream = hh.GetHTTPData(urlString);
            }
            return stream;
        }

        protected void onPostExecute(String stream) {
            if (stream != null) {
                if (!modePref) {
                    SQLiteDatabase db = mHelper.getWritableDatabase();
                    db.delete(CoreDB.CoreEntry.TABLE,
                            CoreDB.CoreEntry.COL_CORE_NAME + " = ?",
                            new String[]{stream});
                    db.close();
                    updateUI();
                } else {
                    try {
                        JSONArray array = new JSONArray(stream);
                        JSONObject obj;
                        SQLiteDatabase db = mHelper.getWritableDatabase();
                        db.delete(CoreDB.CoreEntry.TABLE, null, null);  // delete all current records
                        for (int i = 0; i < array.length(); i++) {
                            obj = array.getJSONObject(i);
                            String core = obj.getString("f1");
                            String inspector = obj.getString("f2");
                            String office = obj.getString("f3");
                            String stamp = obj.getString("f4");
                            String lon = obj.getString("f5");
                            String lat = obj.getString("f6");
                            String rfid = obj.getString("f7");

                            ContentValues values = new ContentValues();
                            values.put(CoreDB.CoreEntry.COL_CORE_NAME, core);
                            values.put(CoreDB.CoreEntry.COL_CORE_TAGGER, inspector);
                            values.put(CoreDB.CoreEntry.COL_CORE_OFFICE, office);
                            values.put(CoreDB.CoreEntry.COL_CORE_STAMP, stamp);
                            values.put(CoreDB.CoreEntry.COL_CORE_LON, lon);
                            values.put(CoreDB.CoreEntry.COL_CORE_LAT, lat);
                            values.put(CoreDB.CoreEntry.COL_CORE_RFID, rfid);
                            db.insertWithOnConflict(CoreDB.CoreEntry.TABLE,
                                    null,
                                    values,
                                    SQLiteDatabase.CONFLICT_REPLACE);
                        }
                        db.close();
                        updateUI();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Toast.makeText(MainActivity.this, "Server appears to be down - please contact admin!", Toast.LENGTH_LONG).show();
                Log.d(LOG_TAG, "FIXME: No server!");
            }
        }
    }

    // get cores on server
    public void getCores(View view) {
        if (!isOnline()) {
            Toast.makeText(this, "No internet connection!", Toast.LENGTH_LONG).show();
            return;
        }
        new ProcessJSON().execute(TxCoreURL2);
        updateUI();
    }

    public void processCores(View view) throws JSONException {

        if (modePref) {
            // Lab mode
            getCores(view);
        } else {
            sendCores(view);
        }
    }

    private void sendCores(View view) throws JSONException {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = db.query(CoreDB.CoreEntry.TABLE,
                new String[]{CoreDB.CoreEntry._ID, CoreDB.CoreEntry.COL_CORE_NAME},
                null, null, null, null, null);
        while (cursor.moveToNext()) {
            int idx = cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_NAME);
            String core = cursor.getString(idx);
            sendCore(core);
        }
        cursor.close();
        db.close();
    }

    private void sendCore(final String core) throws JSONException {

        if (!isOnline()) {
            Toast.makeText(this, "No internet connection!", Toast.LENGTH_LONG).show();
            return;
        }

        Cursor cursor = getAllData(core);
        JSONObject jobj ;
        cursor.moveToFirst();
        jobj = new JSONObject();
        jobj.put("core", cursor.getString(cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_NAME)));
        jobj.put("inspector", cursor.getString(cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_TAGGER)));
        jobj.put("office", cursor.getString(cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_OFFICE)));
        jobj.put("stamp", cursor.getString(cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_STAMP)));
        jobj.put("lon", cursor.getString(cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_LON)));
        jobj.put("lat", cursor.getString(cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_LAT)));
        jobj.put("rfid", cursor.getString(cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_RFID)));

        new ProcessJSON().execute(TxCoreURL1, jobj.toString(), core);

    }


}
