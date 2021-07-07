package com.mtanmay.anyplay.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import androidx.core.app.NotificationCompat;
import com.mtanmay.anyplay.R;

import static com.mtanmay.anyplay.application.App.MEDIA_PLAYBACK_NOTIF_ID;

/**
 * Utility class that manages the state of notifications
 */
public class NotificationUtils {

    /**
     * Creates a notification
     * @param context application context
     * @param songName notification title
     * @param artistName notification text
     * @param openPI intent to execute when notification is clicked
     */
    public static void create(Context context, String songName, String artistName, PendingIntent openPI) {

        NotificationManager manager = context.getSystemService(NotificationManager.class);

        Notification notification = new NotificationCompat.Builder(context, MEDIA_PLAYBACK_NOTIF_ID)
                .setSmallIcon(R.drawable.ic_app) // app icon
                .setPriority(NotificationCompat.PRIORITY_LOW) // for devices lower than Android 7.1 (API LEVEL 25)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(songName)
                .setContentText(artistName)
                .setContentIntent(openPI)
                .setShowWhen(false)
                .setOngoing(true)
                .build();

        manager.notify(1, notification);

    }

    /**
     * Removes all the notifications
     * @param context application context
     */
    public static void cancelAll(Context context) {

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.cancelAll();
    }

}
