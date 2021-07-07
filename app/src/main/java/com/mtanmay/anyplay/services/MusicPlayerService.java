package com.mtanmay.anyplay.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mtanmay.anyplay.main.MainActivity;
import com.mtanmay.anyplay.MusicPlayer;
import com.mtanmay.anyplay.database.MDatabase;
import com.mtanmay.anyplay.song.Song;
import com.mtanmay.anyplay.ui.songview.SongViewActivity;
import com.mtanmay.anyplay.utils.EncryptDecryptUtils;
import com.mtanmay.anyplay.utils.FileUtils;
import com.mtanmay.anyplay.utils.NotificationUtils;
import com.mtanmay.anyplay.utils.ToastUtils;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.mtanmay.anyplay.Constants.AES;
import static com.mtanmay.anyplay.Constants.DIR_AUDIO;
import static com.mtanmay.anyplay.Constants.EXTRAS.CURR_SONG_ID;

public class MusicPlayerService extends Service implements MediaPlayer.OnPreparedListener,
                                                           MediaPlayer.OnCompletionListener,
                                                           MediaPlayer.OnSeekCompleteListener,
                                                           MediaPlayer.OnErrorListener,
                                                           AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "MusicPlayerService";

    IBinder mBinder = new LocalBinder();

    // currently playing audio file
    private Song mSong = null;

    private static MediaPlayer player = null;
    private AudioManager mAudioManager;

    //for maintaining song queue to avoid playing repeated songs while on shuffle
    private List<String> playedSongs = new ArrayList<>();

    private String currentSongId;
    private String receivedSongId;
    private String currSongName = "NA";
    private String currArtistName = "NA";

    private List<File> songsList;
    private File encryptedSongFile = null;

    // keeps track of current audio file's id
    public static MutableLiveData<String> songIdLData;

    // keeps track whether any audio is currently playing
    public static MutableLiveData<Boolean> isPlaying;

    public static boolean serviceRunning = false;
    private int mDirection = 1;
    private boolean onLoop = false;
    private boolean onShuffle = false;

    //intents
    private Intent openSong;
    private PendingIntent openSongPI;

    CompositeDisposable compositeDisposable = new CompositeDisposable();

    // decrypted key for the song
    String songKey = "NA";

    // keeps track if at least one song is played in services' lifetime ; used to evaluate if player is currently paused
    private boolean playedOnce = false;

    @Override
    public void onCreate() {
        super.onCreate();

        mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        if(player == null)
            player = MusicPlayer.getInstance(getApplicationContext());

        player.setOnPreparedListener(this);
        player.setOnSeekCompleteListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);

        songIdLData = new MutableLiveData<>();
        isPlaying = new MutableLiveData<>();

        songIdLData.setValue("NA");
        currentSongId = "NA";
        playedSongs = new ArrayList<>();
        isPlaying.setValue(false);

        songsList = getAllSongsList();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        serviceRunning = true;

        if(player == null)
            player = MusicPlayer.getInstance(getApplicationContext());

        songsList = getAllSongsList();
        for(File song : songsList) {
            Log.d(TAG, "onStartCommand: " + song.getName());
        }

        // getting the {song id} that started the service
        receivedSongId = intent.getStringExtra("EXTRA_SONG_ID");

        if(!currentSongId.equals(receivedSongId)) {
            Log.d(TAG, "onStartCommand: SONG CHANGED | PREVIOUS: " + currentSongId + " | NEW: " + receivedSongId);
            try {
                currentSongId = receivedSongId;
                songIdLData.setValue(currentSongId);
                playSong(currentSongId, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            pause();
            player.seekTo(0);
            start();
        }

        return START_STICKY;
    }

    /**
     * used to play the audio file
     * @param receivedSongId
     * @param newSong specifies whether a new song is played or not to appropriately play or pause the player
     * @throws IOException if file to play is not found
     */
    public void playSong(String receivedSongId, boolean newSong) throws IOException {

        // initialises the player
        player = MusicPlayer.getInstance(getApplicationContext());

        Log.d(TAG, "playSong: SONG PLAYED WITH ID: " + receivedSongId);
        songsList = getAllSongsList();

        // play or pause the song if the same song is played again
        // used by play/pause media control buttons
        if(!newSong) {
            if(player.isPlaying())
                pause();
            else
                start();

            return;
        }

        encryptedSongFile = getSongFile(receivedSongId);

        if(!encryptedSongFile.exists()) {
            Log.d(TAG, "playSong: SONG NOT FOUND");
            return;
        }

        if(playedOnce) {
            if(player.isPlaying())
                player.stop();

            player.reset();
        }

        try {

            File tempSongFile = getDecryptedSongFile(receivedSongId);
            player.setDataSource(tempSongFile.getAbsolutePath());

            if(tempSongFile.exists())
                tempSongFile.delete();

        } catch (Exception e) {
            ToastUtils.create(this, "Unable to play song");
            e.printStackTrace();
        }

        currentSongId = receivedSongId;
        songIdLData.setValue(receivedSongId);
        playedSongs.add(receivedSongId);

        // loading the audio file in the player for playing
        player.prepare();

        // creating the pending intent for the notification ; intent to execute on notification click
        openSong = new Intent(this, SongViewActivity.class);
        openSong.putExtra(CURR_SONG_ID, currentSongId);
        openSong.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openSongPI = PendingIntent.getActivity(this, 0, openSong, PendingIntent.FLAG_CANCEL_CURRENT);

        Disposable loadSong = Single.fromCallable(() -> MDatabase.getInstance(getApplication()).songDao().getSongFromId(currentSongId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(song -> {

                    mSong = song;
                    currSongName = song.getCName();
                    currArtistName = song.getArtist();

                    // setting the currently playing song in the bottom bar
                    MainActivity.showCurrPlayingSong(song);

                    // sending notif about the currently playing song
                    NotificationUtils.create(getApplicationContext(), currSongName, currArtistName, openSongPI);

                });
        compositeDisposable.add(loadSong);
    }

    /**
     * gets the file after decrypting the audio file in the app's internal storage
     * @param receivedSongId
     * @return File
     * @throws Exception
     */
    private File getDecryptedSongFile(String receivedSongId) throws Exception {

        songKey = Single.fromCallable(() -> MDatabase.getInstance(getApplicationContext()).songDao().getSongFromId(receivedSongId).getSecretKey())
                .subscribeOn(Schedulers.io())
                .blockingGet();

        File songFile = FileUtils.getFile(getApplicationContext(), DIR_AUDIO, receivedSongId);
        byte[] songFileBytes = FileUtils.readFile(songFile);
        byte[] keyBytes = Base64.decode(songKey, Base64.NO_WRAP);
        SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, AES);
        byte[] decodedBytes = EncryptDecryptUtils.decode(secretKey, songFileBytes);

        return FileUtils.getTempFile(getApplicationContext(), "audio", decodedBytes, "temp");
    }

    /**
     * returns the object of currently playing song
     * @return Song
     */
    public Song getMSong() {
        return mSong;
    }

    /**
     * pauses the player and abandons the audio focus
     */
    public void pause() {
        if(player != null)
            player.pause();
        isPlaying.setValue(false);
        mAudioManager.abandonAudioFocus(this);
    }

    /**
     * starts the player
     */
    public void start() {
        if(player != null && getFocusResult() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            player.start();
            isPlaying.setValue(true);
        }

    }

    /**
     * result of requesting audio focus
     * @return int
     */
    public int getFocusResult() {

        if(mAudioManager == null) {
            return AudioManager.AUDIOFOCUS_REQUEST_FAILED;
        }

        return mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

    }

    /**
     * returns true if the player is paused
     * @return boolean
     */
    public boolean isPaused() {
        return playedOnce && !player.isPlaying();
    }

    /**
     * position of the seek for the currently playing audio file
     * @return int
     */
    public int getPlayerPos() {
        return this.player.getCurrentPosition() / 1000;
    }

    /**
     * duration of the currently playing audio file in seconds
     * @return int
     */
    public int getPlayerDuration() {
        return this.player.getDuration() / 1000;
    }

    /**
     * returns the player instance
     * @return MediaPlayer
     */
    public MediaPlayer getPlayer() {
        return this.player;
    }

    /**
     * gets the audio file corresponding to the song id
     * @param songId
     * @return File
     */
    private File getSongFile(String songId) {
        return FileUtils.getFile(getApplicationContext(), DIR_AUDIO, songId);
    }

    /**
     * gets the list of audio file from the app's internal storage
     * @return List
     */
    private List<File> getAllSongsList() {

        File songsDir = new File(getApplication().getExternalFilesDir(null) + File.separator + DIR_AUDIO + File.separator);

        if(!songsDir.exists())
            songsDir.mkdir();

        File[] files = songsDir.listFiles();

        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
        return Arrays.asList(files);

    }

    /**
     * plays the next song
     * @param direction specifies whether to play next (-1) or previous song (-1)
     */
    public void playNextSong(int direction) {

        mDirection = direction;
        songsList = getAllSongsList();

        if(onShuffle)
            playShuffleSong();
        else if(onLoop)
            repeatSong();
        else
            playNormalSong(direction);
    }

    private void playNormalSong(int direction) {

        int nextSongIndex = 0, currSongIndex = 0;
        if(songsList.size() == 1) {
            nextSongIndex = 0;
        }
        else {

            for(File song : songsList) {
                if(song.getName().equals(currentSongId))
                    currSongIndex = songsList.indexOf(song);
            }

            if(direction == -1) {
                int index = currSongIndex - 1;
                if(index < 0)
                    index = songsList.size()-1;
                nextSongIndex = index;
            }
            else if(direction == 1) {
                int index = currSongIndex + 1;
                if(index > songsList.size()-1)
                    index = 0;
                nextSongIndex = (index) % songsList.size();
            }
        }

        // next audio file to play
        File nextSong = songsList.get(nextSongIndex);

        if(nextSong == null) {
            playNormalSong(mDirection);
        }

        // checking if the song exists in database
        boolean songInDb = Single.fromCallable(() -> MDatabase.getInstance(getApplicationContext()).songDao().songExists(nextSong.getName()))
                .subscribeOn(Schedulers.io())
                .blockingGet();

        // deleting the song if it's not present in the database
        if(!songInDb) {

            if(nextSong.exists()) {
                File imgFile = new File(getApplicationContext().getExternalFilesDir(null) + File.separator + ".images" + File.separator + nextSong.getName());

                nextSong.delete();
                if(imgFile != null && imgFile.exists())
                    imgFile.delete();
            }

            songsList = getAllSongsList();
            playNormalSong(mDirection);
        }

        try {
            String songId = nextSong.getName();
            playSong(songId, true);
        } catch (IOException e) {
            e.printStackTrace();
            playNormalSong(mDirection);
        }

    }

    // next song to play when on shuffle
    private void playShuffleSong() {

        int nextSongIndex;

        Random random = new Random();
        nextSongIndex = random.nextInt(songsList.size());

        // next audio file to play
        File nextSong = songsList.get(nextSongIndex);

        if(nextSong == null) {
            playShuffleSong();
        }

        // checking if the song exists in database
        boolean songInDb = Single.fromCallable(() -> MDatabase.getInstance(getApplicationContext()).songDao().songExists(nextSong.getName()))
                .subscribeOn(Schedulers.io())
                .blockingGet();

        // deleting the song if it's not present in the database
        if(!songInDb) {
            if(nextSong.exists())
                nextSong.delete();

            songsList = getAllSongsList();
            playShuffleSong();
        }

        // clearing the queue if every audio file is played at least once
        if(playedSongs != null && (playedSongs.size() >= songsList.size())) {
            playedSongs.clear();
        }

        // checking whether the audio file to be played next has already been played once in the current queue
        // doing this makes sure the same audio file is not played more than once until every file is played once
        if(playedSongs != null && !playedSongs.contains(nextSong.getName())) {
            try {
                String songId = nextSong.getName();
                playSong(songId, true);
            } catch (IOException e) {
                e.printStackTrace();
                playShuffleSong();
            }
        }
        else
            playShuffleSong();

    }

    // returns whether the tracks are on loop
    public boolean isOnLoop() {
        return onLoop;
    }

    // returns whether the tracks are on shuffle
    public boolean isOnShuffle() {
        return onShuffle;
    }

    // setting the audio file on loop
    public void setOnLoop(boolean onLoop) {
        this.onLoop = onLoop;
    }

    // setting the tracks on shuffle
    public void setOnShuffle(boolean onShuffle) {
        this.onShuffle = onShuffle;
    }

    // keeps track of the {song id} of currently playing song
    public LiveData<String> getSongIdLData() {
        return songIdLData;
    }

    // when current audio file id completely played
    @Override
    public void onCompletion(MediaPlayer mp) {

        songsList = getAllSongsList();
        Log.d(TAG, "onCompletion: LOOPING: " + onLoop + " | SHUFFLING: " + onShuffle);
        playNextSong(1);

        songIdLData.setValue(currentSongId);
        isPlaying.setValue(false);

    }

    // looping over the same audio file
    private void repeatSong() {
        pause();
        player.seekTo(0);
        start();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        start();
        playedOnce = true;
    }

    // keeps tracks of whether audio is currently playing
    public LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {

        try {
            mp.reset();
            playSong(currentSongId, true);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // handles the scenario when
    // another app starts playing audio (complete audio focus loss): AUDIOFOCUS_LOSS
    // transient audio loss (like in a phone call): AUDIOFOCUS_LOSS_TRANSIENT
    // app is granted audio focus: AUDIOFOCUS_GAIN
    @Override
    public void onAudioFocusChange(int focusChange) {

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: {
                Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_LOSS_TRANSIENT");
                pause();
                break;
            }
            case AudioManager.AUDIOFOCUS_GAIN: {
                Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_GAIN");
                start();
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS: {
                Log.d(TAG, "onAudioFocusChange: AUDIOFOCUS_LOSS");
                pause();
            }
            default:
                Log.d(TAG, "onAudioFocusChange: AUDIO FOCUS DEFAULT");
        }

    }

    // binder object to be used by other components when binding with the service
    public class LocalBinder extends Binder {
        public MusicPlayerService getServiceInstance() {
            return MusicPlayerService.this;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "MusicPlayerService onDestroy: SERVICE KILLED");
        super.onDestroy();

        if(player != null) {
            // releasing the player because it is no longer needed
            player.release();
            player = null;
        }

        // disposing all the disposables
        if(compositeDisposable != null) {
            compositeDisposable.dispose();
        }

        // changing running state to false
        serviceRunning = false;
    }

}