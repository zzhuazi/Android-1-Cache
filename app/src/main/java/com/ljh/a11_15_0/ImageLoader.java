package com.ljh.a11_15_0;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by Administrator on 2017/11/16.
 */

public class ImageLoader {
    private static final String TAG = ImageLoader.class.getSimpleName();
    private static Context context;
    private static int loadingImageRes;
    private static int errorImageRes;

    private ImageLoader(Context context, int loadingImageRes, int errorImageRes) {
        this.context = context;
        this.loadingImageRes = loadingImageRes;
        this.errorImageRes = errorImageRes;
    }

    private static ImageLoader imageLoader = new ImageLoader(context,loadingImageRes, errorImageRes);

    public static ImageLoader getImageLoader() {
        return imageLoader;
    }

    //用于缓存bitmap的容器对象
    private Map<String, Bitmap> cacheMap = new HashMap<String, Bitmap>();

    public void loadImage(String imagePath, ImageView imageView){
        //将需要显示的图片url保存到视图上
        imageView.setTag(imagePath);

        //根据url从一级缓存中去对应的bitmap对象
        //如果有，显示（结束）
        //如果没有，则进入二级缓存
        Bitmap bitmap = getFromFirstCache(imagePath);
        if (bitmap != null){
            imageView.setImageBitmap(bitmap);
            return ;
        }

        //从二级缓存中查找，得到文件名并在sd卡的缓存目录下加载对应的图片得到bitmap对象
        //如果有，显示，结束
        //如果没有，进入三级缓存（网络请求）
        bitmap = getFromSecondCache(imagePath);
        if (bitmap != null){
            imageView.setImageBitmap(bitmap);
            cacheMap.put(imagePath,bitmap);
            return;
        }

        //显示代表提示正在加载的图片，启动分线程联网请求得到的Bitmap对象
        //如果有，显示（主线程），缓存到一二级缓存（分线程）中，结束
        //如果没有，显示提示错误图片，结束
        loadBitmapFromThridCache(imagePath, imageView);
    }

    private void loadBitmapFromThridCache(final String imagePath, final ImageView imageView){
        new AsyncTask<Void, Void ,Bitmap>() {
            @Override
            protected void onPreExecute() {
                imageView.setImageResource(loadingImageRes);
            }

            @Override
            protected Bitmap doInBackground(Void... voids) {
                Bitmap bitmap = null;
                try {
                    //在准备请求服务器图片之前，判断是否需要加载
                    String newImagePath = (String) imageView.getTag();
                    //视图已经被复用
                    if (newImagePath != imagePath){
                        return null;
                    }
                    //得到连接
                    URL url =new URL(imagePath);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    //设置连接
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    //连接
                    connection.connect();
                    //发请求读取返回的数据并封装成Bitmap
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200){
                        InputStream is = connection.getInputStream();
                        //将is封装成Bitmap
                        bitmap = BitmapFactory.decodeStream(is);
                        is.close();
                        if (bitmap != null){
                            //缓存到一级缓存（分线程）
                            cacheMap.put(imagePath,bitmap);
                            //缓存到二级缓存（分线程）
                            String filesPath = context.getExternalFilesDir(null).getAbsolutePath();
                            String fileName = imagePath.substring(imagePath.lastIndexOf("/") + 1);
                            String filePath = filesPath + "/" + fileName;
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(filePath));
                        }
                    }
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                //从联网请求图片到得到对象需要一定的时间,视图可能被复用了,不需要显示
                //在主线程准备显示图片之前,需要判断是否需要显示
                String newImagePath = (String)imageView.getTag();
                if(newImagePath!=imagePath){
                    //视图已经被复用了
                    return;
                }

                //如果没有:显示提示错误的图片(结束)
                if(bitmap == null){
                    imageView.setImageResource(errorImageRes);
                }else{
                    //如果有,显示
                    imageView.setImageBitmap(bitmap);
                }
            }
        }.execute();
    }

    //根据图片url从二级缓存中取相对应的Bitmap对象
    private Bitmap getFromSecondCache(String imagePath){
        Log.i(TAG, "getFromSecondCache: 2222");
        String filesPath = context.getExternalFilesDir(null).getAbsolutePath();
        String fileName = imagePath.substring(imagePath.lastIndexOf("/") + 1);
        String filePath = filesPath + "/" +fileName;
        return BitmapFactory.decodeFile(filePath);
    }

    //根据图片url从一级缓存中取对应的bitmap对象
    private Bitmap getFromFirstCache(String imagePath){
        Log.i(TAG, "getFromFirstCache: 1111");
        return cacheMap.get(imagePath);
    }

    public static void setContext(Context context) {
        ImageLoader.context = context;
    }

    public static void setLoadingImageRes(int loadingImageRes) {
        ImageLoader.loadingImageRes = loadingImageRes;
    }

    public static void setErrorImageRes(int errorImageRes) {
        ImageLoader.errorImageRes = errorImageRes;
    }
}
