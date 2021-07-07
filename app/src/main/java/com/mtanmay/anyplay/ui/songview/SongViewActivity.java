package com.mtanmay.anyplay.ui.songview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.mtanmay.anyplay.R;
import com.mtanmay.anyplay.database.MDatabase;
import com.mtanmay.anyplay.services.MusicPlayerService;
import com.mtanmay.anyplay.utils.EncryptDecryptUtils;
import com.mtanmay.anyplay.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.mtanmay.anyplay.Constants.AES;
import static com.mtanmay.anyplay.Constants.DIR_IMAGES;

public class SongViewActivity extends AppCompatActivity implements  View.OnClickListener,
                                                                    SeekBar.OnSeekBarChangeListener{

    private static final String TAG = "SongViewActivity";

    //views
    ImageView songImg;
    TextView songName;
    TextView artistName, songPos, songDuration;
    SeekBar seekBar;
    ImageView btnPrev, btnPlay, btnNext, btnShuffle, btnLoop;

    String currSongId = "NA";
    File tempImgFile;

    //disposables holder
    CompositeDisposable compositeDisposable = new CompositeDisposable();

    //MPS
    MusicPlayerService mService;
    boolean mBounded;

    //disposables
    Disposable disposableSongLoader, disposableSeekBarProgress;

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mBounded = true;
            MusicPlayerService.LocalBinder mLocalBinder = (MusicPlayerService.LocalBinder) service;
            mService = mLocalBinder.getServiceInstance();

            seekBar.setOnSeekBarChangeListener(SongViewActivity.this);
            btnNext.setOnClickListener(SongViewActivity.this);

            if(mService.isOnLoop())
                btnLoop.getDrawable().setTint(getColor(R.color.orange));
            else
                btnLoop.getDrawable().setTint(Color.GRAY);

            if(mService.isOnShuffle())
                btnShuffle.getDrawable().setTint(getColor(R.color.orange));
            else
                btnShuffle.getDrawable().setTint(Color.GRAY);

            mService.getIsPlaying().observe(SongViewActivity.this, isPlaying -> {
                if(isPlaying)
                    btnPlay.setImageResource(R.drawable.ic_pause);
                else
                    btnPlay.setImageResource(R.drawable.ic_play);
            });

            mService.getSongIdLData().observe(SongViewActivity.this, s -> {

                Log.d(TAG, "onChanged: mService SONG ID CHANGED TO: " + s);
                if(!s.equals("NA")) {
                    disposableSongLoader = Single.fromCallable(() -> MDatabase.getInstance(getApplicationContext()).songDao().getSongFromId(s))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(song -> {
                                if (song != null) {

                                    songName.setText(song.getCName());
                                    artistName.setText(song.getArtist());

                                    seekBar.setProgress(0);
                                    songDuration.setText(songTotalDuration());
                                    seekBar.setMax(mService.getPlayerDuration());
                                    disposableSeekBarProgress = Observable.timer(500, TimeUnit.MILLISECONDS)
                                            .repeat()
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(aLong -> {
                                                    seekBar.setProgress(mService.getPlayerPos());
                                                    songPos.setText(currSongPos());
                                            }, e -> {
                                                Log.d(TAG, "onChanged: PLAYER ERROR");
                                                e.printStackTrace();
                                            });
                                    compositeDisposable.add(disposableSeekBarProgress);

                                    File imgFile = FileUtils.getFile(getApplicationContext(), DIR_IMAGES, song.getSongId());
                                    if(imgFile != null && imgFile.exists()) {

                                        byte[] encryptedImgBytes = FileUtils.readFile(imgFile);
                                        byte[] secretKeyBytes = Base64.decode(song.getSecretKey(), Base64.NO_WRAP);
                                        SecretKey secretKey = new SecretKeySpec(secretKeyBytes, 0, secretKeyBytes.length, AES);
                                        byte[] decryptedImgBytes = EncryptDecryptUtils.decode(secretKey, encryptedImgBytes);
                                        tempImgFile = FileUtils.getTempFile(getApplicationContext(), "img", decryptedImgBytes, song.getSongId());
                                        if(tempImgFile != null) {

                                            Glide.with(SongViewActivity.this)
                                                    .asBitmap()
                                                    .load(Uri.fromFile(tempImgFile))
                                                    .placeholder(R.drawable.loader)
                                                    .into(new CustomTarget<Bitmap>() {
                                                        @Override
                                                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                                            Palette palette = Palette.from(resource).generate();
                                                            Palette.Swatch swatch = palette.getDominantSwatch();
                                                            songImg.setImageBitmap(resource);
                                                            if (swatch != null) {
                                                                songImg.setBackgroundColor(swatch.getRgb());
                                                            }
                                                        }

                                                        @Override
                                                        public void onLoadCleared(@Nullable Drawable placeholder) { }
                                                    });
                                        }

                                    }
                                    else {
                                        Toast.makeText(getApplicationContext(), "img file null", Toast.LENGTH_SHORT).show();
                                        Glide.with(SongViewActivity.this)
                                                .asBitmap()
                                                .load(song.getThumbnailUrl())
                                                .into(new CustomTarget<Bitmap>() {
                                                    @Override
                                                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                                        Palette palette = Palette.from(resource).generate();
                                                        Palette.Swatch swatch = palette.getDominantSwatch();
                                                        songImg.setImageBitmap(resource);
                                                        if (swatch != null) {
                                                            songImg.setBackgroundColor(swatch.getRgb());
                                                        }
                                                    }

                                                    @Override
                                                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                                                });
                                    }
                                }
                            }, Throwable::printStackTrace);
                    compositeDisposable.add(disposableSongLoader);

                }
                else {

                    songName.setText(getString(R.string.loading));
                    artistName.setText(getString(R.string.loading));
                    Glide.with(SongViewActivity.this)
                            .load(R.drawable.loader)
                            .into(songImg);
                }
            });
    }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(SongViewActivity.this, name.toString() + " | SERVICE DISCONNECTED", Toast.LENGTH_SHORT).show();
            mBounded = false;
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_view);

        getSupportActionBar().hide();

        Intent intent = new Intent(this, MusicPlayerService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);

        initViews();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setValues();
    }

    private void setValues() {

        btnPlay.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnPrev.setOnClickListener(this);
        btnShuffle.setOnClickListener(this);
        btnLoop.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        ImageView btn = (ImageView) v;

        if(btn.getId() == R.id.sv_play_btn) {

            try {
                mService.playSong(currSongId, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(btn.getId() == R.id.sv_next_btn) {
            mService.playNextSong(1);
        }
        else if(btn.getId() == R.id.sv_prev_btn) {
            mService.playNextSong(-1);
        }
        else if(btn.getId() == R.id.sv_shuffle_btn) {
            mService.setOnShuffle(!mService.isOnShuffle());
            if(mService.isOnShuffle())
                btn.getDrawable().setTint(getColor(R.color.orange));
            else
                btn.getDrawable().setTint(Color.GRAY);
        }
        else if(btn.getId() == R.id.sv_loop_btn) {
            mService.setOnLoop(!mService.isOnLoop());
            if(mService.isOnLoop())
                btn.getDrawable().setTint(getColor(R.color.orange));
            else
                btn.getDrawable().setTint(Color.GRAY);
        }
    }

    private String currSongPos() {

        if(mService == null)
            return "";

        String minutes, seconds;

        int t_min = mService.getPlayerPos() / 60;
        if(t_min < 10) {
            minutes = "0" + t_min;
        }
        else
            minutes = String.valueOf(t_min);

        int t_sec = mService.getPlayerPos() % 60;
        if(t_sec < 10)
            seconds = "0" + t_sec;
        else
            seconds = String.valueOf(t_sec);

        return (minutes + ":" + seconds);
    }

    private String songTotalDuration() {

        if(mService == null)
            return "";

        String minutes, seconds;

        int t_min = mService.getPlayerDuration() / 60;
        if(t_min < 10)
            minutes = "0" + t_min;
        else
            minutes = String.valueOf(t_min);

        int t_sec = mService.getPlayerDuration() % 60;
        if(t_sec < 10)
            seconds = "0" + t_sec;
        else
            seconds = String.valueOf(t_sec);

        return (minutes + ":" + seconds);
    }

    private void initViews() {
        songImg = findViewById(R.id.sv_img);
        btnPlay = findViewById(R.id.sv_play_btn);
        btnNext = findViewById(R.id.sv_next_btn);
        btnPrev = findViewById(R.id.sv_prev_btn);
        btnShuffle = findViewById(R.id.sv_shuffle_btn);
        btnLoop = findViewById(R.id.sv_loop_btn);
        seekBar = findViewById(R.id.sv_seekbar);
        songName = findViewById(R.id.sv_name);
        artistName = findViewById(R.id.sv_artist);
        songPos = findViewById(R.id.sv_song_curr_pos);
        songDuration = findViewById(R.id.sv_song_duration);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.static_animation, R.anim.slide_down);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        compositeDisposable.dispose();
        if(mBounded) {
            unbindService(mConnection);
            Log.d(TAG, "onDestroy: SERVICE UNBIND SUCCESSFUL");
            mBounded = false;
            mService = null;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mService.getPlayer().pause();
        mService.getPlayer().seekTo(seekBar.getProgress() * 1000);
    }
}