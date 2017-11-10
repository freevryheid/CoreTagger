package andre.smit.coretagger;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.support.v4.app.ActivityCompat;
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

import java.util.Date;

import andre.smit.coretagger.db.CoreDB;
import andre.smit.coretagger.db.CoreDBHelper;

public class CoreActivity extends AppCompatActivity {

    private LocationManager locationManager;
    private double lon;
    private double lat;

    private TextView lblCSJ;
    private TextView lblTagger;
    private TextView lblOffice;
    private TextView lblDateTime;
    private TextView lblLon;
    private TextView lblLat;

    private EditText txtTag;
    private EditText txtLot;
    private EditText txtSubLot;
    private EditText txtCore;

    private Button btnInfo;

    private BroadcastReceiver mRecvr;
    private IntentFilter mFilter;

    // NFC
    private NfcAdapter nfcAdpt;
    private PendingIntent pendingIntent;
    private IntentFilter intentFiltersArray[];
    private String[][] techListsArray;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_core);
//        mContext = this;

        lblCSJ = (TextView) findViewById(R.id.lblCSJ);
        lblTagger = (TextView) findViewById(R.id.lblTagger);
        lblOffice = (TextView) findViewById(R.id.lblOffice);
        lblDateTime = (TextView) findViewById(R.id.lblDateTime);
        lblLon = (TextView) findViewById(R.id.lblLon);
        lblLat = (TextView) findViewById(R.id.lblLat);

        txtTag = (EditText) findViewById(R.id.txtTag);
        txtLot = (EditText) findViewById(R.id.txtLot);
        txtSubLot = (EditText) findViewById(R.id.txtSubLot);
        txtCore = (EditText) findViewById(R.id.txtCore);

        btnInfo = (Button) findViewById(R.id.btnInfo);


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
                Log.e(MainActivity.LOG_TAG, "Error initializing RFID Manager.", e);
            }

        } else if (MainActivity.devicePref.equals("3")) {

            nfcAdpt = NfcAdapter.getDefaultAdapter(this);
            // Check if the smartphone has NFC
            if (nfcAdpt == null) {
                Toast.makeText(this, "NFC not supported", Toast.LENGTH_LONG).show();
                finish();
            }
            // Check if NFC is enabled
            if (!nfcAdpt.isEnabled()) {
                Toast.makeText(this, "Enable NFC before using the app", Toast.LENGTH_LONG).show();
            }

            pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            try {
                ndef.addDataType("*/*");    /* Handles all MIME based dispatches. You should specify only the ones that you need. */
            }
            catch (IntentFilter.MalformedMimeTypeException e) {
                throw new RuntimeException("fail", e);
            }
            intentFiltersArray = new IntentFilter[] {ndef, };
            techListsArray = new String[][] { new String[] { NfcF.class.getName() } };


        } else {
            // FIXME: do something here
        }

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "No location permission!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListenerGPS);

        txtTag.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    updateTextViews();
                }
            }
        });
    }

    public void onNewIntent(Intent intent) {
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        //do something with tagFromIntent
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (MainActivity.devicePref.equals("1")) {
            try {
                RfidManager.deinit();
            } catch (RfidException ignored) {
                // FIXME: do something here
            }
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
//        isLocationEnabled();
        if (MainActivity.devicePref.equals("1")) {
            registerReceiver(mRecvr, mFilter);
        } else if (MainActivity.devicePref.equals("3")) {
            nfcAdpt.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
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
        } else if (MainActivity.devicePref.equals("3")) {
            nfcAdpt.disableForegroundDispatch(this);
        }
    }

    private void updateTextViews() {
        Date now = new Date();
        String datetime = "MM-dd-yyyy kk:mm:ss";
//        DateFormat df = new DateFormat();
        lblCSJ.setText(MainActivity.csjPref);
        lblTagger.setText(MainActivity.taggerPref);
        lblOffice.setText(MainActivity.officePref);
        lblDateTime.setText(DateFormat.format(datetime, now).toString());
        lblLon.setText(Double.toString(lon));
        lblLat.setText(Double.toString(lat));
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

    public void saveScan(View view) {

        String csj = String.valueOf(lblCSJ.getText());
        String lot = String.valueOf(txtLot.getText());
        String sublot = String.valueOf(txtSubLot.getText());
        String core = String.valueOf(txtCore.getText());
        String core_id = csj + "-" + lot + "-" + sublot + "-" + core;

        String inspector = String.valueOf(lblTagger.getText());
        String office = String.valueOf(lblOffice.getText());

        String stamp = String.valueOf(lblDateTime.getText());
        String lon = String.valueOf(lblLon.getText());
        String lat = String.valueOf(lblLat.getText());
        String tag = String.valueOf(txtTag.getText());

        if ((csj.trim().length() == 0) || (lot.trim().length() == 0) || (sublot.trim().length() == 0) || (core.trim().length() == 0) || (inspector.trim().length() == 0) || (stamp.trim().length() == 0) || (lon.trim().length() == 0) || (lat.trim().length() == 0) || (tag.trim().length() == 0)) {
            Toast.makeText(this, "Fill empty fields!", Toast.LENGTH_LONG).show();
            return;
        }

        int lot_number = Integer.parseInt(lot.trim());
        int sublot_number = Integer.parseInt(sublot.trim());
        int core_number = Integer.parseInt(core.trim());

        if (csj.trim().length() != 9) {
            Toast.makeText(this, "CSJ must be 9 characters long!", Toast.LENGTH_LONG).show();
            return;
        }

        if (inspector.trim().length() == 0) {
            Toast.makeText(this, "Missing inspector", Toast.LENGTH_LONG).show();
            return;
        }

        if (office.trim().length() == 0) {
            Toast.makeText(this, "Missing office", Toast.LENGTH_LONG).show();
            return;
        }

        if ((lot_number < 0) || (lot_number > 100)) {
            Toast.makeText(this, "Invalid lot number!", Toast.LENGTH_LONG).show();
            return;
        }

        if ((sublot_number < 0) || (sublot_number > 4)) {
            Toast.makeText(this, "Invalid sublot number!", Toast.LENGTH_LONG).show();
            return;
        }

        if ((core_number < 0) || (core_number > 2)) {
            Toast.makeText(this, "Invalid core number!", Toast.LENGTH_LONG).show();
            return;
        }

        for (String task : MainActivity.coreList) {
            if (task.equals(core_id)) {
                Toast.makeText(this, "Core ID already tagged!", Toast.LENGTH_LONG).show();
                return;
            }
        }

        SQLiteDatabase db = MainActivity.mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CoreDB.CoreEntry.COL_CORE_NAME, core_id);
        values.put(CoreDB.CoreEntry.COL_CORE_TAGGER, inspector);
        values.put(CoreDB.CoreEntry.COL_CORE_OFFICE, office);
        values.put(CoreDB.CoreEntry.COL_CORE_STAMP, stamp);
        values.put(CoreDB.CoreEntry.COL_CORE_LON, lon);
        values.put(CoreDB.CoreEntry.COL_CORE_LAT, lat);
        values.put(CoreDB.CoreEntry.COL_CORE_RFID, tag);
        db.insertWithOnConflict(CoreDB.CoreEntry.TABLE,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        finish();
    }

    private final LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            lat=location.getLatitude();
            lon=location.getLongitude();
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
        @Override
        public void onProviderEnabled(String provider) {
        }
        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private void onRfidReady() {
        try {
            RfidParameters parms = RfidManager.getParameters();
            parms.setOutputMode(RfidConstants.OUTPUT_MODE_INTENT);
            RfidManager.setParameters(parms);
        } catch (RfidException e) {
            Log.e(MainActivity.LOG_TAG, "Error setting RFID parameters.", e);
        }
    }

    private void onScanComplete(Intent intent) {
        String act = intent.getAction();
        if (act.equals(RfidConstants.ACTION_RFID_TAG_SCANNED)) {
            String tagId = intent.getStringExtra(RfidConstants.RFID_FIELD_ID);
            txtTag.setText(tagId);
        } else if (act.equals(RfidConstants.ACTION_RFID_STOP_SCAN_NOTIFICATION)) {
            Log.d(MainActivity.LOG_TAG, "Scanning stopped");
        }
        updateTextViews();
    }

    public void endThis(View view) {
        finish();
    }


}
