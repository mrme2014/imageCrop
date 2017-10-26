# imageCrop
# android6.0原生系统相册裁剪源码抽取改进

### 由于安卓手机各个Rom从4.0到7.0 相册功能各有差异,从4.0以后原生都不在支持圆形裁剪，而各个厂家可能，注意可能会把自己Rom中相册增加圆形裁剪功能。
### 这样的话，调用原生系统裁剪就会有兼容问题。

### 于是乎，我把android6.0原生系统相册裁剪源码抽取了出来并改进，使用方式沿用Intent意图。

- 支持圆形裁剪
- 支持裁剪框宽高最小值的设定
- 支持裁剪框网格是否显示
- 优化裁剪框缩放很小的时候，拖动不灵敏

### 矩形裁剪
![image](https://github.com/mrme2014/imageCrop/raw/master/art/1.gif)

### 圆形裁剪
![image](https://github.com/mrme2014/imageCrop/raw/master/art/2.gif)


### 使用方式

```java
        Intent intent = new Intent(this, CropActivity.class);//替换成“com.android.camera.action.CROP” 模拟器运行可查看原生裁剪是什么样子的
        intent.setDataAndType(getUri("/sdcard/download/1.png"), "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        intent.putExtra("scale", false);//看源码，加不加问题不大，不加还会快一些,默认false
        intent.putExtra("return-data", false);//是否返回bitmap,建议不要用true,图片过大会崩溃的,默认false
        intent.putExtra("scaleUpIfNeeded", false);    //  可避免莫名的黑边,加不加其实无所谓,默认false
        //intent.putExtra(CropActivity.MIN_CROP_WIDTH, 1080);  //矩形裁剪情况下的 最下宽度度值px ,默认是40px
        //intent.putExtra(CropActivity.MIN_CROP_HEIGHT, 300);//矩形裁剪情况下的 最下高度值px，默认是40px
        intent.putExtra(CropActivity.CIRCLE_CROP, true); //是否是圆形裁剪，默认false
        intent.putExtra(CropActivity.DRAW_GRID, true); //是否显示裁剪网格,默认false
        intent.putExtra(MediaStore.EXTRA_OUTPUT, getUri("/sdcard/output.png"));
        
        if (Build.VERSION.SDK_INT > 23) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        
        startActivityForResult(intent, 1);
```
