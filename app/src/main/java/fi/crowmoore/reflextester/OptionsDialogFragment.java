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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.SignInButton;

import static fi.crowmoore.reflextester.OptionsActivity.PREFERENCES;

/**
 * Created by Crowmoore on 11-Oct-16.
 */

public class OptionsDialogFragment extends DialogFragment implements AdapterView.OnItemSelectedListener {

    private SharedPreferences.Editor editor;
    private CheckBox muteCheckBox;
    private boolean explicitSignOut;
    private String soundset;
    private SignInButton signInButton;
    private Button signOutButton;
    private TextView signInInfo;
    private Spinner spinner;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.options_dialog, container, false);
        muteCheckBox = (CheckBox) root.findViewById(R.id.mute_sound);
        spinner = (Spinner) root.findViewById(R.id.soundset_spinner);
        signInButton = (SignInButton) root.findViewById(R.id.sign_in_button);
        signOutButton = (Button) root.findViewById(R.id.sign_out_button);
        signInInfo = (TextView) root.findViewById(R.id.info);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if(dialog != null) {
            dialog.getWindow().setLayout(1000, 1200);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        SharedPreferences settings = getActivity().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        editor = settings.edit();

        explicitSignOut = settings.getBoolean("ExplicitSignOut", false);
        soundset = settings.getString("Soundset", "Frequencies");

        boolean muted = settings.getBoolean("Muted", false);
        muteCheckBox.setChecked(muted);
        muteCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                editor.putBoolean("Muted", button.isChecked());
                editor.apply();
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.soundsets, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setSelection(adapter.getPosition(soundset));
        spinner.setOnItemSelectedListener(this);

        if(!explicitSignOut) {
            signInButton.setVisibility(View.GONE);
            signOutButton.setVisibility(View.VISIBLE);
            signInInfo.setText(R.string.signed_in);
        } else {
            signInButton.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.GONE);
            signInInfo.setText(R.string.not_signed_in);
        }

        getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String selection = parent.getItemAtPosition(position).toString();
        editor.putString("Soundset", selection);
        editor.apply();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //
    }
}
