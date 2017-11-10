package andre.smit.coretagger;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import andre.smit.coretagger.db.CoreDB;
import andre.smit.coretagger.db.CoreDBHelper;

public class ViewActivity extends AppCompatActivity {

//    private CoreDBHelper mHelper;

    private TextView lblCoreID;
    private TextView lblTagger;
    private TextView lblOffice;
    private TextView lblDateTime;
    private TextView lblLon;
    private TextView lblLat;
    private TextView lblTag;

    private Button btnInfo;

    private String coreID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);

        coreID = getIntent().getStringExtra("CORE_ID"); // as passed on from mainactivity

        lblCoreID = (TextView) findViewById(R.id.lblCoreID);
        lblTagger = (TextView) findViewById(R.id.lblTagger);
        lblOffice = (TextView) findViewById(R.id.lblOffice);
        lblDateTime = (TextView) findViewById(R.id.lblDateTime);
        lblLon = (TextView) findViewById(R.id.lblLon);
        lblLat = (TextView) findViewById(R.id.lblLat);
        lblTag = (TextView) findViewById(R.id.lblTag);

        btnInfo = (Button) findViewById(R.id.btnInfo);

//        mHelper = new CoreDBHelper(this);
        Cursor cursor = MainActivity.getAllData(coreID);
        cursor.moveToFirst();
        String name = cursor.getString(cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_NAME));
        String tagger = cursor.getString(cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_TAGGER));
        String office = cursor.getString(cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_OFFICE));
        String stamp = cursor.getString(cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_STAMP));
        String lon = cursor.getString(cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_LON));
        String lat = cursor.getString(cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_LAT));
        String rfid = cursor.getString(cursor.getColumnIndex(CoreDB.CoreEntry.COL_CORE_RFID));

        lblCoreID.setText(name);
        lblTagger.setText(tagger);
        lblOffice.setText(office);
        lblDateTime.setText(stamp);
        lblLon.setText(lon);
        lblLat.setText(lat);
        lblTag.setText(rfid);

        if (MainActivity.modePref) {
            btnInfo.setText("Validate");
        } else {
            btnInfo.setText("Delete");
        }

    }

    public void endThis(View view) {
        finish();
    }

    public void processCore(View view) {
        if (MainActivity.modePref) {
            validateCore();
        } else {
            deleteCore();
        }
    }

    private void validateCore() {
        Intent intent = new Intent(this, ValidateActivity.class);
        intent.putExtra("CORE_ID", coreID);
        startActivity(intent);
        finish();
    }

    private void deleteCore() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this).setCancelable(false);
        alertDialog.setTitle(coreID);
        alertDialog.setMessage("Delete this core?");
        alertDialog.setNegativeButton("No",new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which){
                dialog.cancel();
            }
        });
        alertDialog.setPositiveButton("Yes",new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which){
                SQLiteDatabase db = MainActivity.mHelper.getWritableDatabase();
                db.delete(CoreDB.CoreEntry.TABLE,
                        CoreDB.CoreEntry.COL_CORE_NAME + " = ?",
                        new String[]{coreID});
                db.close();
                finish();
            }
        });
        AlertDialog alert=alertDialog.create();
        alert.show();
    }

}
