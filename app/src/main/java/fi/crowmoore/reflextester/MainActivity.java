package fi.crowmoore.reflextester;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.Scope;
import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameUtils;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import static fi.crowmoore.reflextester.OptionsActivity.PREFERENCES;

public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private GoogleApiClient googleApiClient;
    private static final int RC_SIGN_IN = 9001;
    private boolean signInClicked = false;
    private boolean resolvingConnectionFailure = false;
    private boolean autoStartSignInFlow = true;
    private boolean signInFlow = false;
    private boolean explicitSignOut = false;
    private String name;
    private String email;
    private GoogleSignInOptions signInOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);

        signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Games.API)
                .addScope(Games.SCOPE_GAMES)
                .build();
    }

    protected void onStart() {
        super.onStart();
        if (!signInFlow && !explicitSignOut) {
            googleApiClient.connect();
        }
    }

    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    public void signIn() {
        signInClicked = true;
        googleApiClient.connect();
        //Intent intent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        //startActivityForResult(intent, RC_SIGN_IN);
    }

    public void signOut() {
        signInClicked = false;
        explicitSignOut = true;
        if (googleApiClient != null && googleApiClient.isConnected()) {
            Games.signOut(googleApiClient);
            googleApiClient.disconnect();

            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_button).setVisibility(View.GONE);
        }
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
                Toast.makeText(getApplicationContext(), "Signed in", Toast.LENGTH_SHORT).show();
                findViewById(R.id.sign_in_button).setVisibility(View.GONE);
                findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
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
        findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Toast.makeText(getApplicationContext(), "Connection suspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if(resolvingConnectionFailure) {
            return;
        }
        if(signInClicked || autoStartSignInFlow) {
            autoStartSignInFlow = false;
            signInClicked = false;
            resolvingConnectionFailure = true;

            if(!BaseGameUtils.resolveConnectionFailure(this,
                    googleApiClient, connectionResult,
                    RC_SIGN_IN, getString(R.string.sign_in_error))) {
                resolvingConnectionFailure = false;
            }
        }
        findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
        findViewById(R.id.sign_out_button).setVisibility(View.GONE);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.regular_play: startRegularPlay(); break;
            case R.id.hardcore_play: startHardcorePlay(); break;
            case R.id.options: openOptions(); break;
            case R.id.sign_in_button: signIn(); break;
            case R.id.sign_out_button: signOut(); break;
        }
    }
}
