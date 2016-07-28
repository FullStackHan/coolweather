package com.example.test.coolweather.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.test.coolweather.R;
import com.example.test.coolweather.receive.AutoUpdateService;
import com.example.test.coolweather.util.HttpCallbackListener;
import com.example.test.coolweather.util.HttpUtil;
import com.example.test.coolweather.util.Utility;

public class WeatherActivity extends Activity implements View.OnClickListener {

    private LinearLayout weatherInfoLayout;
    //显示城市名称
    private TextView cityNameText;
    //display publishing time
    private TextView publishTime;
    //display weather Description描述
    private TextView weatherDespText;
    //显示最高气温
    private TextView temp1Text;
    //显示最低气温
    private  TextView temp2Text;
    //显示当前日期
    private TextView currentDataText;


    private Button switchCity;
    private Button refreshWeather;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.weather_activity);

        //初始化各个控件
        switchCity =(Button)findViewById(R.id.switch_city);
        refreshWeather = (Button)findViewById(R.id.refresh_weather);
        switchCity.setOnClickListener(this);
        refreshWeather.setOnClickListener(this);

        weatherInfoLayout = (LinearLayout)findViewById(R.id.weather_info_layout);
        cityNameText = (TextView)findViewById(R.id.city_name);
        publishTime = (TextView)findViewById(R.id.publish_text);
        weatherDespText = (TextView)findViewById(R.id.weather_desp);
        temp1Text = (TextView)findViewById(R.id.temp1);
        temp2Text = (TextView)findViewById(R.id.temp2);
        currentDataText = (TextView)findViewById(R.id.current_data);
        String countyCode = getIntent().getStringExtra("county_code");
        if (!TextUtils.isEmpty(countyCode)){
            //有获得县级的代号时就去查询天气
            publishTime.setText("同步中····");
            weatherInfoLayout.setVisibility(View.INVISIBLE);
            cityNameText.setVisibility(View.INVISIBLE);
            queryWeatherCode(countyCode);

        }else{
            //没有县级代号，直接显示本地天气
            showWeather();
        }
    }

    /**
     * 查询县级代号所对应的天气代号
     *
     */

    private void queryWeatherCode(String countyCode) {
        String address = "http://www.weather.com.cn/data/list3/city"+countyCode+".xml";
        queryFromserver(address,"countyCode");

    }

    /**
     *查询天气代号所对应的天气
     */
    private void queryWeatherInfo(String weatherCode){
        String address= "http://www.weather.com.cn/data/cityinfo/"+weatherCode+".html";
        queryFromserver(address,"weatherCode");

    }



    /**
     *根据传入的地址和类型去向服务器查询天气的代号或天气的信息
     */
    private void queryFromserver(final String address,final String type) {
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(final String response) {
                if("countyCode".equals(type)){
                    if(!TextUtils.isEmpty(response)){
                        //从服务器返回的数据中解析出天气代号
                        String[] array=response.split("\\|");
                        if(array !=null&&array.length==2){
                            String weatherCode = array[1];
                            queryWeatherInfo(weatherCode);
                        }

                    }
                }else if("weatherCode".equals(type)){
                    //处理数据库返回的天气信息 handleWeatherResponse（）方法将返回的Json数据解析并保存在SharedPreference文件中
                    Utility.handleWeatherResponse(WeatherActivity.this,response);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showWeather();
                        }
                    });

                }

            }

            @Override
            public void onError(Exception e) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        publishTime.setText("同步失败");
                    }
                });
            }
        });

    }

    /**
     *
     * 从SharedPreferences文件中读取储存的天气信息，并显示到界面上
     */

    private void showWeather() {
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        cityNameText.setText(prefs.getString("city_name",""));
        temp1Text.setText(prefs.getString("temp1",""));
        temp2Text.setText(prefs.getString("temp2",""));
        weatherDespText.setText(prefs.getString("weather_desp",""));
        publishTime.setText("今天"+prefs.getString("publish_time","")+"发布");
        currentDataText.setText(prefs.getString("current_data",""));
        weatherInfoLayout.setVisibility(View.VISIBLE);
        cityNameText.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.switch_city:
                Intent intent =new Intent(this,ChooseAreaActivity.class);
                intent.putExtra("from_weather_activity",true);
                startActivity(intent);
                finish();
                break;

            case R.id.refresh_weather:
                publishTime.setText("同步中···");
                SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
                String weatherCode = prefs.getString("weather_Code","");
                if (!TextUtils.isEmpty(weatherCode)){
                    queryWeatherInfo(weatherCode);
                }
                break;
            default:
                break;
        }
    }
}
