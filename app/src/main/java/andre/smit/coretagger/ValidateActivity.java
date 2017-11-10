package andre.smit.coretagger;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.trimble.mcs.rfid.v1.RfidConstants;
import com.trimble.mcs.rfid.v1.RfidException;
import com.trimble.mcs.rfid.v1.RfidManager;
import com.trimble.mcs.rfid.v1.RfidParameters;
import com.trimble.mcs.rfid.v1.RfidStatusCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import andre.smit.coretagger.db.CoreDB;
import andre.smit.coretagger.db.CoreDBHelper;

public class ValidateActivity extends AppCompatActivity {

    private static final String LOG_TAG = "CoreTagger";
    private static final String TxCoreURL3 = "http://146.6.93.146:5000/updateCore";

//    private CoreDBHelper mHelper;

    private TextView lblCoreID;
    private TextView lblTagger;
    private TextView lblOffice;
    private TextView lblDateTime;
//    private TextView lblLon;
//    private TextView lblLat;

    private Button btnInfo;
    private Button btnValidate;

    private EditText txtTag;

    private BroadcastReceiver mRecvr;
    private IntentFilter mFilter;

    public String rfid;
    public String coreID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_validate);

        coreID = getIntent().getStringExtra("CORE_ID");

        btnInfo = (Button) findViewById(R.id.btnInfo);
        btnValidate = (Button) findViewById(R.id.btnValidate);

        lblCoreID = (TextView) findViewById(R.id.lblCoreID);
        lblTagger = (TextView) findViewById(R.id.lblTagger);
        lblOffice = (TextView) findViewById(R.id.lblOffice);
        lblDateTime = (TextView) findViewById(R.id.lblDateTime);

        txtTag = (EditText) findViewById(R.id.txtTag);

        Cursor cursor = MainActivity.getAllData(coreID);
        cursor.moveToFirst();
        rfid = cursor.getString(cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_RFID));

        lblCoreID.setText(coreID);

        txtTag.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    updateTextViews();
                }
            }
        });

        if (MainActivity.devicePref.equals("1")) {

            btnInfo.setText("Scan");
            txtTag.setEnabled(false);

            mRecvr = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    onScanComplete(intent);
                }
            };

            mFilter = new IntentFilter();
            mFilter.addAction(RfidConstants.ACTION_RFID_TAG_SCANNED);
            mFilter.addAction(RfidConstants.ACTION_RFID_STOP_SCAN_NOTIFICATION);

            RfidStatusCallback cb = new RfidStatusCallback() {
                @Override
                public void onAPIReady() {
                    onRfidReady();
                }
            };

            try {
                RfidManager.init(this, RfidConstants.SESSION_SCOPE_PRIVATE, cb);
            } catch (RfidException e) {
                Log.e(LOG_TAG, "Error initializing RFID Manager.", e);
            }

        } else {
            // FIXME: do something here
        }

    }

    public void getInfo(View view) {
        updateTextViews();
        if (MainActivity.devicePref.equals("1")) {
            try {
                RfidManager.startScan();
            } catch (RfidException e) {
                Log.e(MainActivity.LOG_TAG, "Error attempting to start/stop scan.", e);
            }
        }
    }

    private void updateTextViews() {
        Date now = new Date();
        String datetime = "MM-dd-yyyy kk:mm:ss";
        lblTagger.setText(MainActivity.taggerPref);
        lblOffice.setText(MainActivity.officePref);
        lblDateTime.setText(DateFormat.format(datetime, now).toString());
    }

    private void validateRFID() {
        String chk = txtTag.getText().toString();
        Log.d(LOG_TAG, "Tag: " + rfid);
        if (chk.equals(rfid)) {
            Log.d(LOG_TAG, "TRUE Matches");
            Match(true);
        } else {
            Log.d(LOG_TAG, "FALSE Matches");
            Match(false);
        }
    }

    private void Match(final Boolean valid) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this).setCancelable(false);
        alertDialog.setTitle(coreID);
        if (valid) {
            alertDialog.setMessage("Good match!\nFinish processing this core?");
        } else {
            alertDialog.setMessage("Bad match!\nFlag this core?");
        }
        alertDialog.setNegativeButton("No",new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which){
                dialog.cancel();
            }
        });
        alertDialog.setPositiveButton("Yes",new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which){
                try {
                    updateCore(valid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        AlertDialog alert=alertDialog.create();
        alert.show();
    }

    public void validateCore(View view) {
        validateRFID();
    }


    public void endThis(View view) {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (MainActivity.devicePref.equals("1")) {
            try {
                RfidManager.deinit();
            } catch (RfidException ignored) {
                // Do something
            }
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
//        isLocationEnabled();
        if (MainActivity.devicePref.equals("1")) {
            registerReceiver(mRecvr, mFilter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (MainActivity.devicePref.equals("1")) {
            unregisterReceiver(mRecvr);
            try {
                RfidManager.stopScan();
            } catch (RfidException ignored) {
                // Do something
            }
        }
    }

    private void onRfidReady() {
        try {
            RfidParameters parms = RfidManager.getParameters();
            parms.setOutputMode(RfidConstants.OUTPUT_MODE_INTENT);
            RfidManager.setParameters(parms);
        } catch (RfidException e) {
            Log.e(LOG_TAG, "Error setting RFID parameters.", e);
        }
    }

    private void onScanComplete(Intent intent) {
        String act = intent.getAction();
        if (act.equals(RfidConstants.ACTION_RFID_TAG_SCANNED)) {
            String tagId = intent.getStringExtra(RfidConstants.RFID_FIELD_ID);
            txtTag.setText(tagId);
        } else if (act.equals(RfidConstants.ACTION_RFID_STOP_SCAN_NOTIFICATION)) {
            Log.d(LOG_TAG, "Scanning stopped");
        }
        validateRFID();
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private void updateCore(final Boolean valid) throws JSONException {

        if (!isOnline()) {
            Toast.makeText(this, "No internet connection!", Toast.LENGTH_LONG).show();
            return;
        }

        String tagger = String.valueOf(lblTagger.getText());
        String office = String.valueOf(lblOffice.getText());
        String stamp = String.valueOf(lblDateTime.getText());

        JSONObject jobj ;
        jobj = new JSONObject();
        jobj.put("core", coreID);
        jobj.put("tagger", tagger);
        jobj.put("lab", office);
        jobj.put("stamp", stamp);
        if (valid) {
            jobj.put("valid", "True");
        } else {
            jobj.put("valid", "False");
        }
        new ProcessJSON().execute(TxCoreURL3, jobj.toString(), coreID);
    }

    private class ProcessJSON extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... strings) {
            String stream = null;
            String urlString = strings[0];
            String jString = strings[1];
            String core = strings[2];
            HTTPDataHandler hh = new HTTPDataHandler();
            stream = hh.SendHTTPData(urlString, jString, core);
            return stream;
        }

        protected void onPostExecute(String stream) {
            if (stream != null) {
                SQLiteDatabase db = MainActivity.mHelper.getWritableDatabase();
                db.delete(CoreDB.CoreEntry.TABLE,
                        CoreDB.CoreEntry.COL_CORE_NAME + " = ?",
                        new String[]{stream});
                db.close();
                finish();
            } else {
                Toast.makeText(ValidateActivity.this, "Server appears to be down - please contact admin!", Toast.LENGTH_LONG).show();
                Log.d(LOG_TAG, "FIXME: No server!");
            }
        }
    }

}
