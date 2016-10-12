package fi.crowmoore.reflextester;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameUtils;

import static fi.crowmoore.reflextester.MainActivity.RC_SIGN_IN;

/**
 * Created by Crowmoore on 09-Oct-16.
 */

public class ApiClientManager implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient client;
    private Activity context;

    private boolean signInClicked = false;
    private boolean resolvingConnectionFailure = false;
    private boolean autoStartSignInFlow = true;

    public ApiClientManager(Activity context) {
        this.context = context;
        buildApiClient();
        connect();
        Log.d("APIClient", "Client created");
    }

    public void setActivity(Activity context) {
        this.context = context;
    }

    public GoogleApiClient getApiClient() {
        Log.d("APIClient", "returning client");
        return this.client;
    }

    public void connect() {
        if(client != null) {
            client.connect();
            Log.d("APIClient", String.valueOf(client.isConnected()));
        } else {
            buildApiClient();
            client.connect();
        }
    }

    public void disconnect() {
        if(client != null && client.isConnected()) {
            client.disconnect();
        }
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    private void buildApiClient() {
        client = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();
        Log.d("APIClient ", client.toString());
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("APIClient", "ApiClient connected");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d("APIClient", "ApiClient connection suspended by cause " + cause);
        client.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d("APIClient", "ApiClient connection failed: " + result.toString());
        if(resolvingConnectionFailure) {
            return;
        }
        if(signInClicked || autoStartSignInFlow) {
            autoStartSignInFlow = false;
            signInClicked = false;
            resolvingConnectionFailure = true;

            if(!BaseGameUtils.resolveConnectionFailure(context,
                    client, result,
                    RC_SIGN_IN, String.valueOf(R.string.sign_in_error))) {
                resolvingConnectionFailure = false;
            }
        }
    }
}
