package com.qiaomu.cropimage;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.android.gallery3d.crop.CropActivity;

import java.io.File;
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        int check = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (check != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else {
            crop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        crop();
    }

    private void crop() {
        //还是按照原生Intent的使用方式，【MIN_CROP_WIDTH】  【MIN_CROP_HEIGHT】  【CIRCLE_CROP】  【DRAW_GRID】 新加的功能。
        // 【set-as-wallpaper】支持裁剪完后设置成壁纸,需要权限-----android.permission.SET_WALLPAPER
        // 其他的配置都是原生。

        Intent intent = new Intent(this, CropActivity.class);//
        intent.setDataAndType(getUri("/sdcard/download/1.png"), "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        intent.putExtra("scale", false);//看源码，加不加问题不大，不加还会快一些,默认false
        intent.putExtra("return-data", false);//是否返回bitmap,建议不要用true,图片过大会崩溃的,默认false
        intent.putExtra("scaleUpIfNeeded", false);    //  可避免莫名的黑边,加不加其实无所谓,默认false
        // intent.putExtra(CropActivity.MIN_CROP_WIDTH, 1080);  //矩形裁剪情况下的 最下宽度度值px ,默认是40px
        //  intent.putExtra(CropActivity.MIN_CROP_HEIGHT, 300);//矩形裁剪情况下的 最下高度值px，默认是40px
        intent.putExtra(CropActivity.CIRCLE_CROP, true); //是否是圆形裁剪，默认false
        intent.putExtra(CropActivity.DRAW_GRID, true); //是否显示裁剪网格,默认false
        intent.putExtra(MediaStore.EXTRA_OUTPUT, getUri("/sdcard/output.png"));


        if (Build.VERSION.SDK_INT > 23) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        startActivityForResult(intent, 1);
    }

    private Uri getUri(String filepath) {
        if (Build.VERSION.SDK_INT >= 24) {
            //7.0以上的读取文件uri要用这种方式了
            return FileProvider.getUriForFile(this, "com.qiaomu.fileprovider", new File(filepath));
        } else {
            return Uri.parse("file://" + filepath);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
//                从剪切图片返回的数据
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        Uri data1 = data.getData();
                        try {
                            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(getUri("/sdcard/output.png")));
                            if (bitmap != null) {
                                ImageView image = (ImageView) findViewById(R.id.image);
                                image.setImageBitmap(bitmap);
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
