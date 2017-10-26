package com.android.gallery3d.crop;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by qiaomu on 2017/10/24.
 */

public class SaveImage {
    private static final String TIME_STAMP_NAME = "_yyyyMMdd_HHmmss";
    private static final String PREFIX_PANO = "PANO";
    private static final String PREFIX_IMG = "IMG";
    private static final String POSTFIX_JPG = ".jpg";
    private static final String AUX_DIR_NAME = ".aux";
    private static final String LOGTAG = "SaveImage";

    private int mCurrentProcessingStep = 1;

    public static final int MAX_PROCESSING_STEPS = 6;
    public static final String DEFAULT_SAVE_DIRECTORY = "EditedOnlinePhotos";

    public static Uri makeAndInsertUri(Context context, Uri sourceUri) {
        long time = System.currentTimeMillis();
        String filename = new SimpleDateFormat(TIME_STAMP_NAME).format(new Date(time));
        File saveDirectory = getFinalSaveDirectory(context, sourceUri);
        File file = new File(saveDirectory, filename + ".JPG");
        return linkNewFileToUri(context, sourceUri, file, time, false);
    }


    public static File getFinalSaveDirectory(Context context, Uri sourceUri) {
        File saveDirectory = SaveImage.getSaveDirectory(context, sourceUri);
        if ((saveDirectory == null) || !saveDirectory.canWrite()) {
            saveDirectory = new File(Environment.getExternalStorageDirectory(),
                    SaveImage.DEFAULT_SAVE_DIRECTORY);
        }
        // Create the directory if it doesn't exist
        if (!saveDirectory.exists())
            saveDirectory.mkdirs();
        return saveDirectory;
    }

    /**
     * If the <code>sourceUri</code> is a local content Uri, update the
     * <code>sourceUri</code> to point to the <code>file</code>.
     * At the same time, the old file <code>sourceUri</code> used to point to
     * will be removed if it is local.
     * If the <code>sourceUri</code> is not a local content Uri, then the
     * <code>file</code> will be inserted as a new content Uri.
     *
     * @return the final Uri referring to the <code>file</code>.
     */
    public static Uri linkNewFileToUri(Context context, Uri sourceUri,
                                       File file, long time, boolean deleteOriginal) {
        File oldSelectedFile = getLocalFileFromUri(context, sourceUri);
        final ContentValues values = getContentValues(context, sourceUri, file, time);

        Uri result = sourceUri;

        // In the case of incoming Uri is just a local file Uri (like a cached
        // file), we can't just update the Uri. We have to create a new Uri.
        boolean fileUri = isFileUri(sourceUri);

        if (fileUri || oldSelectedFile == null || !deleteOriginal) {
            result = context.getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } else {
            context.getContentResolver().update(sourceUri, values, null, null);
            if (oldSelectedFile.exists()) {
                oldSelectedFile.delete();
            }
        }
        return result;
    }

    private static ContentValues getContentValues(Context context, Uri sourceUri,
                                                  File file, long time) {
        final ContentValues values = new ContentValues();

        time /= 1000;
        values.put(MediaStore.Images.Media.TITLE, file.getName());
        values.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.DATE_TAKEN, time);
        values.put(MediaStore.Images.Media.DATE_MODIFIED, time);
        values.put(MediaStore.Images.Media.DATE_ADDED, time);
        values.put(MediaStore.Images.Media.ORIENTATION, 0);
        values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        values.put(MediaStore.Images.Media.SIZE, file.length());
        // This is a workaround to trigger the MediaProvider to re-generate the
        // thumbnail.
        values.put(MediaStore.Images.Media.MINI_THUMB_MAGIC, 0);

        final String[] projection = new String[]{
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.LATITUDE, MediaStore.Images.ImageColumns.LONGITUDE,
        };

        SaveImage.querySource(context, sourceUri, projection,
                new ContentResolverQueryCallback() {

                    @Override
                    public void onCursorResult(Cursor cursor) {
                        values.put(MediaStore.Images.Media.DATE_TAKEN, cursor.getLong(0));

                        double latitude = cursor.getDouble(1);
                        double longitude = cursor.getDouble(2);
                        // TODO: Change || to && after the default location
                        // issue is fixed.
                        if ((latitude != 0f) || (longitude != 0f)) {
                            values.put(MediaStore.Images.Media.LATITUDE, latitude);
                            values.put(MediaStore.Images.Media.LONGITUDE, longitude);
                        }
                    }
                });
        return values;
    }


    public static void querySource(Context context, Uri sourceUri, String[] projection,
                                   ContentResolverQueryCallback callback) {
        ContentResolver contentResolver = context.getContentResolver();
        querySourceFromContentResolver(contentResolver, sourceUri, projection, callback);
    }

    private static void querySourceFromContentResolver(
            ContentResolver contentResolver, Uri sourceUri, String[] projection,
            ContentResolverQueryCallback callback) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(sourceUri, projection, null, null,
                    null);
            if ((cursor != null) && cursor.moveToNext()) {
                callback.onCursorResult(cursor);
            }
        } catch (Exception e) {
            // Ignore error for lacking the data column from the source.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * @param sourceUri
     * @return true if the sourceUri is a local file Uri.
     */
    private static boolean isFileUri(Uri sourceUri) {
        String scheme = sourceUri.getScheme();
        if (scheme != null && scheme.equals(ContentResolver.SCHEME_FILE)) {
            return true;
        }
        return false;
    }

    private static File getSaveDirectory(Context context, Uri sourceUri) {
        File file = getLocalFileFromUri(context, sourceUri);
        if (file != null) {
            return file.getParentFile();
        } else {
            return null;
        }
    }


    private static File getLocalFileFromUri(Context context, Uri srcUri) {
        if (srcUri == null) {
            Log.e(LOGTAG, "srcUri is null.");
            return null;
        }

        String scheme = srcUri.getScheme();
        if (scheme == null) {
            Log.e(LOGTAG, "scheme is null.");
            return null;
        }

        final File[] file = new File[1];
        // sourceUri can be a file path or a content Uri, it need to be handled
        // differently.
        if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            if (srcUri.getAuthority().equals(MediaStore.AUTHORITY)) {
                querySource(context, srcUri, new String[]{
                                MediaStore.Images.ImageColumns.DATA
                        },
                        new ContentResolverQueryCallback() {

                            @Override
                            public void onCursorResult(Cursor cursor) {
                                file[0] = new File(cursor.getString(0));
                            }
                        });
            }
        } else if (scheme.equals(ContentResolver.SCHEME_FILE)) {
            file[0] = new File(srcUri.getPath());
        }
        return file[0];
    }

    public interface ContentResolverQueryCallback {
        void onCursorResult(Cursor cursor);
    }
}
