package fi.crowmoore.reflextester;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.Leaderboards;
import com.google.example.games.basegameutils.BaseGameUtils;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static fi.crowmoore.reflextester.MainActivity.RC_SIGN_IN;
import static fi.crowmoore.reflextester.OptionsActivity.PREFERENCES;

public class HardcorePlay extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient googleApiClient;
    private boolean resolvingConnectionFailure = false;
    private boolean signInClicked = false;
    private boolean autoStartSignInFlow = true;
    private boolean signInFlow = false;
    private boolean explicitSignOut = false;

    private int score;
    private ImageButton red;
    private ImageButton blue;
    private ImageButton green;
    private ImageButton yellow;
    private TextView scoreView;
    private TextView scoreResult;
    private TextView highscoreResult;
    private TextView tapCount;
    private TextView averageTime;
    private TextView countdownText;
    private List<String> commandsList;
    private List<Long> commandTimesList;
    private boolean running;
    private long interval;
    private int selection;
    private int previous;
    private SoundPool soundPool;
    private boolean madman = false;
    private boolean loaded;
    private int low1;
    private int low2;
    private int high1;
    private int high2;
    private AdView adView;
    private ReactionTime reactionTime;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    private AchievementManager achievementManager;
    private boolean starting;
    private Countdown countdown;
    private boolean muted;
    private Dialog scoreDialog;
    private SoundDialogFragment soundDialog;
    private int taps;
    private final int FIRST = 0;
    private final int DECREMENT_AMOUNT = 2;
    private final int MINIMUM_INTERVAL = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hardcore_play);

        settings = getBaseContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        muted = settings.getBoolean("Muted", false);
        editor = settings.edit();

        if(muted) {
            soundDialog = new SoundDialogFragment();
            soundDialog.show(getFragmentManager(), null);
        } else {
            initializeGame();
        }
    }

    public void initializeGame() {
        initializeComponents();

        if(!explicitSignOut && googleApiClient == null) {
            buildApiClient();
        } else {
            startGame();
        }
    }

    public void startGame() {
        if(madman && googleApiClient != null && googleApiClient.isConnected()) {
            achievementManager.unlockAchievement(getString(R.string.achievement_verified_madman));
        }
        countdown.execute();
    }

    public void buildApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();
        achievementManager = new AchievementManager(googleApiClient);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("tag", "GoogleAPIClient Connected");
        startGame();
    }
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d("tag", "GoogleAPIClient connection suspended");
        googleApiClient.connect();
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
    }

    private boolean checkIfCorrect(String command) {
        if(commandsList.isEmpty() || commandTimesList.isEmpty()) {
            return false;
        }

        if(command.equals(commandsList.get(FIRST))) {
            score += getScore(commandTimesList.get(FIRST), System.currentTimeMillis());
            commandsList.remove(0);
            taps += 1;
            scoreView.setText(String.valueOf(score));
            if(googleApiClient != null && googleApiClient.isConnected()) {
                checkScoreForAchievement(score);
            }
            return true;
        }
        return false;
    }

    private void checkScoreForAchievement(int score) {
        if(score >= 10) {
            achievementManager.unlockAchievement(getString(R.string.achievement_are_you_sure_about_this));
        }
        if(score >= 100) {
            achievementManager.unlockAchievement(getString(R.string.achievement_not_going_any_farther));
        }
        if(score >= 3000) {
            achievementManager.unlockAchievement(getString(R.string.achievement_are_you_cheating));
        }
    }

    protected void endGame() {
        running = false;
        soundPool.release();
        HighscoreManager highscore = new HighscoreManager(getBaseContext(), score, "Hardcore");
        boolean newHighscore = highscore.isHighscore();
        if(googleApiClient != null && googleApiClient.isConnected()) {
            Games.Leaderboards.submitScore(googleApiClient, getString(R.string.leaderboard_hardcore_mode), score);
        }
        int currentHighscore = highscore.getHighscore();
        createScoreDialog();
        incrementTimesPlayed();
        tapCount.setText("Taps: " + taps);
        float average = reactionTime.getAverageReactionTime();
        averageTime.setText(String.format(Locale.US, "Average reaction time: %.02f sec", average));
        scoreResult.setText("Score: " + score);
        if(newHighscore) {
            highscoreResult.setText("New highscore: " + currentHighscore);
        } else {
            highscoreResult.setText("Highscore: " + currentHighscore);
        }
    }

    private void incrementTimesPlayed() {
        int timesPlayed = settings.getInt("TimesPlayedHardcore", 0);
        timesPlayed = timesPlayed + 1;
        editor.putInt("TimesPlayedHardcore", timesPlayed);
        editor.apply();
        if(googleApiClient != null && googleApiClient.isConnected()) {
            incrementAchievements();
        }
    }

    private void incrementAchievements() {
        achievementManager.incrementAchievement(getString(R.string.achievement_hardcore_rookie), 1);
        achievementManager.incrementAchievement(getString(R.string.achievement_hardcore_seasoned), 1);
        achievementManager.incrementAchievement(getString(R.string.achievement_hardcore_senior), 1);
        achievementManager.incrementAchievement(getString(R.string.achievement_hardcore_expert), 1);
        achievementManager.incrementAchievement(getString(R.string.achievement_hardcore_grandmaster), 1);
    }

