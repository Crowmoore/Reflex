package fi.crowmoore.reflextester;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.android.gms.common.SignInButton;
import com.google.android.gms.games.Games;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static fi.crowmoore.reflextester.OptionsActivity.PREFERENCES;

public class MainActivity extends FragmentActivity implements View.OnClickListener {

    public static final String RESOURCE_PREFIX = "android.resource://fi.crowmoore.reflextester/";
    public static final int RC_SIGN_IN = 9001;
    public static final int REQUEST_LEADERBOARD = 100;
    public static final int REQUEST_ACHIEVEMENTS = 101;
    private LeaderboardDialogFragment leaderboardDialog;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    private AchievementManager achievementManager;
    private OptionsDialogFragment optionsDialog;
    private StatsDialogFragment statsDialog;
    private SoundDialogFragment soundDialog;
    private SignInDialogFragment signInDialog;
    private MediaPlayer player;
    private boolean explicitSignOut = false;
    private boolean muted = false;
    private float volume;
    private ArrayList<Integer> playlist;
    private Map<Integer, String> songDictionary;
    private String mode;
    private int currentSong;
    private Reflex reflex;
    private TextView signInInfo;
    private TextView songName;
    private SignInButton signInButton;
    private Button signOutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        songName = (TextView) findViewById(R.id.song_name);

        setOnClickListeners();
        reflex = (Reflex) getApplicationContext();

        settings = getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        editor = settings.edit();
        explicitSignOut = settings.getBoolean("ExplicitSignOut", false);
        muted = settings.getBoolean("Muted", false);
        volume = settings.getFloat("Volume", 0.5f);

        reflex.setMusicManager(this);
        reflex.getMusicManager().setVolume(volume);
        reflex.getMusicManager().play();
        songName.setText(reflex.getMusicManager().getSong());

