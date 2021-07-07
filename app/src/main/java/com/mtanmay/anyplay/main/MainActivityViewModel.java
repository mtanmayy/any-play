package com.mtanmay.anyplay.main;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.mtanmay.anyplay.song.Song;

public class MainActivityViewModel extends AndroidViewModel {

    private MutableLiveData<String> songName;
    private MutableLiveData<String> artistName;
    private MutableLiveData<Song> song;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);

        songName = new MutableLiveData<>();
        artistName = new MutableLiveData<>();
        song = new MutableLiveData<>();

        songName.setValue("NA");
        artistName.setValue("NA");
        song.setValue(null);
    }

    public LiveData<String> getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName.setValue(songName);
    }

    public LiveData<String> getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName.setValue(artistName);
    }

    public LiveData<Song> getSong() {
        return song;
    }

    public void setSong(Song song) {
        this.song.setValue(song);
    }

}
