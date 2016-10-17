package fi.crowmoore.reflextester;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import java.sql.Ref;

/**
 * Created by Crowmoore on 09-Oct-16.
 */
public class Reflex extends MultiDexApplication {

    private ApiClientManager manager = null;
    private MusicManager musicManager = null;

    public void setManager(Activity context) {
        manager = new ApiClientManager(context);
    }

    public void setMusicManager(Activity context) {
        musicManager = new MusicManager(context);
    }

    public MusicManager getMusicManager() {
        return musicManager;
    }

    public ApiClientManager getManager() {
        return manager;
    }
}
