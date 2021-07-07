package com.mtanmay.anyplay.song;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class
SongViewModel extends AndroidViewModel {

    // calls repository methods as requested by the ui controller

    private SongRepository repository;

    public SongViewModel(@NonNull Application application) {
        super(application);
        repository = new SongRepository(application);
    }

    public void insertSong(Song song) {
        repository.insertSong(song);
    }

    public void deleteSong(Song song) {
        repository.deleteSong(song);
    }

    public LiveData<List<Song>> getAllSongs() {
        return repository.getAllSongs();
    }

}
