package com.ljh.a11_15_0;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int WHAT_REQUEST_SUCCESS = 1;
    static final int WHAT_REQUEST_ERRO = 2;
    private static final String TAG = MainActivity.class.getSimpleName();
    ListView lv_main;
    LinearLayout ll_main_loading;
    List<ShopInfo> data;
    ShopInfoAdapter adapter;



    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WHAT_REQUEST_SUCCESS:
                    ll_main_loading.setVisibility(View.GONE);
                    lv_main.setAdapter(adapter);
                    break;
                case WHAT_REQUEST_ERRO:
                    ll_main_loading.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "加载数据失败", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lv_main = findViewById(R.id.lv_main);
        ll_main_loading = findViewById(R.id.ll_main_loading);

        adapter = new ShopInfoAdapter();

        //主线程，显示提示视图
        ll_main_loading.setVisibility(View.VISIBLE);
        new Thread() {
            @Override
            public void run() {
                try {
                    String jsonString = requestJson();
                    data = new Gson().fromJson(jsonString, new TypeToken<List<ShopInfo>>() {
                    }.getType());
                    //发送请求成功信息
                    handler.sendEmptyMessage(WHAT_REQUEST_SUCCESS);
                } catch (Exception e) {
                    e.printStackTrace();
                    //发送请求失败的信息
                    handler.sendEmptyMessage(WHAT_REQUEST_ERRO);
                }
            }
        }.start();
    }

    private String requestJson() throws Exception {
        String result = null;
        String path = "http://192.168.137.1:8080/Web_11_15_0/shopInfoListServlet";
        URL url = new URL(path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = -1;
            while ((len = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            baos.close();
            inputStream.close();
            connection.disconnect();
            result = baos.toString();
        }
        return result;
    }

    private class ShopInfoAdapter extends BaseAdapter {
       /* private ImageLoader imageLoader;
        public ShopInfoAdapter(){
            imageLoader = new ImageLoader(MainActivity.this, R.drawable.loading, R.drawable.error);
        }*/
       ImageLoader imageLoader = ImageLoader.getImageLoader();

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int i) {
            return data.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            //得到当前行的数据对象
            ShopInfo shopInfo = data.get(i);
            View view;
            ViewHolder viewHolder;
            if(convertView == null){
                view = View.inflate(MainActivity.this,R.layout.item_main,null);
                viewHolder = new ViewHolder();
                viewHolder.nameTV = view.findViewById(R.id.tv_item_name);
                viewHolder.priceTV = view.findViewById(R.id.tv_item_price);
                viewHolder.imageView = view.findViewById(R.id.iv_item_icon);
                view.setTag(viewHolder);
            }else{
                view = convertView;
                viewHolder = (ViewHolder) view.getTag();
            }
            //设置数据
            viewHolder.nameTV.setText(shopInfo.getName());
            viewHolder.priceTV.setText(shopInfo.getPrice() + "元");
            String imagePath = shopInfo.getImagePath();
            //根据图片路径启动分线程动态请求服务加载图片并显示
            imageLoader.setContext(MainActivity.this);
            imageLoader.setLoadingImageRes(R.drawable.loading);
            imageLoader.setErrorImageRes(R.drawable.error);
            imageLoader.loadImage(imagePath,viewHolder.imageView);
            return view;
        }
    }

    class ViewHolder{
        TextView nameTV;
        TextView priceTV;
        ImageView imageView;
    }
}
