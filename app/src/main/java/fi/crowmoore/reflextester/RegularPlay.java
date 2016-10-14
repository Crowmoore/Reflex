package fi.crowmoore.reflextester;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
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

public class RegularPlay extends AppCompatActivity implements View.OnClickListener {

    private boolean explicitSignOut = false;

    private int score;
    private AppCompatImageButton red;
    private AppCompatImageButton blue;
    private AppCompatImageButton green;
    private AppCompatImageButton yellow;
    private TextView scoreView;
    private TextView countdownText;
    private List<String> commandsList;
    private List<Long> commandTimesList;
    private boolean running;
    private long interval;
    private int selection;
    private int previous;
    private SoundPool soundPool;
    private boolean loaded;
    private int sound1;
    private int sound2;
    private int sound3;
    private int sound4;
    private boolean muted;
    private Countdown countdown;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    private AchievementManager achievementManager;
    private GameOverDialogFragment gameOverDialog;
    private ReactionTime reactionTime;
    private AdView adView;
    private int taps;
    private final int FIRST = 0;
    private final int DECREMENT_AMOUNT = 5;
    private final int MINIMUM_INTERVAL = 300;

    private Reflex reflex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regular_play);

        reflex = (Reflex) getApplicationContext();

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
            commandsList.remove(FIRST);
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
        if(score >= 300) {
            achievementManager.unlockAchievement(getString(R.string.achievement_babys_first_steps));
        }
        if(score >= 1000) {
            achievementManager.unlockAchievement(getString(R.string.achievement_getting_the_hang_of_it));
        }
        if(score >= 3000) {
            achievementManager.unlockAchievement(getString(R.string.achievement_master_of_focus));
        }
        if(score >= 5000) {
            achievementManager.unlockAchievement(getString(R.string.achievement_insane_reflexes));
        }
    }

    protected void endGame() {
        running = false;
        if(soundPool != null) {
            soundPool.release();
        }
        HighscoreManager highscore = new HighscoreManager(getBaseContext(), score, "Regular");
        boolean newHighscore = highscore.isHighscore();
        if(reflex.getManager() != null && reflex.getManager().isConnected()) {
            Games.Leaderboards.submitScore(reflex.getManager().getApiClient(), getString(R.string.leaderboard_regular_mode), score);
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

        float averageReactionTime = settings.getFloat("ReactionTime", 0);
        if(averageReactionTime == 0) {
            editor.putFloat("ReactionTime", reactionTime.getAverageReactionTime());
            editor.apply();
        } else {
            float average = (averageReactionTime + reactionTime.getAverageReactionTime()) / 2;
            editor.putFloat("ReactionTime", average);
            editor.apply();
            Log.d("stats", "Average " + average);
        }
    }

    private void incrementTimesPlayed() {
        int timesPlayed = settings.getInt("TimesPlayedRegular", 0);
        timesPlayed = timesPlayed + 1;
        editor.putInt("TimesPlayedRegular", timesPlayed);
        editor.apply();
        if(reflex.getManager() != null && reflex.getManager().isConnected() && achievementManager != null) {
            incrementAchievements();
        }
    }

    private void incrementAchievements() {
        achievementManager.incrementAchievement(getString(R.string.achievement_rookie), 1);
        achievementManager.incrementAchievement(getString(R.string.achievement_seasoned), 1);
        achievementManager.incrementAchievement(getString(R.string.achievement_senior), 1);
        achievementManager.incrementAchievement(getString(R.string.achievement_expert), 1);
        achievementManager.incrementAchievement(getString(R.string.achievement_grandmaster), 1);
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
                bundle = setupTaskCommandBundle(command, 0);
                publishProgress(bundle);
            }
            return null;
        }

        protected void onProgressUpdate(Bundle... parameters) {
            int task = parameters[0].getInt("task");
            String command = parameters[0].getString("command");
            switch(task) {
                case 0: removeHighlight(command); break;
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

    private void removeHighlight(String command) {
        switch(command) {
            case "Blue": blue.setImageResource(R.drawable.ic_blue_off); break;
            case "Red": red.setImageResource(R.drawable.ic_red_off); break;
            case "Green": green.setImageResource(R.drawable.ic_green_off); break;
            case "Yellow": yellow.setImageResource(R.drawable.ic_yellow_off); break;
        }
    }

    private void highlightCommand(String command) {
        switch(command) {
            case "Blue": blue.setImageResource(R.drawable.ic_blue_on);
                         playSound(sound1);
                         break;
            case "Red": red.setImageResource(R.drawable.ic_red_on);
                        playSound(sound2);
                        break;
            case "Green": green.setImageResource(R.drawable.ic_green_on);
                          playSound(sound3);
                          break;
            case "Yellow": yellow.setImageResource(R.drawable.ic_yellow_on);
                           playSound(sound4);
                           break;
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
        red = (AppCompatImageButton) findViewById(R.id.button_red);
        blue = (AppCompatImageButton) findViewById(R.id.button_blue);
        green = (AppCompatImageButton) findViewById(R.id.button_green);
        yellow = (AppCompatImageButton) findViewById(R.id.button_yellow);

        countdownText = (TextView) findViewById(R.id.text_countdown);
        countdownText.setVisibility(View.VISIBLE);
        countdown = new Countdown();
        commandsList = new ArrayList<>();
        commandTimesList = new ArrayList<>();

        settings = getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        muted = settings.getBoolean("Muted", false);
        explicitSignOut = settings.getBoolean("ExplicitSignOut", false);
        editor = settings.edit();
        reactionTime = new ReactionTime();

        createSoundPool();
        initializeSoundPool();

        MobileAds.initialize(getApplicationContext(), String.valueOf(R.string.app_id_for_ads));
        adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice(String.valueOf(R.string.test_device_id)).build();
        //AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        taps = 0;
        selection = 0;
        previous = 0;
        running = true;
    }

    private void initializeSoundPool() {
        String soundset = settings.getString("Soundset", "Frequencies");
        switch(soundset) {
            case "Frequencies": sound1 = soundPool.load(RegularPlay.this, R.raw.low1, 1);
                                sound2 = soundPool.load(RegularPlay.this, R.raw.low2, 1);
                                sound3 = soundPool.load(RegularPlay.this, R.raw.high1, 1);
                                sound4 = soundPool.load(RegularPlay.this, R.raw.high2, 1); break;
            case "Farm Animals":sound1 = soundPool.load(RegularPlay.this, R.raw.cat, 1);
                                sound2 = soundPool.load(RegularPlay.this, R.raw.dog, 1);
                                sound3 = soundPool.load(RegularPlay.this, R.raw.cow, 1);
                                sound4 = soundPool.load(RegularPlay.this, R.raw.sheep, 1); break;
            case "Vinyl Drums": sound1 = soundPool.load(RegularPlay.this, R.raw.vinyl1, 1);
                                sound2 = soundPool.load(RegularPlay.this, R.raw.vinyl2, 1);
                                sound3 = soundPool.load(RegularPlay.this, R.raw.vinyl3, 1);
                                sound4 = soundPool.load(RegularPlay.this, R.raw.vinyl4, 1); break;
            case "Grand Piano": sound1 = soundPool.load(RegularPlay.this, R.raw.piano1, 1);
                                sound2 = soundPool.load(RegularPlay.this, R.raw.piano2, 1);
                                sound3 = soundPool.load(RegularPlay.this, R.raw.piano3, 1);
                                sound4 = soundPool.load(RegularPlay.this, R.raw.piano4, 1); break;
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

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.back_button: onBackButtonClick(); break;
            case R.id.reset_button: onResetButtonClicked(); break;
        }
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
