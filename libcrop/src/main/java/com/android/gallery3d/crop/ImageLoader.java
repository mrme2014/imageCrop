package com.android.gallery3d.crop;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by qiaomu on 2017/10/24.
 */

public class ImageLoader {
    private static final String LOGTAG = "ImageLoader";


    public static final String JPEG_MIME_TYPE = "image/jpeg";
    public static final int DEFAULT_COMPRESS_QUALITY = 95;

    public static final int ORI_NORMAL = ExifInterface.ORIENTATION_NORMAL;
    public static final int ORI_ROTATE_90 = ExifInterface.ORIENTATION_ROTATE_90;
    public static final int ORI_ROTATE_180 = ExifInterface.ORIENTATION_ROTATE_180;
    public static final int ORI_ROTATE_270 = ExifInterface.ORIENTATION_ROTATE_270;
    public static final int ORI_FLIP_HOR = ExifInterface.ORIENTATION_FLIP_HORIZONTAL;
    public static final int ORI_FLIP_VERT = ExifInterface.ORIENTATION_FLIP_VERTICAL;
    public static final int ORI_TRANSPOSE = ExifInterface.ORIENTATION_TRANSPOSE;
    public static final int ORI_TRANSVERSE = ExifInterface.ORIENTATION_TRANSVERSE;

    private static final int BITMAP_LOAD_BACKOUT_ATTEMPTS = 5;
    private static final float OVERDRAW_ZOOM = 1.2f;

    /**
     * Loads a bitmap at a given URI that is downsampled so that both sides are
     * smaller than maxSideLength. The Bitmap's original dimensions are stored
     * in the rect originalBounds.
     *
     * @param uri            URI of image to open.
     * @param context        context whose ContentResolver to use.
     * @param maxSideLength  max side length of returned bitmap.
     * @param originalBounds If not null, set to the actual bounds of the stored bitmap.
     * @param useMin         use min or max side of the original image
     * @return downsampled bitmap or null if this operation failed.
     */
    public static Bitmap loadConstrainedBitmap(Uri uri, Context context, int maxSideLength,
                                               Rect originalBounds, boolean useMin) {
        if (maxSideLength <= 0 || uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to getScaledBitmap");
        }
        // Get width and height of stored bitmap
        Rect storedBounds = loadBitmapBounds(context, uri);
        if (originalBounds != null) {
            originalBounds.set(storedBounds);
        }
        int w = storedBounds.width();
        int h = storedBounds.height();

        // If bitmap cannot be decoded, return null
        if (w <= 0 || h <= 0) {
            return null;
        }

        // Find best downsampling size
        int imageSide = 0;
        if (useMin) {
            imageSide = Math.min(w, h);
        } else {
            imageSide = Math.max(w, h);
        }
        int sampleSize = 1;
        while (imageSide > maxSideLength) {
            imageSide >>>= 1;
            sampleSize <<= 1;
        }

        // Make sure sample size is reasonable
        if (sampleSize <= 0 ||
                0 >= (int) (Math.min(w, h) / sampleSize)) {
            return null;
        }
        return loadDownsampledBitmap(context, uri, sampleSize);
    }


    /**
     * Returns the bounds of the bitmap stored at a given Url.
     */
    public static Rect loadBitmapBounds(Context context, Uri uri) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        loadBitmap(context, uri, o);
        return new Rect(0, 0, o.outWidth, o.outHeight);
    }

    /**
     * Loads a bitmap that has been downsampled using sampleSize from a given url.
     */
    public static Bitmap loadDownsampledBitmap(Context context, Uri uri, int sampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inSampleSize = sampleSize;
        return loadBitmap(context, uri, options);
    }

    /**
     * Returns the bitmap from the given uri loaded using the given options.
     * Returns null on failure.
     */
    public static Bitmap loadBitmap(Context context, Uri uri, BitmapFactory.Options o) {
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to loadBitmap");
        }
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(is, null, o);
        } catch (FileNotFoundException e) {
            Log.e(LOGTAG, "FileNotFoundException for " + uri, e);
        } finally {
            Utils.closeSilently(is);
        }
        return null;
    }


    /**
     * Returns the rotation of image at the given URI as one of 0, 90, 180,
     * 270.  Defaults to 0.
     */
    public static int getMetadataRotation(Context context, Uri uri) {
        int orientation = getMetadataOrientation(context, uri);
        switch (orientation) {
            case ORI_ROTATE_90:
                return 90;
            case ORI_ROTATE_180:
                return 180;
            case ORI_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    /**
     * Returns the image's orientation flag.  Defaults to ORI_NORMAL if no valid
     * orientation was found.
     */
    public static int getMetadataOrientation(Context context, Uri uri) {
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to getOrientation");
        }

        // First try to find orientation data in Gallery's ContentProvider.
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri,
                    new String[]{MediaStore.Images.ImageColumns.ORIENTATION},
                    null, null, null);
            if (cursor != null && cursor.moveToNext()) {
                int ori = cursor.getInt(0);
                switch (ori) {
                    case 90:
                        return ORI_ROTATE_90;
                    case 270:
                        return ORI_ROTATE_270;
                    case 180:
                        return ORI_ROTATE_180;
                    default:
                        return ORI_NORMAL;
                }
            }
        } catch (SQLiteException e) {
            // Do nothing
        } catch (IllegalArgumentException e) {
            // Do nothing
        } catch (IllegalStateException e) {
            // Do nothing
        } catch (Exception e) {

        } finally {
            if (cursor != null) cursor.close();
        }
        ExifInterface exif = null;
        InputStream is = null;
        // Fall back to checking EXIF tags in file or input stream.
        try {
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                String mimeType = getMimeType(uri);
                if (!JPEG_MIME_TYPE.equals(mimeType)) {
                    return ORI_NORMAL;
                }
                String path = uri.getPath();
                exif = new ExifInterface(path);
                return parseExif(exif);
                //exif.readExif(path);
            }
        } catch (IOException e) {
            Log.w(LOGTAG, "Failed to read EXIF orientation", e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                Log.w(LOGTAG, "Failed to close InputStream", e);
            }
        }
        return ORI_NORMAL;
    }

    private static int parseExif(ExifInterface exif) {
        Integer tagval = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ORI_NORMAL);
        if (tagval != null) {
            int orientation = tagval;
            switch (orientation) {
                case ORI_NORMAL:
                case ORI_ROTATE_90:
                case ORI_ROTATE_180:
                case ORI_ROTATE_270:
                case ORI_FLIP_HOR:
                case ORI_FLIP_VERT:
                case ORI_TRANSPOSE:
                case ORI_TRANSVERSE:
                    return orientation;
                default:
                    return ORI_NORMAL;
            }
        }
        return ORI_NORMAL;
    }

    /**
     * Returns the Mime type for a Url.  Safe to use with Urls that do not
     * come from Gallery's content provider.
     */
    public static String getMimeType(Uri src) {
        String postfix = MimeTypeMap.getFileExtensionFromUrl(src.toString());
        String ret = null;
        if (postfix != null) {
            ret = MimeTypeMap.getSingleton().getMimeTypeFromExtension(postfix);
        }
        return ret;
    }
}
