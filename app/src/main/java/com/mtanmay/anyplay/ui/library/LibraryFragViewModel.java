package com.mtanmay.anyplay.ui.library;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.mtanmay.anyplay.song.Song;
import com.mtanmay.anyplay.services.MusicPlayerService;
import com.mtanmay.anyplay.utils.ToastUtils;

import java.io.File;

import static com.mtanmay.anyplay.Constants.DIR_AUDIO;

public class LibraryFragViewModel extends AndroidViewModel {

    private static final String TAG = "HomeFragViewModel";

    private Intent playIntent;

    public LibraryFragViewModel(@NonNull Application application) {
        super(application);

    }

    @SuppressLint("CheckResult")
    public void playSong(@NonNull Song song) {

        if(songExists(song.getSongId())) {

            playIntent = new Intent(getApplication(), MusicPlayerService.class);
            playIntent.putExtra("EXTRA_SONG_ID", song.getSongId());
            getApplication().startService(playIntent);

        }
        else {
            Log.d(TAG, "playSong: Song not found | UNABLE TO START SERVICE");
            ToastUtils.create(getApplication().getApplicationContext(), "Song not found!");
        }

    }

    /**
     * @param songId ID of the audio file
     * @return true if the requested audio file exists
     */
    private boolean songExists(String songId) {

        File[] songList = new File(getApplication().getExternalFilesDir(null) + File.separator + DIR_AUDIO).listFiles();

        for(File songFile : songList) {
            if(songFile.getName().contains(songId))
                return true;
        }
        return false;

    }


}
