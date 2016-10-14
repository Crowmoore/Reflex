package fi.crowmoore.reflextester;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static fi.crowmoore.reflextester.OptionsActivity.PREFERENCES;

/**
 * Created by Crowmoore on 13-Oct-16.
 */

public class MusicManager implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private ArrayList<Integer> playlist;
    private Map<Integer, String> songDictionary;
    private SharedPreferences settings;
    private Activity context;
    private MediaPlayer player;
    private int currentSong = 0;
    private TextView songName;

    public MusicManager(Activity context) {
        this.context = context;
        songName = (TextView) context.findViewById(R.id.song_name);
        context.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        settings = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        pairIDsToNames();
        createShuffledPlaylist();

        createPlayer();
    }

    private void createPlayer() {
        player = MediaPlayer.create(context, playlist.get(currentSong));
        float volume = retrieveUserSetVolume();
        player.setVolume(volume, volume);
        player.setOnCompletionListener(this);
    }

    public float retrieveUserSetVolume() {
        return settings.getFloat("Volume", 0.5f);
    }

    public void play() {
        player.start();
        songName.setText(getSong());
    }

    public void setVolume(float volume) {
        player.setVolume(volume, volume);
    }

    public void pause() {
        player.pause();
    }

    public String getSong() {
        return songDictionary.get(playlist.get(currentSong));
    }

    public ArrayList getPlaylist() {
        return playlist;
    }

    public Map<Integer, String> getSongDictionary() {
        return songDictionary;
    }

    private void pairIDsToNames() {
        songDictionary = new HashMap<>();
        songDictionary.put(context.getResources().getIdentifier("keep_on_dreaming", "raw", context.getPackageName()), "A Drop A Day - Keep On Dreaming");
        songDictionary.put(context.getResources().getIdentifier("the_shortest_path", "raw", context.getPackageName()), "A Drop A Day - The Shortest Path");
        songDictionary.put(context.getResources().getIdentifier("we_just_started", "raw", context.getPackageName()), "A Drop A Day - We Just Started");

        Log.d("dictionary", songDictionary.toString());
    }

    private void createShuffledPlaylist() {
        playlist = new ArrayList<>();
        playlist.add(R.raw.keep_on_dreaming);
        playlist.add(R.raw.the_shortest_path);
        playlist.add(R.raw.we_just_started);

        Collections.shuffle(playlist);
    }

    @Override
    public void onCompletion(MediaPlayer player) {
        if(currentSong < playlist.size() - 1) {
            currentSong++;
            player.reset();
            try {
                player.setDataSource(context, Uri.parse("android.resource://fi.crowmoore.reflextester/" + playlist.get(currentSong)));
                player.setOnPreparedListener(this);
                player.prepareAsync();
            } catch (IOException e) {
                Log.d("Exception", e.toString());
            }
        } else {
            currentSong = 0;
            player.reset();
            try {
                player.setDataSource(context, Uri.parse("android.resource://fi.crowmoore.reflextester/" + playlist.get(currentSong)));
                player.setOnPreparedListener(this);
                player.prepareAsync();
            } catch (IOException e) {
                Log.d("Exception", e.toString());
            }
        }
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        play();
    }
}
