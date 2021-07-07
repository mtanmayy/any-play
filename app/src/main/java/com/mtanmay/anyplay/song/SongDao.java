package com.mtanmay.anyplay.song;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

/**
 * operations that can be performed on the database
 */
@Dao
public interface SongDao {

    @Insert
    void insert(Song song);

    @Update
    void update(Song song);

    @Delete
    void delete(Song song);

    @Query("DELETE FROM songs_table")
    void deleteAllSongs();

    // returns live data containing list of songs to observe
    @Query("SELECT * FROM songs_table ORDER BY added_on DESC")
    LiveData<List<Song>> getAllSongs();

    // returns list of songs
    @Query("SELECT * from songs_table ORDER BY added_on DESC")
    List<Song> getSongs();

    // audio file corresponding to the {song id}
    @Query("SELECT * FROM songs_table WHERE songId = :id")
    Song getSongFromId(String id);

    // list of songs satisfying the query
    @Query("SELECT * FROM songs_table WHERE c_name LIKE :query COLLATE UTF8_UNICODE_CI ORDER BY added_on DESC")
    List<Song> searchSongs(String query);

    // returns true if a audio file with the given id exists in the database
    @Query("SELECT EXISTS (SELECT * FROM songs_table WHERE songId = :id)")
    boolean songExists(String id);

}
