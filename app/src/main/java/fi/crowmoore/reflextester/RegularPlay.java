package fi.crowmoore.reflextester;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
import java.util.Random;

import static fi.crowmoore.reflextester.MainActivity.RC_SIGN_IN;
import static fi.crowmoore.reflextester.OptionsActivity.PREFERENCES;

public class RegularPlay extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
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
    private TextView leaderboardRank;
    private List<String> commandsList = new ArrayList<>();
    private boolean running;
    private long interval;
    private int selection;
    private int previous;
    private SoundPool soundPool;
    private boolean loaded;
    private int low1;
    private int low2;
    private int high1;
    private int high2;
    private boolean muted;
    private boolean starting;
    private Dialog scoreDialog;
    private SharedPreferences.Editor editor;
    private AchievementManager achievementManager;
    private AdView adView;
    private final int FIRST = 0;
    private final int DECREMENT_AMOUNT = 5;
    private final int MINIMUM_INTERVAL = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regular_play);

        initializeComponents();

        GameLoop gameLoop = new GameLoop();
        gameLoop.execute();
    }

    protected void onStart() {
        super.onStart();
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Games.API)
                .build();
        if (!signInFlow && !explicitSignOut) {
            googleApiClient.connect();
            achievementManager = new AchievementManager(this, googleApiClient);
            Log.d("tag", "GoogleAPIClient connection called");
            if(googleApiClient.isConnected()) {
                Log.d("tag", "GoogleAPIClient connected");
            } else {
                Log.d("tag", "GoogleAPIClient not connected");
            }
        }
    }
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("tag", "GoogleAPIClient Connected");
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
    }

    private boolean checkIfCorrect(String command) {
        if(commandsList.isEmpty()) {
            return false;
        }
        if(command.equals(commandsList.get(FIRST))) {
          commandsList.remove(0);
          score += 10;
          scoreView.setText(String.valueOf(score));
          checkScoreForAchievement(score);
          return true;
        }
        return false;
    }

    private void checkScoreForAchievement(int score) {
        if(score >= 300) {
            achievementManager.unlockAchievement("Baby's first steps", "CgkI1sfZypEcEAIQAQ");
        }
        if(score >= 1000) {
            achievementManager.unlockAchievement("Getting the hang of it", "CgkI1sfZypEcEAIQAg");
        }
        if(score >= 3000) {
            achievementManager.unlockAchievement("Master of focus", "CgkI1sfZypEcEAIQAw");
        }
        if(score >= 5000) {
            achievementManager.unlockAchievement("Insane reflexes", "CgkI1sfZypEcEAIQBA");
        }
    }

    protected void endGame() {
        running = false;
        soundPool.release();
        HighscoreManager highscore = new HighscoreManager(getBaseContext(), score, "Regular");
        boolean newHighscore = highscore.isHighscore();
        Games.Leaderboards.submitScore(googleApiClient, getString(R.string.leaderboard_regular_id), score);
        int currentHighscore = highscore.getHighscore();
        createScoreDialog();
        loadPlayerRank();
        scoreResult.setText("Score: " + score);
        highscoreResult.setText("Highscore: " + currentHighscore);
    }

    private void loadPlayerRank() {
        try {
            Games.Leaderboards.loadCurrentPlayerLeaderboardScore(googleApiClient, getString(R.string.leaderboard_hardcore_id), LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC).setResultCallback(new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {
                @Override
                public void onResult(final Leaderboards.LoadPlayerScoreResult scoreResult) {
                    LeaderboardScore lbs = scoreResult.getScore();
                    String rank = lbs.getDisplayRank();
                    if(rank != null) {
                        leaderboardRank.setText("Leaderboard rank: " + lbs.getDisplayRank());
                    } else {
                        leaderboardRank.setText("Could not retrieve leaderboard rank");
                    }
                }
            });
        } catch (Exception e) {
            leaderboardRank.setText("Could not retrieve leaderboard rank");
        }
    }

    private class GameLoop extends AsyncTask<Void, Bundle, Void> {
        @Override
        protected Void doInBackground(Void... parameters) {
            while(running) {
                if(starting) { showCountDown(); }
                String command = getRandomCommand();
                commandsList.add(command);
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

    private void showCountDown() {
        for (int i = 2; i > 0; i--) {
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
            case KeyEvent.KEYCODE_HOME: endGame(); break;
            case KeyEvent.KEYCODE_MENU: endGame(); break;
            case KeyEvent.KEYCODE_POWER: endGame(); break;
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
            case "Blue": blue.setImageResource(R.drawable.blue_off); break;
            case "Red": red.setImageResource(R.drawable.red_off); break;
            case "Green": green.setImageResource(R.drawable.green_off); break;
            case "Yellow": yellow.setImageResource(R.drawable.yellow_off); break;
        }
    }

    private void highlightCommand(String command) {
        switch(command) {
            case "Blue": blue.setImageResource(R.drawable.blue_on);
                         playSound(low1);
                         break;
            case "Red": red.setImageResource(R.drawable.red_on);
                        playSound(low2);
                        break;
            case "Green": green.setImageResource(R.drawable.green_on);
                          playSound(high1);
                          break;
            case "Yellow": yellow.setImageResource(R.drawable.yellow_on);
                           playSound(high2);
                           break;
        }
    }

    private void playSound(int sound) {
        if(!muted) {
            soundPool.stop(sound);
            soundPool.play(sound, 1, 1, 1, 0, 1f);
        }
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

        final SharedPreferences settings = getBaseContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        muted = settings.getBoolean("Muted", false);
        editor = settings.edit();

        createSoundPool();
        low1 = soundPool.load(RegularPlay.this, R.raw.low1, 1);
        low2 = soundPool.load(RegularPlay.this, R.raw.low2, 1);
        high1 = soundPool.load(RegularPlay.this, R.raw.high1, 1);
        high2 = soundPool.load(RegularPlay.this, R.raw.high2, 1);

        MobileAds.initialize(getApplicationContext(), String.valueOf(R.string.app_id_for_ads));
        adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice(String.valueOf(R.string.test_device_id)).build();
        //AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        selection = 0;
        previous = 0;
        starting = true;
        running = true;
    }

    public void onBackButtonClick(View view) {
        scoreDialog.dismiss();
        finish();
    }
    public void onResetButtonClicked(View view) {
        scoreDialog.dismiss();
        RegularPlay.this.recreate();
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
        scoreDialog = new Dialog(RegularPlay.this);
        scoreDialog.setContentView(R.layout.score_dialog);
        scoreDialog.setTitle("Game Over");

        scoreResult = (TextView) scoreDialog.findViewById(R.id.score_result);
        highscoreResult = (TextView) scoreDialog.findViewById(R.id.highscore_result);
        leaderboardRank = (TextView) scoreDialog.findViewById(R.id.leaderboard_rank);

        scoreDialog.show();
    }
}
