package fi.crowmoore.reflextester;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import static fi.crowmoore.reflextester.OptionsActivity.PREFERENCES;

public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private GoogleApiClient googleApiClient;
    public static final int RC_SIGN_IN = 9001;
    public static final int REQUEST_LEADERBOARD = 100;
    public static final int REQUEST_ACHIEVEMENTS = 101;
    private LeaderboardDialogFragment dialog;
    private SignInDialogFragment signInDialog;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    private AchievementManager achievementManager;
    private boolean signInClicked = false;
    private boolean resolvingConnectionFailure = false;
    private boolean autoStartSignInFlow = true;
    private boolean signInFlow = false;
    private boolean explicitSignOut = false;
    private TextView signInInfo;
    private String mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        setOnClickListeners();

        settings = getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        editor = settings.edit();
        explicitSignOut = settings.getBoolean("ExplicitSignOut", false);

        if(!explicitSignOut && googleApiClient == null) {
            buildApiClient();
        } else {
            findViewById(R.id.leaderboards).setEnabled(false);
            findViewById(R.id.achievements).setEnabled(false);
        }
    }

    protected void setOnClickListeners() {
        findViewById(R.id.regular_play).setOnClickListener(this);
        findViewById(R.id.hardcore_play).setOnClickListener(this);
        findViewById(R.id.options).setOnClickListener(this);
        findViewById(R.id.leaderboards).setOnClickListener(this);
        findViewById(R.id.achievements).setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        explicitSignOut = settings.getBoolean("ExplicitSignOut", false);
        if(googleApiClient == null && !explicitSignOut) {
            buildApiClient();
        }
        if(googleApiClient != null && !explicitSignOut) {
            googleApiClient.connect();
        }
        if(explicitSignOut) {
            findViewById(R.id.leaderboards).setEnabled(false);
            findViewById(R.id.achievements).setEnabled(false);
        }
    }

    public void buildApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();
        achievementManager = new AchievementManager(googleApiClient);
    }

    public void signIn() {
        signInClicked = true;
        editor.putBoolean("ExplicitSignOut", false);
        editor.apply();
        if(googleApiClient == null) {
            buildApiClient();
        }
        googleApiClient.connect();
    }

    public void signOut() {
        signInClicked = false;
        editor.putBoolean("ExplicitSignOut", true);
        editor.apply();
        Games.signOut(googleApiClient);
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();

            findViewById(R.id.leaderboards).setEnabled(false);
            findViewById(R.id.achievements).setEnabled(false);
        }
    }

    public void startRegularPlay() {
        if(googleApiClient != null && googleApiClient.isConnected()) {
            startActivity(new Intent(MainActivity.this, RegularPlay.class));
            overridePendingTransition(R.anim.open_activity, R.anim.close_activity);
        } else {
            mode = "Regular";
            signInDialog = new SignInDialogFragment();
            signInDialog.show(getFragmentManager(), null);
        }
    }

    public void startHardcorePlay() {
        if(googleApiClient != null && googleApiClient.isConnected()) {
            startActivity(new Intent(MainActivity.this, HardcorePlay.class));
            overridePendingTransition(R.anim.open_activity, R.anim.close_activity);
        } else {
            mode = "Hardcore";
            signInDialog = new SignInDialogFragment();
            signInDialog.show(getFragmentManager(), null);
        }
    }

    public void openOptions() {
        startActivity(new Intent(MainActivity.this, OptionsActivity.class));
        overridePendingTransition(R.anim.open_activity, R.anim.close_activity);
    }

    private void showLeaderboard(String type) {
        switch(type) {
            case "Regular": startActivityForResult(Games.Leaderboards.getLeaderboardIntent(googleApiClient,
                            getString(R.string.leaderboard_regular_mode)), REQUEST_LEADERBOARD);
                            dialog.dismiss();
                            break;
            case "Hardcore": startActivityForResult(Games.Leaderboards.getLeaderboardIntent(googleApiClient,
                             getString(R.string.leaderboard_hardcore_mode)), REQUEST_LEADERBOARD);
                             dialog.dismiss();
                             break;
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("tag", "GoogleAPIClient Connected");
        findViewById(R.id.leaderboards).setEnabled(true);
        findViewById(R.id.achievements).setEnabled(true);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d("tag", "GoogleAPIClient connection suspended");
        googleApiClient.connect();
    }

    public void showDialog() {
        dialog = new LeaderboardDialogFragment();
        dialog.show(getFragmentManager(), null);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("tag", "GoogleAPIClient connection failed. Resolving");
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
        findViewById(R.id.leaderboards).setEnabled(false);
        findViewById(R.id.achievements).setEnabled(false);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.regular_play: startRegularPlay(); break;
            case R.id.hardcore_play: startHardcorePlay(); break;
            case R.id.options: openOptions(); break;
            case R.id.leaderboards: showDialog(); break;
            case R.id.regular_leaderboard: showLeaderboard("Regular"); break;
            case R.id.hardcore_leaderboard: showLeaderboard("Hardcore"); break;
            case R.id.achievements: showAchievements(); break;
        }
    }

    public void showAchievements() {
        startActivityForResult(achievementManager.getAchievementsIntent(), REQUEST_ACHIEVEMENTS);
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
        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        }
    }

    public static class SignInDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();

            final View dialogView = inflater.inflate(R.layout.sign_in_dialog, null);
            builder.setView(dialogView);
            return builder.create();
        }
        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        }
    }

    public void onNotNowClicked(View view) {
        editor.putBoolean("ExplicitSignOut", true);
        editor.apply();
        startGame();
    }

    public void onSignInClicked(View view) {
        editor.putBoolean("ExplicitSignOut", false);
        editor.apply();
        if(googleApiClient == null) {
            buildApiClient();
        } else {
            googleApiClient.connect();
        }
        startGame();
    }

    private void startGame() {
        signInDialog.dismiss();
        switch(mode) {
            case "Regular": startActivity(new Intent(MainActivity.this, RegularPlay.class));
                            overridePendingTransition(R.anim.open_activity, R.anim.close_activity); break;
            case "Hardcore": startActivity(new Intent(MainActivity.this, HardcorePlay.class));
                             overridePendingTransition(R.anim.open_activity, R.anim.close_activity); break;
        }
    }
}