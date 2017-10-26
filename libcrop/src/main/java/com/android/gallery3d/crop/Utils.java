package com.android.gallery3d.crop;

import android.os.ParcelFileDescriptor;

import java.io.Closeable;

/**
 * Created by qiaomu on 2017/10/24.
 */

public class Utils {
    public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
        }
    }

    public static void closeSilently(ParcelFileDescriptor c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
        }
    }

}
