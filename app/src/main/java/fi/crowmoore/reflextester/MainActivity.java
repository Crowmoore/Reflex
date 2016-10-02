package fi.crowmoore.reflextester;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.data.Application;
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
    public static final int RC_SIGN_IN = 9001;
    public static final int REQUEST_LEADERBOARD = 100;
    private LeaderboardDialogFragment dialog;
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
    }

    protected void onStart() {
        super.onStart();
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Games.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        if (!signInFlow && !explicitSignOut) {
            googleApiClient.connect();
        }
    }

    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    protected void onPause() {
        super.onPause();
        googleApiClient.disconnect();
    }

    protected void onResume() {
        super.onPause();
        googleApiClient.connect();
    }

    public void signIn() {
        if(googleApiClient.isConnected()) {
            Log.d("tag", "GoogleAPIClient already connected, button should not have been visible");
        } else {
            signInClicked = true;
            googleApiClient.connect();
            Log.d("tag", "GoogleAPIClient connected via manual input");
        }
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

    public void startRegularPlay() {
        startActivity(new Intent(MainActivity.this, RegularPlay.class));
    }

    public void startHardcorePlay() {
        startActivity(new Intent(MainActivity.this, HardcorePlay.class));
    }

    public void openOptions() {
        startActivity(new Intent(MainActivity.this, OptionsActivity.class));
    }

    private void showLeaderboard(String type) {
        switch(type) {
            case "Regular": startActivityForResult(Games.Leaderboards.getLeaderboardIntent(googleApiClient,
                            "CgkI1sfZypEcEAIQCA"), REQUEST_LEADERBOARD);
                            dialog.dismiss();
                            break;
            case "Hardcore": startActivityForResult(Games.Leaderboards.getLeaderboardIntent(googleApiClient,
                            "CgkI1sfZypEcEAIQCQ"), REQUEST_LEADERBOARD);
                            dialog.dismiss();
                            break;
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("tag", "GoogleAPIClient Connected");
        findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d("tag", "GoogleAPIClient connection suspended");
        findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
        findViewById(R.id.sign_out_button).setVisibility(View.GONE);
    }

    public void showDialog() {
        dialog = new LeaderboardDialogFragment();
        dialog.show(getFragmentManager(), null);
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
            case R.id.leaderboards: showDialog(); break;
            case R.id.sign_in_button: signIn(); break;
            case R.id.sign_out_button: signOut(); break;
            case R.id.regular_leaderboard: showLeaderboard("Regular"); break;
            case R.id.hardcore_leaderboard: showLeaderboard("Hardcore"); break;
        }
    }

    public static class LeaderboardDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();

            final View dialogView = inflater.inflate(R.layout.leaderboard_dialog, null);
            builder.setView(dialogView);
            return builder.create();
        }
    }
}