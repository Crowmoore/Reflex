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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static fi.crowmoore.reflextester.OptionsActivity.PREFERENCES;

public class HardcorePlay extends AppCompatActivity implements View.OnClickListener {

    private boolean explicitSignOut = false;

    private int score;
    private ImageButton red;
    private ImageButton blue;
    private ImageButton green;
    private ImageButton yellow;
    private TextView scoreView;
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
    private int sound1;
    private int sound2;
    private int sound3;
    private int sound4;
    private AdView adView;
    private ReactionTime reactionTime;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    private AchievementManager achievementManager;
    private GameOverDialogFragment gameOverDialog;
    private Countdown countdown;
    private boolean muted;
    private int taps;
    private final int FIRST = 0;
    private final int DECREMENT_AMOUNT = 3;
    private final int MINIMUM_INTERVAL = 300;

    private Reflex reflex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hardcore_play);

        reflex = (Reflex) getApplicationContext();

        settings = getBaseContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        muted = settings.getBoolean("Muted", false);
        editor = settings.edit();

        initializeGame();
    }

    public void initializeGame() {
        initializeComponents();

        if(!explicitSignOut && reflex.getManager() == null) {
            reflex.setManager(this);
            achievementManager = new AchievementManager(reflex.getManager().getApiClient());
            startGame();
        } else if(reflex.getManager() != null && reflex.getManager().isConnected()) {
            reflex.getManager().setActivity(this);
            achievementManager = new AchievementManager(reflex.getManager().getApiClient());
            startGame();
        } else {
            startGame();
        }
    }

    public void startGame() {
        touchEventsAllowed(false);
        countdown.execute();
    }

    public void onPause() {
        super.onPause();
        reflex.getMusicManager().pause();
        endGame();
        gameOverDialog.dismiss();
        finish();
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
            if(reflex.getManager() != null && reflex.getManager().isConnected() && achievementManager != null) {
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
        if(score >= 1000) {
            achievementManager.unlockAchievement(getString(R.string.achievement_are_you_cheating));
        }
        if(score >= 5000) {
            achievementManager.unlockAchievement(getString(R.string.achievement_reported));
        }
        if(score >= 10000) {
            achievementManager.unlockAchievement(getString(R.string.achievement_ok_im_out));
        }
    }

    protected void endGame() {
        running = false;
        if(soundPool != null) {
            soundPool.release();
        }
        HighscoreManager highscore = new HighscoreManager(getBaseContext(), score, "Hardcore");
        boolean newHighscore = highscore.isHighscore();
        if(reflex.getManager() != null && reflex.getManager().isConnected()) {
            Games.Leaderboards.submitScore(reflex.getManager().getApiClient(), getString(R.string.leaderboard_hardcore_mode), score);
        }
        int currentHighscore = highscore.getHighscore();
        incrementTimesPlayed();
        gatherStatistics();
        float average = reactionTime.getAverageReactionTime();
        createGameOverDialog(score, currentHighscore, taps, average);
    }

    private void gatherStatistics() {
        int lifetimeTaps = settings.getInt("TapCount", 0) + taps;
        Log.d("stats", "Lifetime taps " + lifetimeTaps);
        editor.putInt("TapCount", lifetimeTaps);
        editor.apply();

        float averageReactionTime = settings.getFloat("ReactionTimeHardcore", 0);
        if (averageReactionTime == 0) {
            editor.putFloat("ReactionTimeHardcore", reactionTime.getAverageReactionTime());
            editor.apply();
        } else {
            float average = (averageReactionTime + reactionTime.getAverageReactionTime()) / 2;
            editor.putFloat("ReactionTimeHardcore", average);
            editor.apply();
            Log.d("stats", "Hardcore Average" + average);
        }
    }

    private void incrementTimesPlayed() {
        int timesPlayed = settings.getInt("TimesPlayedHardcore", 0);
        timesPlayed = timesPlayed + 1;
        editor.putInt("TimesPlayedHardcore", timesPlayed);
        editor.apply();
        if(reflex.getManager() != null && reflex.getManager().isConnected() && achievementManager != null) {
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

    @Override
    public void onResume() {
        super.onResume();
        reflex.getMusicManager().play();
        if(!explicitSignOut) {
            reflex.getManager().setActivity(this);
        }
    }

    private class GameLoop extends AsyncTask<Void, Bundle, Void> {
        @Override
        protected Void doInBackground(Void... parameters) {
            while(running) {
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
            touchEventsAllowed(true);
            GameLoop gameLoop = new GameLoop();
            gameLoop.execute();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch(keyCode) {
            case KeyEvent.KEYCODE_BACK: endGame(); break;
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
            case "Blue": playSound(sound1); break;
            case "Red": playSound(sound2); break;
            case "Green": playSound(sound3); break;
            case "Yellow": playSound(sound4); break;
        }
    }

    private void playSound(int sound) {
        if(!muted) {
            soundPool.stop(sound);
            soundPool.play(sound, 1, 1, 1, 0, 1f);
        }
    }

    private int getScore(long startTime, long endTime) {
        reactionTime.addAverageTimeToList(startTime, endTime);
        commandTimesList.remove(FIRST);
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
        scoreView.setText("0");
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
        initializeSoundPool();

        reactionTime = new ReactionTime();
        MobileAds.initialize(getApplicationContext(), String.valueOf(R.string.app_id_for_ads));
        adView = (AdView) findViewById(R.id.adViewHC);
        //AdRequest adRequest = new AdRequest.Builder().addTestDevice(String.valueOf(R.string.test_device_id)).build();
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        taps = 0;
        selection = 0;
        previous = 0;
        running = true;
    }

    private void initializeSoundPool() {
        String soundset = settings.getString("Soundset", "Frequencies");
        switch(soundset) {
            case "Frequencies": sound1 = soundPool.load(HardcorePlay.this, R.raw.low1, 1);
                                sound2 = soundPool.load(HardcorePlay.this, R.raw.low2, 1);
                                sound3 = soundPool.load(HardcorePlay.this, R.raw.high1, 1);
                                sound4 = soundPool.load(HardcorePlay.this, R.raw.high2, 1); break;
            case "Farm Animals":sound1 = soundPool.load(HardcorePlay.this, R.raw.cat, 1);
                                sound2 = soundPool.load(HardcorePlay.this, R.raw.dog, 1);
                                sound3 = soundPool.load(HardcorePlay.this, R.raw.cow, 1);
                                sound4 = soundPool.load(HardcorePlay.this, R.raw.sheep, 1); break;
            case "Vinyl Drums": sound1 = soundPool.load(HardcorePlay.this, R.raw.vinyl1, 1);
                                sound2 = soundPool.load(HardcorePlay.this, R.raw.vinyl2, 1);
                                sound3 = soundPool.load(HardcorePlay.this, R.raw.vinyl3, 1);
                                sound4 = soundPool.load(HardcorePlay.this, R.raw.vinyl4, 1); break;
            case "Grand Piano": sound1 = soundPool.load(HardcorePlay.this, R.raw.piano1, 1);
                                sound2 = soundPool.load(HardcorePlay.this, R.raw.piano2, 1);
                                sound3 = soundPool.load(HardcorePlay.this, R.raw.piano3, 1);
                                sound4 = soundPool.load(HardcorePlay.this, R.raw.piano4, 1); break;
        }
    }



    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.back_button: onBackButtonClick(); break;
            case R.id.reset_button: onResetButtonClicked(); break;
        }
    }

    public void onBackButtonClick() {
        gameOverDialog.dismiss();
        this.finish();
        overridePendingTransition(R.anim.open_activity, R.anim.close_activity);
    }
    public void onResetButtonClicked() {
        gameOverDialog.dismiss();
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

    protected void createGameOverDialog(int score, int highscore, int taps, float reaction) {
        Bundle bundle = new Bundle();
        bundle.putInt("score", score);
        bundle.putInt("highscore", highscore);
        bundle.putInt("taps", taps);
        bundle.putFloat("reaction", reaction);
        gameOverDialog = GameOverDialogFragment.getNewDialogInstance(bundle);
        gameOverDialog.show(getFragmentManager(), null);
        getFragmentManager().executePendingTransactions();
        gameOverDialog.getDialog().findViewById(R.id.reset_button).setOnClickListener(this);
        gameOverDialog.getDialog().findViewById(R.id.back_button).setOnClickListener(this);
    }

    protected void touchEventsAllowed(boolean value) {
        switch(String.valueOf(value)) {
            case "true": getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE); break;
            case "false": getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE); break;
        }
    }
}