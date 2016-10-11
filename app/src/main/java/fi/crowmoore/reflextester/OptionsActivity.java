package fi.crowmoore.reflextester;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

public class OptionsActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String PREFERENCES = "PreferencesFile";
    private TextView signInInfo;
    private boolean explicitSignOut = false;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    private Reflex reflex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        reflex = (Reflex) getApplicationContext();

        signInInfo = (TextView) findViewById(R.id.info);
        settings = getBaseContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        editor = settings.edit();

        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.button_back).setOnClickListener(this);

        CheckBox muteCheckBox = (CheckBox) findViewById(R.id.mute_sound);

        if(settings.contains("Muted")) {
            boolean muted = settings.getBoolean("Muted", false);
            muteCheckBox.setChecked(muted);
        }

        muteCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                editor.putBoolean("Muted", button.isChecked());
                editor.apply();
            }
        });

        explicitSignOut = settings.getBoolean("ExplicitSignOut", false);

        if(!explicitSignOut && reflex.getManager() == null) {
            reflex.setManager(this);
        }

        if(explicitSignOut){
            Log.d("Regular", "manager signed out");
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_button).setVisibility(View.GONE);
            signInInfo.setText(R.string.not_signed_in);
        } else {
            reflex.getManager().setActivity(this);
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
            signInInfo.setText(R.string.signed_in);
        }
    }

    public void signIn() {
        editor.putBoolean("ExplicitSignOut", false);
        editor.apply();
        reflex.setManager(this);
        findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
        signInInfo.setText(R.string.signed_in);

    }

    public void signOut() {
        editor.putBoolean("ExplicitSignOut", true);
        editor.apply();
        if (reflex.getManager().isConnected()) {
            Games.signOut(reflex.getManager().getApiClient());
            reflex.getManager().disconnect();
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_button).setVisibility(View.GONE);
            signInInfo.setText(R.string.not_signed_in);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if(!explicitSignOut && reflex.getManager() != null) {
            reflex.getManager().setActivity(this);
        }
    }

    public void onBackButtonClick() {
        finish();
        overridePendingTransition(R.anim.open_activity, R.anim.close_activity);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.sign_in_button: signIn(); break;
            case R.id.sign_out_button: signOut(); break;
            case R.id.button_back: onBackButtonClick(); break;
        }
    }
}
