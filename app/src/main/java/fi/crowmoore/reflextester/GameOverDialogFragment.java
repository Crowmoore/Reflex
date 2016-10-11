package fi.crowmoore.reflextester;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

/**
 * Created by Crowmoore on 11-Oct-16.
 */

public class GameOverDialogFragment extends DialogFragment {

    static GameOverDialogFragment getNewDialogInstance(Bundle bundle) {
        GameOverDialogFragment dialog = new GameOverDialogFragment();
        dialog.setArguments(bundle);

        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.score_dialog, container, false);
        Bundle bundle = getArguments();
        setGameResults(root, bundle);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if(dialog != null) {
            dialog.getWindow().setLayout(1000, 1400);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().setCancelable(false);
        getDialog().setCanceledOnTouchOutside(false);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
    }

    protected void setGameResults(View view, Bundle bundle) {
        TextView scoreView = (TextView) view.findViewById(R.id.score_result);
        TextView highscoreView = (TextView) view.findViewById(R.id.highscore_result);
        TextView tapsView = (TextView) view.findViewById(R.id.tap_count);
        TextView reactionView = (TextView) view.findViewById(R.id.reaction_time);

        int score = bundle.getInt("score");
        int highscore = bundle.getInt("highscore");
        int taps = bundle.getInt("taps");
        float reaction = bundle.getFloat("reaction");

        scoreView.setText(String.format(Locale.US, "Score: %1$d", score));
        highscoreView.setText(String.format(Locale.US, "Highscore: %1$d", highscore));
        tapsView.setText(String.format(Locale.US, "Taps: %1$d", taps));
        reactionView.setText(String.format(Locale.US, "Average reaction: %.02f sec", reaction));
    }
}
