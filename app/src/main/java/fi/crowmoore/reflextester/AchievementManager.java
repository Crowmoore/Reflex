package fi.crowmoore.reflextester;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

import static fi.crowmoore.reflextester.OptionsActivity.PREFERENCES;

/**
 * Created by Crowmoore on 01-Oct-16.
 */

public class AchievementManager {

    private GoogleApiClient client;
    private SharedPreferences.Editor editor;

    public AchievementManager(Context context, GoogleApiClient client) {
        this.client = client;

        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    public void unlockAchievement(String achievement, String id) {
        Games.Achievements.unlock(client, id);
        editor.putBoolean(achievement, true);
        editor.apply();
    }
}
