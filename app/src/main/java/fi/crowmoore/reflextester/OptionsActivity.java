package fi.crowmoore.reflextester;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;

public class OptionsActivity extends AppCompatActivity {

    public static final String PREFERENCES = "PreferencesFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        final SharedPreferences settings = getBaseContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = settings.edit();

        CheckBox muteCheckBox = (CheckBox) findViewById(R.id.mute_sound);

        if(settings.contains("Muted")) {
            boolean muted = settings.getBoolean("Muted", false);
            muteCheckBox.setChecked(muted);
        }

        muteCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                if(button.isChecked()) {
                    editor.putBoolean("Muted", true);
                    editor.apply();
                } else {
                    editor.putBoolean("Muted", false);
                    editor.apply();
                }
            }
        });
    }
    public void onBackButtonClick(View view) {
        finish();
        overridePendingTransition(R.anim.open_activity, R.anim.close_activity);
    }
}
