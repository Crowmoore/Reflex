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
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import static fi.crowmoore.reflextester.OptionsActivity.PREFERENCES;

public class MainActivity extends FragmentActivity implements View.OnClickListener {

    private GoogleApiClient googleApiClient;
    public static final int RC_SIGN_IN = 9001;
    public static final int REQUEST_LEADERBOARD = 100;
    public static final int REQUEST_ACHIEVEMENTS = 101;
    private LeaderboardDialogFragment leaderboardDialog;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    private AchievementManager achievementManager;
    private SignInDialogFragment signInDialog;
    private boolean signInClicked = false;
    private boolean resolvingConnectionFailure = false;
    private boolean autoStartSignInFlow = true;
    private boolean signInFlow = false;
    private boolean explicitSignOut = false;
    private TextView signInInfo;
    private String mode;
    private Reflex reflex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        setOnClickListeners();
        reflex = (Reflex) getApplicationContext();

        settings = getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        editor = settings.edit();
        explicitSignOut = settings.getBoolean("ExplicitSignOut", false);
        if(!explicitSignOut) {
            reflex.setManager(this);
            achievementManager = new AchievementManager(reflex.getManager().getApiClient());
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if(!explicitSignOut && reflex.getManager() != null) {
            reflex.getManager().setActivity(this);
        }
    }

    protected void setOnClickListeners() {
        findViewById(R.id.regular_play).setOnClickListener(this);
        findViewById(R.id.hardcore_play).setOnClickListener(this);
        findViewById(R.id.options).setOnClickListener(this);
        findViewById(R.id.leaderboards).setOnClickListener(this);
        findViewById(R.id.achievements).setOnClickListener(this);
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        explicitSignOut = settings.getBoolean("ExplicitSignOut", false);
//        if(googleApiClient == null && !explicitSignOut) {
//            buildApiClient();
//            Log.d("GoogleAPI", "build");
//        }
//        if(googleApiClient != null && !explicitSignOut) {
//            googleApiClient.connect();
//            Log.d("GoogleAPI", "connect");
//        }
//        if(explicitSignOut) {
//            Log.d("GoogleAPI", "nope");
//            findViewById(R.id.leaderboards).setEnabled(false);
//            findViewById(R.id.achievements).setEnabled(false);
//        }
//    }

    public void startRegularPlay() {
        if(reflex.getManager() != null && reflex.getManager().isConnected()) {
            startActivity(new Intent(MainActivity.this, RegularPlay.class));
            overridePendingTransition(R.anim.open_activity, R.anim.close_activity);
        } else {
            mode = "Regular";
            signInDialog = new SignInDialogFragment();
            signInDialog.show(getFragmentManager(), null);
            getFragmentManager().executePendingTransactions();
            signInDialog.getDialog().findViewById(R.id.dialog_sign_in_button).setOnClickListener(this);
            signInDialog.getDialog().findViewById(R.id.dialog_not_now_button).setOnClickListener(this);
        }
    }

    public void startHardcorePlay() {
        if(reflex.getManager() != null && reflex.getManager().isConnected()) {
            startActivity(new Intent(MainActivity.this, HardcorePlay.class));
            overridePendingTransition(R.anim.open_activity, R.anim.close_activity);
        } else {
            mode = "Hardcore";
            signInDialog = new SignInDialogFragment();
            signInDialog.show(getFragmentManager(), null);
            getFragmentManager().executePendingTransactions();
            signInDialog.getDialog().findViewById(R.id.dialog_sign_in_button).setOnClickListener(this);
            signInDialog.getDialog().findViewById(R.id.dialog_not_now_button).setOnClickListener(this);
        }
    }

    public void openOptions() {
        startActivity(new Intent(MainActivity.this, OptionsActivity.class));
        overridePendingTransition(R.anim.open_activity, R.anim.close_activity);
    }

    private void showLeaderboard(String type) {
        switch(type) {
            case "Regular": startActivityForResult(Games.Leaderboards.getLeaderboardIntent(reflex.getManager().getApiClient(),
                            getString(R.string.leaderboard_regular_mode)), REQUEST_LEADERBOARD);
                            leaderboardDialog.dismiss();
                            break;
            case "Hardcore": startActivityForResult(Games.Leaderboards.getLeaderboardIntent(reflex.getManager().getApiClient(),
                             getString(R.string.leaderboard_hardcore_mode)), REQUEST_LEADERBOARD);
                             leaderboardDialog.dismiss();
                             break;
        }
    }

//    @Override
//    public void onConnected(Bundle connectionHint) {
//        Log.d("tag", "GoogleAPIClient Connected");
//        findViewById(R.id.leaderboards).setEnabled(true);
//        findViewById(R.id.achievements).setEnabled(true);
//    }
//
//    @Override
//    public void onConnectionSuspended(int cause) {
//        Log.d("tag", "GoogleAPIClient connection suspended");
//        googleApiClient.connect();
//    }

    public void showDialog() {
        if(reflex.getManager() != null && reflex.getManager().isConnected()) {
            leaderboardDialog = new LeaderboardDialogFragment();
            leaderboardDialog.show(getFragmentManager(), null);
            getFragmentManager().executePendingTransactions();
            leaderboardDialog.getDialog().findViewById(R.id.regular_leaderboard).setOnClickListener(this);
            leaderboardDialog.getDialog().findViewById(R.id.hardcore_leaderboard).setOnClickListener(this);
        }
    }

//    @Override
//    public void onConnectionFailed(ConnectionResult connectionResult) {
//        Log.d("tag", "GoogleAPIClient connection failed. Resolving");
//        if(resolvingConnectionFailure) {
//            return;
//        }
//        if(signInClicked || autoStartSignInFlow) {
//            autoStartSignInFlow = false;
//            signInClicked = false;
//            resolvingConnectionFailure = true;
//
//            if(!BaseGameUtils.resolveConnectionFailure(this,
//                    googleApiClient, connectionResult,
//                    RC_SIGN_IN, getString(R.string.sign_in_error))) {
//                resolvingConnectionFailure = false;
//            }
//        }
//        findViewById(R.id.leaderboards).setEnabled(false);
//        findViewById(R.id.achievements).setEnabled(false);
//    }

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
            case R.id.dialog_sign_in_button: onSignInClicked(); break;
            case R.id.dialog_not_now_button: onNotNowClicked(); break;
        }
    }

    public void showAchievements() {
        if(reflex.getManager() != null && reflex.getManager().isConnected()) {
            startActivityForResult(achievementManager.getAchievementsIntent(), REQUEST_ACHIEVEMENTS);
        }
    }

