package com.mtanmay.anyplay.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Utility class for checking network status
 */
public class NetworkUtils {

    /**
     *
     * @param context application context
     * @return true if the device is connected to a network i.e. either to a wi-fi or to mobile data
     */
    public static boolean isConnected(Context context) {

        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo infoMobile = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo infoWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        boolean connMobile = false, connWifi = false;

        if(infoMobile != null)
            connMobile = infoMobile.getState() == NetworkInfo.State.CONNECTED;

        if(infoWifi != null)
            connWifi = infoWifi.getState() == NetworkInfo.State.CONNECTED;

        return connMobile || connWifi;

    }
}
