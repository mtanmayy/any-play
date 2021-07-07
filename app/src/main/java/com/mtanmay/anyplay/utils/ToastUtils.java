package com.mtanmay.anyplay.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Utility class for global toast
 */
public class ToastUtils {

    private static final String TAG = "ToastUtils";

    private static Toast mToast = null;

    @SuppressLint("ShowToast")
    public static void create(Context context, String msg) {

        Log.d(TAG, "create: Toast is null" + (mToast == null));

        if(mToast != null)
            mToast.cancel();

        mToast = Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT);
        mToast.show();

    }

}