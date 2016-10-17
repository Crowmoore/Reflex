package fi.crowmoore.reflextester;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Locale;

import static fi.crowmoore.reflextester.OptionsActivity.PREFERENCES;

/**
 * Created by Crowmoore on 11-Oct-16.
 */

public class StatsDialogFragment extends DialogFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.stats_dialog, container, false);
        setStats(root);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if(dialog != null) {
            //dialog.getWindow().setLayout(1000, 1400);
        }
    }

    private void setStats(View view) {
        SharedPreferences preferences = getActivity().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        TextView regularScore = (TextView) view.findViewById(R.id.highscore_regular);
        TextView hardcoreScore = (TextView) view.findViewById(R.id.highscore_hardcore);
        TextView regularGames = (TextView) view.findViewById(R.id.games_regular);
        TextView hardcoreGames = (TextView) view.findViewById(R.id.games_hardcore);
        TextView regularReaction = (TextView) view.findViewById(R.id.reaction_regular);
        TextView hardcoreReaction = (TextView) view.findViewById(R.id.reaction_hardcore);
        TextView taps = (TextView) view.findViewById(R.id.taps_total);

        regularScore.setText("Regular highscore: " + preferences.getInt("RegularHighscore", 0));
        hardcoreScore.setText("Hardcore highscore: " + preferences.getInt("HardcoreHighscore", 0));
        regularGames.setText("Regular games played: " + preferences.getInt("TimesPlayedRegular", 0));
        hardcoreGames.setText("Hardcore games played: " + preferences.getInt("TimesPlayedHardcore", 0));
        regularReaction.setText(String.format(Locale.US, "Regular reaction: %.02f sec", preferences.getFloat("ReactionTime", 0)));
        hardcoreReaction.setText(String.format(Locale.US, "Hardcore reaction: %.02f sec", preferences.getFloat("ReactionTimeHardcore", 0)));
        taps.setText("Total taps: " + preferences.getInt("TapCount", 0));
    }
}
