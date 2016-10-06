package fi.crowmoore.reflextester;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Crowmoore on 06-Oct-16.
 */

public class ReactionTime {

    private List<Long> averages;

    public ReactionTime() {
        this.averages = new ArrayList<>();
    }

    protected void addAverageTimeToList(long start, long end) {
        long average = end - start;
        Log.d("average", "Average: " + average);
        averages.add(average);
    }

    protected String getAverageReactionTime() {
        if(averages.isEmpty()) {
            return "Average reaction time: 0";
        }
        long sum = 0;
        for (long time : averages) {
            sum += time;
        }
        float average = sum / averages.size();
        float averageAsSeconds = average / 1000;
        String result = String.format(Locale.US, "Average reaction time: %.02f sec", averageAsSeconds);
        return result;
    }
}
