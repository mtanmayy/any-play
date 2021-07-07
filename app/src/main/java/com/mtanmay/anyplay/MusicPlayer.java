package com.mtanmay.anyplay;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

/**
 * Maintains a single instance instance of the media player used to play audio files
 */
public class MusicPlayer {

    // Singleton class

    private static final String TAG = "MusicPlayer";

    private static MediaPlayer instance = null;

    public static MediaPlayer getInstance(Context context) {

        if(instance == null) {
            Log.d(TAG, "getInstance: PLAYER INSTANCE: null | CREATING NEW PLAYER");
            instance = new MediaPlayer();
        }
        return instance;
    }

}
