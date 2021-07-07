package com.mtanmay.anyplay.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.mtanmay.anyplay.song.Song;
import com.mtanmay.anyplay.song.SongDao;

@Database(entities = Song.class, version = 1)
public abstract class MDatabase extends RoomDatabase {

    private static MDatabase instance;
    public abstract SongDao songDao();

    // singleton
    public static synchronized MDatabase getInstance(Context context) {

        if(instance == null) {

            instance = Room.databaseBuilder(context.getApplicationContext(),
                                            MDatabase.class,
                                            "song_database")
                    .fallbackToDestructiveMigration()
                    .build();

        }
        return instance;
    }

}
