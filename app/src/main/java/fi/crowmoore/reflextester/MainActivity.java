package fi.crowmoore.reflextester;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onRegularPlayClick(View view) {
        startActivity(new Intent(MainActivity.this, RegularPlay.class));
    }

    public void onOptionsClick(View view) {
        startActivity(new Intent(MainActivity.this, OptionsActivity.class));
    }
}
