package com.mtanmay.anyplay.main;   

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.navigation.NavigationView;
import com.mtanmay.anyplay.MusicPlayer;
import com.mtanmay.anyplay.R;
import com.mtanmay.anyplay.database.MDatabase;
import com.mtanmay.anyplay.services.MusicPlayerService;
import com.mtanmay.anyplay.song.Song;
import com.mtanmay.anyplay.song.SongDao;
import com.mtanmay.anyplay.ui.songview.SongViewActivity;
import com.mtanmay.anyplay.utils.EncryptDecryptUtils;
import com.mtanmay.anyplay.utils.FileUtils;
import com.mtanmay.anyplay.utils.NotificationUtils;
import com.mtanmay.anyplay.utils.ToastUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.palette.graphics.Palette;

import java.io.File;
import java.lang.ref.WeakReference;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import static com.mtanmay.anyplay.Constants.AES;
import static com.mtanmay.anyplay.Constants.DIR_IMAGES;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Toolbar toolbar;
    private MusicPlayerService mService;
    private static MainActivityViewModel mViewModel;
    private AppBarConfiguration mAppBarConfiguration;

    private ImageView btnPlay;
    private TextView songName;
    private ImageView songImg;
    private CardView bottomBar;
    private TextView artistName;

    private static WeakReference<Context> mContext;
    public static WeakReference<CardView> weakBottomBar;
    private static WeakReference<TextView> weakSongName;
    private static WeakReference<ImageView> weakSongImg;
    private static WeakReference<TextView> weakArtistName;

    private boolean mBounded;
    private static boolean showBottomBar = false;

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mBounded = true;
            MusicPlayerService.LocalBinder mLocalBinder = (MusicPlayerService.LocalBinder) service;
            mService = mLocalBinder.getServiceInstance();

            btnPlay = findViewById(R.id.bottom_bar_play_btn);

            if(mService.getMSong() != null && showBottomBar)
                showCurrPlayingSong(mService.getMSong());

            if(mService.isPaused())
                btnPlay.setImageResource(R.drawable.ic_play_bb);
            else
                btnPlay.setImageResource(R.drawable.ic_pause_bb);

            btnPlay.setOnClickListener(v -> {
                Log.d(TAG, "onServiceConnected: BTN PLAY CLICKED");
                if(mService.getPlayer().isPlaying())
                    mService.pause();
                else
                    mService.start();
            });

            mService.getIsPlaying().observe(MainActivity.this, isPlaying ->  {
                if(isPlaying)
                    btnPlay.setImageResource(R.drawable.ic_pause_bb);
                else
                    btnPlay.setImageResource(R.drawable.ic_play_bb);

                if(showBottomBar && weakBottomBar != null)
                    weakBottomBar.get().setVisibility(View.VISIBLE);
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBounded = false;
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AnyPlay_NoActionBar);
        setContentView(R.layout.activity_main);
        mContext = new WeakReference<>(getApplicationContext());

        // Custom toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // binding the service
        Intent serviceIntent = new Intent(this, MusicPlayerService.class);
        bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);

        // initializing navigation comps
        initNavigationComponents();

        //initializing views
        initViews();

        mViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(MainActivityViewModel.class);

        initObservers();

        if(showBottomBar)
            weakBottomBar.get().setVisibility(View.VISIBLE);
        else
            weakBottomBar.get().setVisibility(View.GONE);

        weakBottomBar.get().setOnClickListener(v -> {

            Intent openSongView = new Intent(this, SongViewActivity.class);
            startActivity(openSongView);
            overridePendingTransition(R.anim.slide_up, R.anim.static_animation);

        });

        btnPlay.setOnClickListener(v -> {
            if(MusicPlayer.getInstance(mContext.get()) != null) {
                if(MusicPlayer.getInstance(mContext.get()).isPlaying())
                    btnPlay.setImageResource(R.drawable.ic_pause_bb);
                else
                    btnPlay.setImageResource(R.drawable.ic_play_bb);
            }

        });
    }

    private void initViews() {
        bottomBar = findViewById(R.id.bottomBar);
        songName = findViewById(R.id.bottom_bar_song_name);
        artistName = findViewById(R.id.bottom_bar_artist_name);
        songImg = findViewById(R.id.bottom_bar_song_img);
        btnPlay = findViewById(R.id.bottom_bar_play_btn);

        weakBottomBar = new WeakReference<>(bottomBar);
        weakArtistName = new WeakReference<>(artistName);
        weakSongName = new WeakReference<>(songName);
        weakSongImg = new WeakReference<>(songImg);

    }

    public static void showCurrPlayingSong(Song song) {

        showBottomBar = true;
        mViewModel.setSongName(song.getCName());
        mViewModel.setArtistName(song.getArtist());
        mViewModel.setSong(song);
        if(weakBottomBar != null && weakBottomBar.get() != null)
            weakBottomBar.get().setVisibility(View.VISIBLE);

    }

    // decrypting the image file and setting it to the bottom song bar
    private static void setSongImg(Song song) {

        try {
            File imgFile = FileUtils.getFile(mContext.get(), DIR_IMAGES, song.getSongId());
            if (imgFile != null) {

                byte[] encryptedImgBytes = FileUtils.readFile(imgFile);
                byte[] secretKeyBytes = Base64.decode(song.getSecretKey(), Base64.NO_WRAP);
                SecretKey secretKey = new SecretKeySpec(secretKeyBytes, 0, secretKeyBytes.length, AES);
                byte[] decryptedImgBytes = EncryptDecryptUtils.decode(secretKey, encryptedImgBytes);
                File tempImgFile = FileUtils.getTempFile(mContext.get(), "img", decryptedImgBytes, song.getSongId());
                if (tempImgFile != null) {

                    Glide.with(mContext.get())
                            .asBitmap()
                            .load(Uri.fromFile(tempImgFile))
                            .placeholder(R.drawable.loader)
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    Palette palette = Palette.from(resource).generate();
                                    Palette.Swatch swatch = palette.getDominantSwatch();
                                    weakSongImg.get().setImageBitmap(resource);
                                    if (swatch != null) {
                                        weakSongImg.get().setBackgroundColor(swatch.getRgb());
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                }
                            });
                }

            }
            else {
                Log.d(TAG, "setSongImg: NO IMAGE FILE FOUND");
            }
        } catch (Exception e) {
            Log.e(TAG, e.getClass() + ": " + e.getMessage());
        }
    }

    public void initObservers() {

        if(mViewModel == null)
            mViewModel = new ViewModelProvider(this,
                    ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(MainActivityViewModel.class);

        mViewModel.getSongName().observe(this, s -> {
            if(!s.equals("NA")) {
                if(weakSongName != null)
                    weakSongName.get().setText(s);
            }
        });

        mViewModel.getArtistName().observe(this, s -> {
            if(!s.equals("NA")) {
                if(weakArtistName != null)
                    weakArtistName.get().setText(s);
            }
        });

        mViewModel.getSong().observe(this, MainActivity::setSongImg);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.main, menu);
//        menu.getItem(0).getIcon().setTint(getResources().getColor(R.color.black, getTheme()));
        TypedArray typedArray = getTheme().obtainStyledAttributes(new int[] {R.attr.app_text});
        menu.getItem(0).getIcon().setTint(typedArray.getColor(0, 0));
        return true;
    }

    @SuppressLint("CheckResult")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.action_delete_all) {

            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert
                    .setTitle(getApplicationContext().getString(R.string.app_name))
                    .setMessage("This will delete all songs. Proceed?")
                    .setPositiveButton("Confirm", (dialog, which) -> deleteAllSongs())
                    .setNegativeButton("Cancel", (dialog, which) -> {})
                    .setCancelable(true)
                    .create()
                    .show();

            return true;
        }
        else
            return super.onOptionsItemSelected(item);
    }

    private void deleteAllSongs() {

        SongDao songDao = Single.fromCallable(() -> MDatabase.getInstance(getApplicationContext()).songDao())
                .subscribeOn(Schedulers.io())
                .blockingGet();

        //background thread to delete all the songs from the database
        Thread thread = new Thread(new deleteAllThread(songDao));
        thread.start();

        if(weakBottomBar != null)
            weakBottomBar.get().setVisibility(View.GONE);

        if(mBounded && mService != null)
            mService.getPlayer().stop();

        showBottomBar = false;

        File audioDir = new File(getApplicationContext().getExternalFilesDir(null) + File.separator + ".audio");
        if(!audioDir.exists())
            audioDir.mkdir();

        File imgDir = new File(getApplicationContext().getExternalFilesDir(null) + File.separator + ".images");
        if(!imgDir.exists())
            imgDir.mkdir();

        // deleting all the downloaded audio and image files
        try {
            if(audioDir != null && audioDir.exists()) {

                for(File file : audioDir.listFiles()) {
                    if (!file.getName().contains(".m4a.part"))
                        file.delete();
                }
                audioDir.delete();
            }

            if(imgDir != null && imgDir.exists())
                org.apache.commons.io.FileUtils.forceDelete(imgDir);

            ToastUtils.create(getApplicationContext(), "All songs deleted!");
        } catch (Exception e) {
            ToastUtils.create(getApplicationContext(), "Unable to delete songs!");
        }

        NotificationUtils.cancelAll(getApplicationContext());
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void initNavigationComponents() {

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.setDrawerIndicatorEnabled(true);
        toggle.setToolbarNavigationClickListener(v -> {
            drawer.openDrawer(GravityCompat.START);
        });

        // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_library, R.id.nav_search, R.id.nav_about)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if(drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // unbinding the service when activity is destroyed
        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
            mService = null;
        }
    }

    // runnable class to initiate database deletion in background
    private static class deleteAllThread implements Runnable {

        private SongDao dao;

        public deleteAllThread(SongDao mDao) {
            dao = mDao;
        }

        @Override
        public void run() {
            dao.deleteAllSongs();
        }
    }

}
