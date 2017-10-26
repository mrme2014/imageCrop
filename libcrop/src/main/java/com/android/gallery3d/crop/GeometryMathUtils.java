package com.android.gallery3d.crop;

/**
 * Created by qiaomu on 2017/10/24.
 */

public class GeometryMathUtils {

    public static float clamp(float i, float low, float high) {
        return Math.max(Math.min(i, high), low);
    }

    public static float vectorLength(float[] a) {
        return (float) Math.hypot(a[0], a[1]);
    }

    public static float[] shortestVectorFromPointToLine(float[] point, float[] line) {
        float x1 = line[0];
        float x2 = line[2];
        float y1 = line[1];
        float y2 = line[3];
        float xdelt = x2 - x1;
        float ydelt = y2 - y1;
        if (xdelt == 0 && ydelt == 0)
            return null;
        float u = ((point[0] - x1) * xdelt + (point[1] - y1) * ydelt)
                / (xdelt * xdelt + ydelt * ydelt);
        float[] ret = {
                (x1 + u * (x2 - x1)), (y1 + u * (y2 - y1))
        };
        float[] vec = {
                ret[0] - point[0], ret[1] - point[1]
        };
        return vec;
    }


    public static float[] normalize(float[] a) {
        float length = (float) Math.hypot(a[0], a[1]);
        float[] b = {
                a[0] / length, a[1] / length
        };
        return b;
    }

    // A onto B
    public static float scalarProjection(float[] a, float[] b) {
        float length = (float) Math.hypot(b[0], b[1]);
        return dotProduct(a, b) / length;
    }
    // A . B
    public static float dotProduct(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1];
    }


    public static float[] lineIntersect(float[] line1, float[] line2) {
        float a0 = line1[0];
        float a1 = line1[1];
        float b0 = line1[2];
        float b1 = line1[3];
        float c0 = line2[0];
        float c1 = line2[1];
        float d0 = line2[2];
        float d1 = line2[3];
        float t0 = a0 - b0;
        float t1 = a1 - b1;
        float t2 = b0 - d0;
        float t3 = d1 - b1;
        float t4 = c0 - d0;
        float t5 = c1 - d1;

        float denom = t1 * t4 - t0 * t5;
        if (denom == 0)
            return null;
        float u = (t3 * t4 + t5 * t2) / denom;
        float[] intersect = {
                b0 + u * t0, b1 + u * t1
        };
        return intersect;
    }
}
