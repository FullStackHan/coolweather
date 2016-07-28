package com.example.test.coolweather.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.test.coolweather.R;
import com.example.test.coolweather.model.City;
import com.example.test.coolweather.model.CoolWeatherDB;
import com.example.test.coolweather.model.County;
import com.example.test.coolweather.model.Province;
import com.example.test.coolweather.util.HttpCallbackListener;
import com.example.test.coolweather.util.HttpUtil;
import com.example.test.coolweather.util.Utility;

import java.util.ArrayList;
import java.util.List;

public class ChooseAreaActivity extends Activity {
    public static final int LEVEL_PROCINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    //判断是否从WeatherActivity跳转过来
    private boolean isFromWeatherActivity;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private CoolWeatherDB coolWeatherDB;
    private List<String> dataList = new ArrayList<String>();
    private  List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;

    //选中的省县
    private Province selectProvince;
    private City selectCity;
    //当前选中的级别
    private int currentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity",false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //已经选择了城市并且不是从WeatherActivity跳转过来，才会跳转到weatheractivity
        if(prefs.getBoolean("city_selected",false) && !isFromWeatherActivity){
            Intent intent = new Intent(this,WeatherActivity.class);
            startActivity(intent);
            finish();
            return;

        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);
        listView=(ListView)findViewById(R.id.list_view);
        titleText=(TextView)findViewById(R.id.title_text);
        adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        coolWeatherDB=CoolWeatherDB.getInstance(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long arg3) {
                if(currentLevel==LEVEL_PROCINCE){
                    selectProvince=provinceList.get(index);
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){

                    selectCity=cityList.get(index);
                    queryCounties();
                }else if (currentLevel ==LEVEL_COUNTY){
                    String countyCode = countyList.get(index).getCountyCode();
                    Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
                    intent.putExtra("county_code",countyCode);
                    startActivity(intent);
                    finish();
                }

            }
        });
        queryProvinces();
    }

    /**
     * 查询全国所有的省 优先从数据库查询 若没有查询到再去服务器上查询
     */
    private void queryProvinces() {
        provinceList=coolWeatherDB.loadProvince();
        if(provinceList.size()>0){
            dataList.clear();
            for (Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("中国");
            currentLevel = LEVEL_PROCINCE;
        }else{
            queryFromServer(null,"province");
        }

    }

    /**
     * 查询选中市内所有的县 若数据库没有再到服务器查询
     */
    private void queryCounties() {
        countyList=coolWeatherDB.loadCounties(selectCity.getId());
        if(countyList.size()>0){
            dataList.clear();
            for (County county : countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectCity.getCityName());
            currentLevel=LEVEL_COUNTY;

        }else{
            queryFromServer(selectCity.getCityCode(),"county");
        }
    }


    /**
     * 查询选中省内所有的市   若数据库没有再到服务器查询
     */
    private void queryCities() {
        cityList = coolWeatherDB.loadCities(selectProvince.getId());
        if(cityList.size()>0){
            dataList.clear();
            for (City city : cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectProvince.getProvinceName());
            currentLevel=LEVEL_CITY;

        }else{
            queryFromServer(selectProvince.getProvinceCode(),"city");
        }


    }


    /**
     * 根据传入的代号和类型从服务器上查询省市县数据
     *
     */

    private void queryFromServer(final String code, final String type) {
        String address;
        if(!TextUtils.isEmpty(code)) {
            address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
        }else{
            address = "http://www.weather.com.cn/data/list3/city.xml";
        }
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;
                if("province".equals(type)){
                    result= Utility.handleProvincesResponse(coolWeatherDB,response);
                }else if ("city".equals(type)){
                    result=Utility.handleCityResponse(coolWeatherDB,response,selectProvince.getId());
                }else if ("county".equals(type)){
                    result=Utility.handleCountyResponse(coolWeatherDB,response,selectCity.getId());
                }
                if(result){
                    //通过runOnUIThread（）方法回到主线程 处理逻辑
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                //通过runONUIThread（）方法回到主线程处理逻辑
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });


    }

    /**
     * 关闭进度对话框
     */

    private void closeProgressDialog() {
        if (progressDialog!=null){
            progressDialog.dismiss();
        }

    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog==null){
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载。。。");
            progressDialog.setCanceledOnTouchOutside(false);
        }

        progressDialog.show();
    }
    /**
     *back按键，根据当前级别来判断，应该返回市列表 省列表 还是直接退出
     */

    @Override
    public void onBackPressed() {
        if (currentLevel == LEVEL_COUNTY){
            queryCities();
        }else if (currentLevel == LEVEL_CITY){
            queryProvinces();
        }else{
            if (isFromWeatherActivity){
                Intent intent = new Intent(this,WeatherActivity.class);
                startActivity(intent);
            }
            finish();

        }

    }
}
