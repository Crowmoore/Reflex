package fi.crowmoore.reflextester;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.sql.Ref;

/**
 * Created by Crowmoore on 09-Oct-16.
 */
public class Reflex extends Application {

    private ApiClientManager manager = null;

    public void setManager(Activity context) {
        manager = new ApiClientManager(context);
    }

    public ApiClientManager getManager() {
        return manager;
    }
}
