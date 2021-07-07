package com.mtanmay.anyplay.application;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;

import java.io.File;

import static com.mtanmay.anyplay.Constants.DIR_AUDIO;

/**
 * Wrapper class that wraps the whole application |
 * First thing that runs when the app is launched
 */
public class App extends Application {

    private static final String TAG = "App";
    public static final String MEDIA_PLAYBACK_NOTIF_ID = "media_playback";

    @Override
    public void onCreate() {
        super.onCreate();

        createMediaPlaybackNotifChannel();
        deletePartialFiles();
        initYDL();

    }

    /**
     * Creates the notification channel to be used for devices running Android O and higher
     */
    private void createMediaPlaybackNotifChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    MEDIA_PLAYBACK_NOTIF_ID,
                    "Media Playback",
                    NotificationManager.IMPORTANCE_LOW);

            channel.setDescription("Media Playback Controls");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);

        }

    }

    /**
     * Deletes the files that were not downloaded fully or properly
     */
    private void deletePartialFiles() {

        File[] files = new File(getExternalFilesDir(null) + File.separator + DIR_AUDIO  + File.separator).listFiles();
        if(files == null)
            return;

        for(File file : files) {
            if(file.getName().contains(".m4a") || file.getName().contains(".part"))
                file.delete();
        }

    }

    /**
     * Initializes YDL library
     */
    private void initYDL() {

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    YoutubeDL.getInstance().init(getApplicationContext());
                    Log.d(TAG, "SearchFragViewModel: YDL initialized");
                } catch (YoutubeDLException e) {
                    e.printStackTrace();
                    Log.d(TAG, "SearchFragViewModel: YDL can't be initialized");
                }
            }
        };
        thread.start();
    }
}
