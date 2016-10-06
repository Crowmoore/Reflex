package fi.crowmoore.reflextester;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

import static fi.crowmoore.reflextester.OptionsActivity.PREFERENCES;

/**
 * Created by Crowmoore on 01-Oct-16.
 */

public class AchievementManager {

    private GoogleApiClient client;

    public AchievementManager(GoogleApiClient client) {
        this.client = client;
    }

    protected void unlockAchievement(String id) {
        Log.d("Achievement", "Unlock achievement " + id);
        Games.Achievements.unlock(client, id);
    }

    protected void incrementAchievement(String id, int amount) {
        Log.d("Achievement", "Increment achievement " + id);
        Games.Achievements.increment(client, id, amount);
    }

    protected Intent getAchievementsIntent() {
        return Games.Achievements.getAchievementsIntent(client);
    }
}
