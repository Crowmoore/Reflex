package fi.crowmoore.reflextester;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Crowmoore on 06-Oct-16.
 */

public class ReactionTime {

    private List<Long> times;

    public ReactionTime() {
        this.times = new ArrayList<>();
    }

    protected void addAverageTimeToList(long start, long end) {
        long average = end - start;
        Log.d("average", "Average: " + average);
        times.add(average);
    }

    protected float getAverageReactionTime() {
        if(times.isEmpty()) {
            return 0;
        }
        long sum = 0;
        for (long time : times) {
            sum += time;
        }
        float average = sum / times.size();
        float averageAsSeconds = average / 1000;
        return averageAsSeconds;
    }
}
