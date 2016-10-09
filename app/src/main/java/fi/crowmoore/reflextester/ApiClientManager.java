package fi.crowmoore.reflextester;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.data.Application;
import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameUtils;

import static fi.crowmoore.reflextester.MainActivity.RC_SIGN_IN;

/**
 * Created by Crowmoore on 09-Oct-16.
 */

public class ApiClientManager implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient client;
    private Context context;
    private Activity activity;

    public ApiClientManager(Context context) {
        this.context = context;
        buildApiClient();
        connect();
        Log.d("pylly", "Client created");
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public GoogleApiClient getApiClient() {
        Log.d("pylly", "returning client");
        return this.client;
    }

    public void connect() {
        if(client != null) {
            client.connect();
            Log.d("pylly", String.valueOf(client.isConnected()));
        } else {
            buildApiClient();
            client.connect();
        }
    }

    public void disconnect() {
        if(client != null && client.isConnected()) {
            client.disconnect();
            Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show();
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
        Log.d("pylly", client.toString());
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("pylly", "ApiClient connected");
        Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d("pylly", "ApiClient connection suspended by cause " + cause);
        client.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d("pylly", "ApiClient connection failed: " + result.toString());
//        Intent signInIntent = Games.getSignInIntent(client);
//        startActivityForResult(signInIntent, RC_SIGN_IN);
//        if(resolvingConnectionFailure) {
//            return;
//        }
//        if(signInClicked || autoStartSignInFlow) {
//            autoStartSignInFlow = false;
//            signInClicked = false;
//            resolvingConnectionFailure = true;
//
//            if(!BaseGameUtils.resolveConnectionFailure(this,
//                    client, result,
//                    RC_SIGN_IN, String.valueOf(R.string.sign_in_error))) {
//                resolvingConnectionFailure = false;
//            }
//        }
    }
}
