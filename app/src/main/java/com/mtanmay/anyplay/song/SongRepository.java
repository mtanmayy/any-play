package com.mtanmay.anyplay.song;

import android.app.Application;

import androidx.lifecycle.LiveData;
import com.mtanmay.anyplay.database.MDatabase;
import java.util.List;

public class SongRepository{

    // contains thread classes to run database operations in the background

    private SongDao mDao;
    private LiveData<List<Song>> allSongs;

    public SongRepository(Application application) {

        MDatabase mDatabase = MDatabase.getInstance(application);
        mDao = mDatabase.songDao();
        allSongs = mDao.getAllSongs();
    }

    public void insertSong(Song song) {
        Thread thread = new Thread(new insertThread(song));
        thread.start();
    }

    public void updateSong(Song song) {
        Thread thread = new Thread(new updateThread(song));
        thread.start();
    }

    public void deleteSong(Song song) {
        Thread thread = new Thread(new deleteThread(song));
        thread.start();
    }

    public void deleteAllSongs() {
        Thread thread = new Thread(new deleteAllThread());
        thread.start();
    }

    public LiveData<List<Song>> getAllSongs() {
        return allSongs;
    }


    // classes to execute DAO operations in the background thread
    private class insertThread implements Runnable {

        private SongDao dao;
        private final Song song;

        public insertThread(Song song) {
            this.song = song;
            dao = mDao;
        }

        @Override
        public void run() {
            dao.insert(song);
        }
    }

    private class updateThread implements Runnable {

        private SongDao dao;
        private final Song song;

        public updateThread(Song song) {
            this.song = song;
            dao = mDao;
        }

        @Override
        public void run() {
            dao.update(song);
        }
    }

    private class deleteThread implements Runnable {

        private SongDao dao;
        private final Song song;

        public deleteThread(Song song) {
            this.song = song;
            dao = mDao;
        }

        @Override
        public void run() {
            dao.delete(song);
        }
    }

    private class deleteAllThread implements Runnable {

        private SongDao dao;

        public deleteAllThread() {
            dao = mDao;
        }

        @Override
        public void run() {
            dao.deleteAllSongs();
        }
    }

}
