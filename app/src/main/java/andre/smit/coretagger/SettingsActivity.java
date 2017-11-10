package andre.smit.coretagger;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.widget.Switch;

public class SettingsActivity extends AppCompatActivity {

    public static final String KEY_PREF_MODE_SWITCH = "mode_switch";
    public static final String KEY_PREF_TAGGER_EDIT = "tagger_edit";
    public static final String KEY_PREF_CSJ_EDIT = "csj_edit";
    public static final String KEY_PREF_OFFICE_EDIT = "office_edit";
    public static final String KEY_PREF_DEVICE_LIST = "device_list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        //create a dialog to ask yes no question whether or not the user wants to exit
    }

}
