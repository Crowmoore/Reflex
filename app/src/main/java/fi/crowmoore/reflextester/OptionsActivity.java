package fi.crowmoore.reflextester;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;

public class OptionsActivity extends AppCompatActivity {

    public static final String PREFERENCES = "PreferencesFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        final SharedPreferences settings = getBaseContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = settings.edit();

        SeekBar seekBar = (SeekBar) findViewById(R.id.slider_volume);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editor.putInt("Volume", seekBar.getProgress());
                editor.apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        if(settings.contains("Volume")) {
            int volume = settings.getInt("Volume", 50);
            seekBar.setProgress(volume);
        }
    }

    public void onBackButtonClick(View view) {
        finish();
    }
}
