package fi.crowmoore.reflextester;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Game;
import com.google.android.gms.games.Games;

import static fi.crowmoore.reflextester.OptionsActivity.PREFERENCES;

public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private GoogleApiClient googleApiClient;
    private static final int RC_SIGN_IN = 9001;
    private String name;
    private String email;
    private GoogleSignInOptions signInOptions;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        initializeAndConnect();
    }

    private void signIn() {
        Intent intent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(intent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if(result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                name = account.getDisplayName();
                email = account.getEmail();
                editor.putBoolean("SignedIn", true);
                editor.apply();
                Toast.makeText(getApplicationContext(), "Signed in", Toast.LENGTH_SHORT).show();
                findViewById(R.id.sign_in).setVisibility(View.GONE);
                Log.d("tag", "Name: " + name + " email: " + email);
            } else {
                Toast.makeText(getApplicationContext(), "Unable to sign in", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void startRegularPlay() {
        startActivity(new Intent(MainActivity.this, RegularPlay.class));
    }

    public void startHardcorePlay() {
        startActivity(new Intent(MainActivity.this, HardcorePlay.class));
    }

    public void openOptions() {
        startActivity(new Intent(MainActivity.this, OptionsActivity.class));
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
        findViewById(R.id.sign_in).setVisibility(View.GONE);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Toast.makeText(getApplicationContext(), "Connection suspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_SHORT).show();
        findViewById(R.id.sign_in).setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.regular_play: startRegularPlay(); break;
            case R.id.hardcore_play: startHardcorePlay(); break;
            case R.id.options: openOptions(); break;
            case R.id.sign_in: signIn(); break;
        }
    }

    private void initializeAndConnect() {
        signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        settings = getBaseContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        editor = settings.edit();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                //.addApi(Games.API)
                //.addScope(Games.SCOPE_GAMES)
                .addApi(Auth.GOOGLE_SIGN_IN_API, signInOptions)
                .build();
        googleApiClient.connect();


        findViewById(R.id.sign_in).setOnClickListener(this);
        if(settings.contains("SignedIn")) {
            boolean signedIn = settings.getBoolean("SignedIn", false);
            if(signedIn) {
                findViewById(R.id.sign_in).setVisibility(View.GONE);
            }
        }
    }
}
