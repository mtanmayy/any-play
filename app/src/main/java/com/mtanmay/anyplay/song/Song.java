package com.mtanmay.anyplay.song;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

/**
 * SQLite table for songs that defines the data stored for each song
 */
@Entity(tableName = "songs_table")
public class Song {

     // contains some fields that will be removed in future versions as they
     // effectively serve no purpose like {like_count, dislike_count}

    // As of the current version, {song_url} has no need
    // However maybe used in future versions to re-download a audio file if needed

    @NonNull
    @PrimaryKey
    private String songId;

    @ColumnInfo(name = "yt_name")
    private String ytName;

    @ColumnInfo(name = "c_name")
    private String cName;

    private String artist;

    @ColumnInfo(name = "like_count")
    private int likeCount;

    @ColumnInfo(name = "dislike_count")
    private int dislikeCount;

    @ColumnInfo(name = "thumbnail_url")
    private String thumbnailUrl;

    @ColumnInfo(name = "added_on")
    private long addedOn;

    @ColumnInfo(name = "secret_key")
    private String secretKey;

    public Song(String songId, String ytName, String cName, String artist, int likeCount, int dislikeCount, String thumbnailUrl, String secretKey, long addedOn) {
        this.songId = songId;
        this.ytName = ytName;
        this.cName = cName;
        this.artist = artist;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.thumbnailUrl = thumbnailUrl;
        this.addedOn = addedOn;
        this.secretKey = secretKey;
    }

    public String getYtName() {
        return ytName;
    }

    public String getCName() {
        return cName;
    }

    public String getArtist() {
        return artist;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getDislikeCount() {
        return dislikeCount;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getSecretKey() {
        return secretKey;
    }

    @NotNull
    public String getSongId() {
        return songId;
    }

    public long getAddedOn() {
        return addedOn;
    }

}