//    private void loadPlayerRank() {
//        if(googleApiClient != null && googleApiClient.isConnected()) {
//            Games.Leaderboards.loadCurrentPlayerLeaderboardScore(googleApiClient, getString(R.string.leaderboard_hardcore_mode), LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC).setResultCallback(new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {
//                @Override
//                public void onResult(final Leaderboards.LoadPlayerScoreResult scoreResult) {
//                    LeaderboardScore lbs = scoreResult.getScore();
//                    String rank;
//                    try {
//                        rank = lbs.getDisplayRank();
//                        leaderboardRank.setText("Leaderboard rank: " + rank);
//                    } catch (Exception e) {
//                        leaderboardRank.setText("Could not retrieve leaderboard rank");
//                    }
//                }
//            });
//        }
//    }

    private class GameLoop extends AsyncTask<Void, Bundle, Void> {
        @Override
        protected Void doInBackground(Void... parameters) {
            while(running) {
                if(starting) { showCountDown(); }
                String command = getRandomCommand();
                commandsList.add(command);
                commandTimesList.add(System.currentTimeMillis());
                Bundle bundle = setupTaskCommandBundle(command, 1);
                publishProgress(bundle);
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    Log.e("Error", "AsyncTask interrupted!");
                }
                decrementIntervalBy(DECREMENT_AMOUNT);
            }
            return null;
        }

        protected void onProgressUpdate(Bundle... parameters) {
            int task = parameters[0].getInt("task");
            String command = parameters[0].getString("command");
            switch(task) {
                case 1: highlightCommand(command); break;
            }
        }
    }

    private class Countdown extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... parameters) {
            for (int i = 3; i > 0; i--) {
                publishProgress(i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e("Error", "Interrupted!");
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... parameters) {
            countdownText.setText("Starting in " + parameters[0]);
        }

        @Override
        protected void onPostExecute(Void parameters) {
            countdownText.setVisibility(View.GONE);
            initializeListeners();
            GameLoop gameLoop = new GameLoop();
            gameLoop.execute();
        }
    }

    private void showCountDown() {
        for (int i = 3; i > 0; i--) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e("Error", "Interrupted!");
            }
        }
        initializeListeners();
        starting = false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch(keyCode) {
            case KeyEvent.KEYCODE_BACK: endGame(); break;
            default: endGame(); break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void decrementIntervalBy(int decrementAmount) {
        interval -= decrementAmount;
        if(interval <= MINIMUM_INTERVAL) {
            interval = MINIMUM_INTERVAL;
        }
    }

    private Bundle setupTaskCommandBundle(String command, int task) {
        Bundle bundle = new Bundle();
        bundle.putString("command", command);
        bundle.putInt("task", task);
        return bundle;
    }

    private void highlightCommand(String command) {
        switch(command) {
            case "Blue": playSound(low1); break;
            case "Red": playSound(low2); break;
            case "Green": playSound(high1); break;
            case "Yellow": playSound(high2); break;
        }
    }

    private void playSound(int sound) {
        if(!muted) {
            soundPool.stop(sound);
            soundPool.play(sound, 1, 1, 1, 0, 1f);
        }
    }

    private int getScore(long startTime, long endTime) {
        commandTimesList.remove(FIRST);
        reactionTime.addAverageTimeToList(startTime, endTime);
        long baseScore = 10;
        long difference = endTime - startTime;
        long bonus = (1000 - difference) / 100;
        if(bonus < 1) {
            bonus = 0;
        }
        return (int) (baseScore + bonus);
    }

    private String getRandomCommand() {
        Random random = new Random();
        while(selection == previous) {
            selection = random.nextInt(4) + 1;
        }
        previous = selection;
        switch (selection) {
            case 1: return "Blue";
            case 2: return "Red";
            case 3: return "Green";
            case 4: return "Yellow";
        }
        return "";
    }

    private void initializeComponents() {
        score = 0;
        interval = 700;
        loaded = false;

        scoreView = (TextView) findViewById(R.id.score);
        red = (ImageButton) findViewById(R.id.button_red);
        blue = (ImageButton) findViewById(R.id.button_blue);
        green = (ImageButton) findViewById(R.id.button_green);
        yellow = (ImageButton) findViewById(R.id.button_yellow);

        countdownText = (TextView) findViewById(R.id.text_countdown);
        countdownText.setVisibility(View.VISIBLE);
        countdown = new Countdown();
        commandsList = new ArrayList<>();
        commandTimesList = new ArrayList<>();

        createSoundPool();
        low1 = soundPool.load(HardcorePlay.this, R.raw.low1, 1);
        low2 = soundPool.load(HardcorePlay.this, R.raw.low2, 1);
        high1 = soundPool.load(HardcorePlay.this, R.raw.high1, 1);
        high2 = soundPool.load(HardcorePlay.this, R.raw.high2, 1);

        reactionTime = new ReactionTime();
        MobileAds.initialize(getApplicationContext(), String.valueOf(R.string.app_id_for_ads));
        adView = (AdView) findViewById(R.id.adViewHC);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice(String.valueOf(R.string.test_device_id)).build();
        //AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        taps = 0;
        selection = 0;
        previous = 0;
        starting = true;
        running = true;
    }

    public void onBackButtonClick(View view) {
        scoreDialog.dismiss();
        this.finish();
        overridePendingTransition(R.anim.open_activity, R.anim.close_activity);
    }
    public void onResetButtonClicked(View view) {
        scoreDialog.dismiss();
        initializeGame();
    }

    private void initializeListeners() {
        View.OnClickListener blueListener = new View.OnClickListener() {
            public void onClick(View view) {
                if(!checkIfCorrect("Blue")) {
                    endGame();
                }
            }
        };

        View.OnClickListener redListener = new View.OnClickListener() {
            public void onClick(View view) {
                if(!checkIfCorrect("Red")) {
                    endGame();
                }
            }
        };

        View.OnClickListener greenListener = new View.OnClickListener() {
            public void onClick(View view) {
                if(!checkIfCorrect("Green")) {
                    endGame();
                }
            }
        };

        View.OnClickListener yellowListener = new View.OnClickListener() {
            public void onClick(View view) {
                if(!checkIfCorrect("Yellow")) {
                    endGame();
                }
            }
        };

        blue.setOnClickListener(blueListener);
        red.setOnClickListener(redListener);
        green.setOnClickListener(greenListener);
        yellow.setOnClickListener(yellowListener);
    }

    protected void createSoundPool() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            createNewSoundPool();
        } else {
            createOldSoundPool();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void createNewSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();
    }

    @SuppressWarnings("deprecation")
    protected void createOldSoundPool() {
        soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
    }

    protected void createScoreDialog() {
        scoreDialog = new Dialog(HardcorePlay.this);
        scoreDialog.setContentView(R.layout.score_dialog);
        scoreDialog.setTitle("Game Over");

        scoreResult = (TextView) scoreDialog.findViewById(R.id.score_result);
        highscoreResult = (TextView) scoreDialog.findViewById(R.id.highscore_result);
        tapCount = (TextView) scoreDialog.findViewById(R.id.tap_count);
        averageTime = (TextView) scoreDialog.findViewById(R.id.reaction_time);

        scoreDialog.show();
    }

    public static class SoundDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();

            final View dialogView = inflater.inflate(R.layout.hc_sound_dialog, null);
            builder.setView(dialogView);
            return builder.create();
        }
    }

    public void onNoMadmanClicked(View view) {
        muted = false;
        editor.putBoolean("Muted", false);
        editor.apply();
        soundDialog.dismiss();
        initializeGame();
    }

    public void onYesMadmanClicked(View view) {
        madman = true;
        soundDialog.dismiss();
        initializeGame();
    }
}
/*
class ColorListener extends View.OnClickListener {

    String color;
    public ColorListener(String color) {
        this.color=color;
    }

    @Override
    public void onClick(View v) {
        if(!checkIfCorrect("Yellow")) {
            endGame();
        }
    }
}*/