package com.hkk.hi.weather;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hkk.hi.weather.data.Weather;
import com.hkk.hi.weather.utils.MyHttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hi on 2016/8/3.
 */
public class MainActivity extends Activity {

    private TextView city_tv, pm_tv, pm_color_tv, temp_tv, wind_tv, date_tv, weather_tv;
    private TextView item_weather_tv, item_week_tv, item_temp_tv;
    private ImageView refresh_iv, search_iv;
    private HorizontalScrollView weather_other_sv;
    private LinearLayout weather_other_ll;

    private static String BASE_URL = "http://weather.123.duba.net/static/weather_info/";
    private int cityCode = 101200101;

    private MyHandler myHandler = new MyHandler();
    private List<Weather> weatherList;   //用数组存储一周的天气情况
    private String[] weekStr = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView(); //初始化控件
        getData();  //获取天气数据
        MyClickListener listener = new MyClickListener();
        refresh_iv.setOnClickListener(listener);
        search_iv.setOnClickListener(listener);
    }

    private class MyClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.refresh_iv:
                    refresh_iv.setBackgroundResource(R.drawable.refresh);
                    final AnimationDrawable animation = (AnimationDrawable) refresh_iv.getBackground();
                    animation.start();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animation.stop();
                            getData();
                        }
                    }, 1000);
                    break;
                case R.id.search_iv:
                    Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                    startActivityForResult(intent, 100);
                    break;
            }
        }
    }

    /**
     * 更新界面数据
     */
    public class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            city_tv.setText(weatherList.get(0).getCity());
            pm_tv.setText(weatherList.get(0).getPm());
            temp_tv.setText(weatherList.get(0).getTemp());
            wind_tv.setText(weatherList.get(0).getWind());
            date_tv.setText(weatherList.get(0).getDate());
            weather_tv.setText(weatherList.get(0).getWeather());
            setPmColor(weatherList.get(0).getPm_color());
            weather_other_ll.removeAllViews();

            for (int i = 1; i < 6; i++) {
                Weather weather = weatherList.get(i);
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item, null);
                item_temp_tv = (TextView) view.findViewById(R.id.item_temp_tv);
                item_weather_tv = (TextView) view.findViewById(R.id.item_weather_tv);
                item_week_tv = (TextView) view.findViewById(R.id.item_week_tv);
                item_temp_tv.setText(weather.getItem_temp());
                item_weather_tv.setText(weather.getItem_weather());
                item_week_tv.setText(weather.getItem_week());
                weather_other_ll.addView(view);
            }

        }
    }
    private void setPmColor(int pm) {
        if (pm <= 50) {
            pm_color_tv.setBackgroundColor(getResources().getColor(R.color.green));
        } else if (pm <= 100) {
            pm_color_tv.setBackgroundColor(getResources().getColor(R.color.yellow));
        } else if (pm <= 150) {
            pm_color_tv.setBackgroundColor(getResources().getColor(R.color.orange));
        } else if (pm <= 200) {
            pm_color_tv.setBackgroundColor(getResources().getColor(R.color.red));
        } else if (pm <= 300) {
            pm_color_tv.setBackgroundColor(getResources().getColor(R.color.purple));
        } else {
            pm_color_tv.setBackgroundColor(getResources().getColor(R.color.brown));
        }
    }
    private List<Weather> getData() {
        weatherList = new ArrayList<Weather>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                URL url = null;
                try {
                    url = new URL(BASE_URL + cityCode + ".html");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                MyHttpUtil myHttpUtil = new MyHttpUtil();
                String data = myHttpUtil.getData(url);
                data = data.substring(data.indexOf("(") + 1, data.indexOf(")"));
                try {
                    JSONObject rootObject = new JSONObject(data); //得到JSON数据的根对象
                    JSONObject jsObject = rootObject.getJSONObject("weatherinfo"); //取出weatherinfo对应的值

                    for (int i = 0; i < 6; i++) {
                        Weather weather = new Weather();
                        if (i == 0) {
                            weather.setCity(jsObject.getString("city"));
                            weather.setPm("PM:" + jsObject.getString("pm") + " " + jsObject.getString("pm-level"));
                            weather.setTemp(jsObject.getString("temp") + "°");
                            weather.setWind(jsObject.getString("wd") + "" + jsObject.getString("ws"));
                            weather.setDate(jsObject.getString("date_y") + " " + jsObject.getString("week"));
                            weather.setWeather(jsObject.getString("weather1") + " " + jsObject.getString("temp1"));
                            weather.setPm_color(jsObject.getInt("pm"));
                            for (int j = 0; j < 7; j++) {
                                if (weekStr[j].equals(jsObject.getString("week"))) {
                                    weather.setWeek(j);
                                }
                            }
                            weatherList.add(weather);
                        } else {
                            setWeek(weather, i);
                            weather.setItem_weather(jsObject.getString("weather" + (i + 1)));
                            weather.setItem_temp(jsObject.getString("temp" + (i + 1)));
                            weatherList.add(weather);
                        }
                    }
                    Message message = Message.obtain();
                    myHandler.sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }


        }).start();
        return weatherList;
    }

    private void setWeek(Weather weather, int i) {
        int a = (weatherList.get(0).getWeek() + i) % 7;
        switch (a) {
            case 0:
                weather.setItem_week("星期日");
                break;
            case 1:
                weather.setItem_week("星期一");
                break;
            case 2:
                weather.setItem_week("星期二");
                break;
            case 3:
                weather.setItem_week("星期三");
                break;
            case 4:
                weather.setItem_week("星期四");
                break;
            case 5:
                weather.setItem_week("星期五");
                break;
            case 6:
                weather.setItem_week("星期六");
                break;
        }
    }

    private void initView() {
        city_tv = (TextView) findViewById(R.id.city_tv);
        pm_tv = (TextView) findViewById(R.id.pm_tv);
        pm_color_tv = (TextView) findViewById(R.id.pm_color_tv);
        temp_tv = (TextView) findViewById(R.id.temp_tv);
        wind_tv = (TextView) findViewById(R.id.wind_tv);
        date_tv = (TextView) findViewById(R.id.date_tv);
        weather_tv = (TextView) findViewById(R.id.weather_tv);
        refresh_iv = (ImageView) findViewById(R.id.refresh_iv);
        search_iv = (ImageView) findViewById(R.id.search_iv);

        weather_other_sv = (HorizontalScrollView) findViewById(R.id.weather_other_sv);
        weather_other_ll = (LinearLayout) findViewById(R.id.weather_other_ll);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 200 && requestCode == 100) {
            cityCode = data.getIntExtra("CODE", 0);
            getData();
        }
    }
}