//    public static class LeaderboardDialogFragment extends DialogFragment {
//        @Override
//        public Dialog onCreateDialog(Bundle savedInstanceState) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//            LayoutInflater inflater = getActivity().getLayoutInflater();
//
//            final View dialogView = inflater.inflate(R.layout.leaderboard_dialog, null);
//            builder.setView(dialogView);
//            return builder.create();
//        }
//        @Override
//        public void onActivityCreated(Bundle savedInstanceState) {
//            super.onActivityCreated(savedInstanceState);
//            getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
//        }
//    }

//    public static class SignInDialogFragment extends DialogFragment {
//        @Override
//        public Dialog onCreateDialog(Bundle savedInstanceState) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//            LayoutInflater inflater = getActivity().getLayoutInflater();
//
//            final View dialogView = inflater.inflate(R.layout.sign_in_dialog, null);
//            builder.setView(dialogView);
//            return builder.create();
//        }
//        @Override
//        public void onActivityCreated(Bundle savedInstanceState) {
//            super.onActivityCreated(savedInstanceState);
//            getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
//        }
//    }

    public void onNotNowClicked() {
        editor.putBoolean("ExplicitSignOut", true);
        editor.apply();
        startGame();
    }

    public void onSignInClicked() {
        editor.putBoolean("ExplicitSignOut", false);
        editor.apply();
        reflex.setManager(this);
        startGame();

    }

    private void startGame() {
        signInDialog.dismiss();
        switch(mode) {
            case "Regular": startActivity(new Intent(getApplicationContext(), RegularPlay.class));
                            overridePendingTransition(R.anim.open_activity, R.anim.close_activity); break;
            case "Hardcore": startActivity(new Intent(MainActivity.this, HardcorePlay.class));
                             overridePendingTransition(R.anim.open_activity, R.anim.close_activity); break;
        }
    }
}