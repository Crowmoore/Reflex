package fi.crowmoore.reflextester;

import android.annotation.TargetApi;
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
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static fi.crowmoore.reflextester.OptionsActivity.PREFERENCES;

public class RegularPlay extends AppCompatActivity {

    private int score;
    private ImageButton red;
    private ImageButton blue;
    private ImageButton green;
    private ImageButton yellow;
    private TextView scoreView;
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
    private int volume;
    private final int FIRST = 0;
    private final int DECREMENT_AMOUNT = 5;
    private final int MINIMUM_INTERVAL = 350;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regular_play);

        initializeComponents();

        GameLoop gameLoop = new GameLoop();
        gameLoop.execute();
    }

    public boolean checkIfCorrect(String command) {
      if(command.equals(commandsList.get(FIRST))) {
          commandsList.remove(0);
          score += 10;
          scoreView.setText(String.valueOf(score));
          return true;
      } else {
          return false;
      }
    }

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

    protected void endGame() {
        running = false;
        soundPool.release();
        finish();
    }

    private class GameLoop extends AsyncTask<Void, Bundle, Void> {
        @Override
        protected Void doInBackground(Void... parameters) {
            while(running) {
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            endGame();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void decrementIntervalBy(int decrementAmount) {
        interval -= decrementAmount;
        if(interval <= MINIMUM_INTERVAL) {
            interval = MINIMUM_INTERVAL;
        }
    }

    public Bundle setupTaskCommandBundle(String command, int task) {
        Bundle bundle = new Bundle();
        bundle.putString("command", command);
        bundle.putInt("task", task);
        return bundle;
    }

    public void removeHighlight(String command) {
        switch(command) {
            case "Blue": blue.setImageResource(R.drawable.blue_off); break;
            case "Red": red.setImageResource(R.drawable.red_off); break;
            case "Green": green.setImageResource(R.drawable.green_off); break;
            case "Yellow": yellow.setImageResource(R.drawable.yellow_off); break;
        }
    }

    public void highlightCommand(String command) {
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

    public void playSound(int sound) {
        soundPool.stop(sound);
        soundPool.play(sound, volume, volume, 1, 0, 1f);
    }

    public String getRandomCommand() {
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
        volume = settings.getInt("Volume", 50);

        createSoundPool();
        low1 = soundPool.load(RegularPlay.this, R.raw.low1, 1);
        low2 = soundPool.load(RegularPlay.this, R.raw.low2, 1);
        high1 = soundPool.load(RegularPlay.this, R.raw.high1, 1);
        high2 = soundPool.load(RegularPlay.this, R.raw.high2, 1);

        blue.setOnClickListener(blueListener);
        red.setOnClickListener(redListener);
        green.setOnClickListener(greenListener);
        yellow.setOnClickListener(yellowListener);

        selection = 0;
        previous = 0;
        running = true;
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
}