        if(!explicitSignOut) {
            reflex.setManager(this);
            achievementManager = new AchievementManager(reflex.getManager().getApiClient());
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onPause() {
        super.onPause();
        reflex.getMusicManager().pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        reflex.getMusicManager().play();
        if(!explicitSignOut && reflex.getManager() == null) {
            reflex.getManager().setActivity(this);
        }
        if(!explicitSignOut && !reflex.getManager().isConnected()) {
            reflex.getManager().connect();
        }
    }

    protected void setOnClickListeners() {
        findViewById(R.id.regular_play).setOnClickListener(this);
        findViewById(R.id.hardcore_play).setOnClickListener(this);
        findViewById(R.id.stats).setOnClickListener(this);
        findViewById(R.id.options).setOnClickListener(this);
        findViewById(R.id.leaderboards).setOnClickListener(this);
        findViewById(R.id.achievements).setOnClickListener(this);
    }

    public void startRegularPlay() {
        startActivity(new Intent(MainActivity.this, RegularPlay.class));
        overridePendingTransition(R.anim.open_activity, R.anim.close_activity);
    }

    public void startHardcorePlay() {
        muted = settings.getBoolean("Muted", false);
        if(!muted) {
            startActivity(new Intent(MainActivity.this, HardcorePlay.class));
            overridePendingTransition(R.anim.open_activity, R.anim.close_activity);
        } else {
            openSoundDialog();
        }
    }

    public void openSoundDialog() {
        soundDialog = new SoundDialogFragment();
        soundDialog.show(getFragmentManager(), null);
        getFragmentManager().executePendingTransactions();
        soundDialog.getDialog().findViewById(R.id.dialog_madman_no).setOnClickListener(this);
        soundDialog.getDialog().findViewById(R.id.dialog_madman_yes).setOnClickListener(this);
    }

    public void openOptions() {
        optionsDialog = new OptionsDialogFragment();
        optionsDialog.show(getFragmentManager(), null);
        getFragmentManager().executePendingTransactions();

        signInButton = (SignInButton) optionsDialog.getDialog().findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(this);
        signOutButton = (Button) optionsDialog.getDialog().findViewById(R.id.sign_out_button);
        signOutButton.setOnClickListener(this);
        signInInfo = (TextView) optionsDialog.getDialog().findViewById(R.id.info);
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

    public void showDialog() {
        //if(reflex.getManager() != null && reflex.getManager().isConnected()) {
            leaderboardDialog = new LeaderboardDialogFragment();
            leaderboardDialog.show(getFragmentManager(), null);
            getFragmentManager().executePendingTransactions();
            leaderboardDialog.getDialog().findViewById(R.id.regular_leaderboard).setOnClickListener(this);
            leaderboardDialog.getDialog().findViewById(R.id.hardcore_leaderboard).setOnClickListener(this);
        //}
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.regular_play: startRegularPlay(); break;
            case R.id.hardcore_play: startHardcorePlay(); break;
            case R.id.stats: openStatsDialog(); break;
            case R.id.options: openOptions(); break;
            case R.id.sign_in_button: onOptionsSignInClicked(); break;
            case R.id.sign_out_button: onOptionsSignOutClicked(); break;
            case R.id.leaderboards: showDialog(); break;
            case R.id.regular_leaderboard: showLeaderboard("Regular"); break;
            case R.id.hardcore_leaderboard: showLeaderboard("Hardcore"); break;
            case R.id.achievements: showAchievements(); break;
            case R.id.dialog_sign_in_button: onSignInClicked(); break;
            case R.id.dialog_not_now_button: onNotNowClicked(); break;
            case R.id.close_button: statsDialog.dismiss(); break;
            case R.id.dialog_madman_no: onNoMadmanClicked(); break;
            case R.id.dialog_madman_yes: onYesMadmanClicked(); break;
        }
    }

    public void onNoMadmanClicked() {
        muted = false;
        editor.putBoolean("Muted", false);
        editor.apply();
        soundDialog.dismiss();
        startActivity(new Intent(MainActivity.this, HardcorePlay.class));
        overridePendingTransition(R.anim.open_activity, R.anim.close_activity);
    }

    public void onYesMadmanClicked() {
        if(!explicitSignOut && reflex.getManager().isConnected()) {
            achievementManager.unlockAchievement(getString(R.string.achievement_verified_madman));
        }
        soundDialog.dismiss();
        startActivity(new Intent(MainActivity.this, HardcorePlay.class));
        overridePendingTransition(R.anim.open_activity, R.anim.close_activity);
    }

    public void onOptionsSignInClicked() {
        explicitSignOut = false;
        editor.putBoolean("ExplicitSignOut", false);
        editor.apply();
        reflex.setManager(this);
        signInButton.setVisibility(View.GONE);
        signOutButton.setVisibility(View.VISIBLE);
        signInInfo.setText(R.string.signed_in);
    }

    public void onOptionsSignOutClicked() {
        explicitSignOut = true;
        editor.putBoolean("ExplicitSignOut", true);
        editor.apply();
        if (reflex.getManager().isConnected()) {
            Games.signOut(reflex.getManager().getApiClient());
            reflex.getManager().disconnect();
            signInButton.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.GONE);
            signInInfo.setText(R.string.not_signed_in);
        }
    }

    public void openStatsDialog() {
        statsDialog = new StatsDialogFragment();
        statsDialog.show(getFragmentManager(), null);
        getFragmentManager().executePendingTransactions();
        statsDialog.getDialog().findViewById(R.id.close_button).setOnClickListener(this);
    }

    public void showAchievements() {
        if(reflex.getManager() != null && reflex.getManager().isConnected()) {
            achievementManager = new AchievementManager(reflex.getManager().getApiClient());
            startActivityForResult(achievementManager.getAchievementsIntent(), REQUEST_ACHIEVEMENTS);
        }
    }

    public void onNotNowClicked() {
        explicitSignOut = true;
        editor.putBoolean("ExplicitSignOut", true);
        editor.apply();
        startGame();
    }

    public void onSignInClicked() {
        explicitSignOut = false;
        editor.putBoolean("ExplicitSignOut", false);
        editor.apply();
        reflex.setManager(this);
        if(reflex.getManager() != null && reflex.getManager().isConnected()) {
            startGame();
        }
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