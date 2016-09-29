package fi.crowmoore.reflextester;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onRegularPlayClick(View view) {
        startActivity(new Intent(MainActivity.this, RegularPlay.class));
    }

    public void onHardcorePlayClick(View view) {
        startActivity(new Intent(MainActivity.this, HardcorePlay.class));
    }

    public void onOptionsClick(View view) {
        startActivity(new Intent(MainActivity.this, OptionsActivity.class));
    }

    @Override
    public void onConnected(Bundle connectionHint) {

    }

    @Override
    public void onConnectionSuspended(int cause) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onClick(View view) {

    }
}
