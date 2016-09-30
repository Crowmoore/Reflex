package fi.crowmoore.reflextester;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import static fi.crowmoore.reflextester.OptionsActivity.PREFERENCES;

/**
 * Created by Crowmoore on 30-Sep-16.
 */

public class HighscoreManager {

    private int score;
    private Context context;
    private String mode;
    SharedPreferences preferences;

    public HighscoreManager(Context context, int score, String mode) {
        this.score = score;
        this.context = context;
        this.mode = mode;

        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    private int getPreviousBest() {
        switch (mode) {
            case "Regular": return preferences.getInt("RegularHighscore", 0);
            case "Hardcore": return preferences.getInt("HardcoreHighscore", 0);
        }
        return 0;
    }

    protected boolean newHighscore() {
        int previousBest = getPreviousBest();
        if(score > previousBest) {
            SharedPreferences.Editor editor = preferences.edit();
            switch (mode) {
                case "Regular": editor.putInt("RegularHighscore", score);
                                editor.apply();
                                break;
                case "Hardcore": editor.putInt("HardcoreHighscore", score);
                                 editor.apply();
                                 break;
            }
            return true;
        } else return false;
    }
}
