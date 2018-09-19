package com.example.haze;

import android.Manifest;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.AVSaveOption;
import com.avos.avoscloud.CountCallback;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.GetCallback;
import com.avos.avoscloud.SaveCallback;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.example.haze.db.City;
import com.example.haze.db.County;
import com.example.haze.db.Province;
import com.example.haze.domain.Address;
import com.example.haze.gson.Weather;
import com.example.haze.util.HttpUtil;
import com.example.haze.util.Utility;


import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 遍历省市县数据
 */
public class ChooseAreaFragment extends Fragment{
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTY=2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList=new ArrayList<>();
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    private Province selectedProvince;//选中省份
    private City selectedCity;//选中市
    private County selectedCounty;//选中县城
    private int currentLevel;//当前选中级别
    private LocationClient locationClient;//定位客户端
    private Button locateButton;//定位按钮
    private TextView locationText;//定位城市信息
    private Address address;
    private Button queryButton;//查询天气按钮

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.choose_area,container,false);
        titleText=view.findViewById(R.id.titleText);
        backButton=view.findViewById(R.id.backButton);
        locateButton=view.findViewById(R.id.locateButton);
        queryButton=view.findViewById(R.id.queryButton);
        locationText=view.findViewById(R.id.location_city);
        listView=view.findViewById(R.id.listView);
        adapter=new ArrayAdapter<>(getActivity(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        // 初始化参数依次为 this, AppId, AppKey
        AVOSCloud.initialize(getActivity(),"O5vAdJP2oEStLVKzrm7jKK68-gzGzoHsz","DcyztGAXHWKtvEGTo7nuwcCo");
        locateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationClient=new LocationClient(getActivity());
                locationClient.registerLocationListener(new LocationListener());
                getRequestPermisssions();
            }
        });
        queryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(locationText.getText()==""||locationText.getText()=="未知"||address==null){
                    Toast.makeText(getActivity(),"请先定位所在城市",Toast.LENGTH_SHORT).show();
                }else{
                    //学校课程要求，闲的蛋疼，非要去服务器再去取一下
                    AVQuery<AVObject> query=new AVQuery<>("Address");
                    // 按时间，降序排列
                    query.orderByDescending("updatedAt");
                    //找出最近更新的城市
                    final Address address_server=new Address();
                    query.getFirstInBackground(new GetCallback<AVObject>() {
                        @Override
                        public void done(AVObject avObject, AVException e) {
                            if(e==null){
                                //获取城市天气id
                                address_server.setAddress(avObject.get("address").toString());
                                Log.d("leanCloud城市天气id", "done: "+avObject.get("cid"));
                                address_server.setCid(avObject.get("cid").toString());
                            }
                        }
                    });
                    //获取城市id
                    String weatherId=address.getCid();
                    Log.d("WeatherId", " "+weatherId);
                    if(getActivity() instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        WeatherActivity activity=(WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefreshLayout.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel==LEVEL_PROVINCE){
                    selectedProvince=provinceList.get(position);
                    queryCities();
                }
                else if(currentLevel==LEVEL_CITY){
                    selectedCity=cityList.get(position);
                    queryCounties();
                }else if(currentLevel==LEVEL_COUNTY){
                    String weatherId=countyList.get(position).getWeatherId();
                    Log.d("WeatherId", " "+weatherId+":"+countyList.get(position).getCountyName());
                    if(getActivity() instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        WeatherActivity activity=(WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefreshLayout.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }
                }

            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel==LEVEL_COUNTY){
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }
    /**
     * 查询全国所有的省份
     */
    private void queryProvinces(){
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList= DataSupport.findAll(Province.class);
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province:provinceList)
                dataList.add(province.getProvinceName());
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else{
            String address="http://guolin.tech/api/china";
            quertFromServer(address,"province");
        }
    }
    /**
     * 查询选中省内所有的市
     */
    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList= DataSupport.where("provinceId=?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size()>0){
            dataList.clear();
            for(City city:cityList)
                dataList.add(city.getCityName());
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            String address="http://guolin.tech/api/china/"+provinceCode;
            quertFromServer(address,"city");
        }
    }
    /**
     * 查询选中市内所有的县
     */
    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList=DataSupport.where("cityId=?",String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size()>0){
            dataList.clear();
            for(County county:countyList)
                dataList.add(county.getCountyName());
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }else{
            int provinceCode=selectedProvince.getProvinceCode();
            int cityCode=selectedCity.getCityCode();
            Log.d(getActivity().toString(), provinceCode+" "+cityCode);
            String address="http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            quertFromServer(address,"county");
        }
    }
    /**
     * 根据传入的地址和类型从服务器上查询省市县数据
     */
    private void quertFromServer(String address,final String type){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
               //处理线程逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getActivity(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();
                boolean result=false;
                if("province".equals(type))
                    result= Utility.handleProvinceResponse(responseText);
                else if("city".equals(type))
                    result=Utility.handleCityResponse(responseText,selectedProvince.getId());
                else if("county".equals(type))
                    result= Utility.handleCountyResponse(responseText,selectedCity.getId());
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type))
                                queryProvinces();
                            else if("city".equals(type))
                                queryCities();
                            else if("county".equals(type))
                                queryCounties();
                        }
                    });
                }
            }
        });
    }
    /**
     * 显示进度对话框
     */
    private void showProgressDialog(){
        if(progressDialog==null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog(){
        if(progressDialog!=null)
            progressDialog.dismiss();
    }
    private void requestLocation(){
        initLocation();
        locationClient.start();
    }
    private void initLocation(){
        LocationClientOption option=new LocationClientOption();
        option.setScanSpan(5000);
        option.setIsNeedAddress(true);
        option.setIsNeedLocationDescribe(true);
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        locationClient.setLocOption(option);
    }
    //查看请求是否被允许
    public void getRequestPermisssions(){
        List<String> permissionList=new ArrayList<>();
        if(ContextCompat.checkSelfPermission(getActivity(), Manifest
                .permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(getActivity(), Manifest
                .permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if(ContextCompat.checkSelfPermission(getActivity(), Manifest
                .permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(ContextCompat.checkSelfPermission(getActivity(), Manifest
                .permission.ACCESS_WIFI_STATE)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_WIFI_STATE);
        }
        if(!permissionList.isEmpty()){
            String[] permissions=permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(getActivity(),permissions,1);
        }else{
            requestLocation();
        }
    }
    //获取GPS定位请求
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if(grantResults.length>0){
                    for(int result:grantResults){
                        if(result!= PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(getActivity(),"必须同意所有权限才能使用本应用",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    requestLocation();
                }else{
                    Toast.makeText(getActivity(),"发生未知错误",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }
    public class LocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            //设置定位城市
            String district=bdLocation.getDistrict();
            Log.d("位置信息", "onReceiveLocation: "+district);
            locationText.setText(district);
            address=new Address();
            address.setAddress(district);
            locationClient.stop();
            //将定位城市数据信息保存于leanCloud服务器中
            //查询数据库中是否有此城市数据
            requestCityId(address);
            try{
                Thread.sleep(700);
            }catch (Exception e){
                e.printStackTrace();
            }
            //将定位城市存放于leanCloud服务器
            if(address!=null||address.getAddress()!=""||address.getAddress()!="未知"){
                Log.d("leanCloud", "onReceiveLocation: "+address.getAddress());
                AVQuery<AVObject> query=new AVQuery<>("Address");
                query.getFirstInBackground(new GetCallback<AVObject>() {
                    @Override
                    public void done(final AVObject object, AVException e) {
                        Log.d("AVObject", "done: "+object.getClass());
                        object.put("address",address.getAddress());
                        object.put("cid",address.getCid());
                        AVSaveOption option=new AVSaveOption();
                        option.query(new AVQuery<>("Address").
                                whereEqualTo("address",address.getAddress()));
                        option.setFetchWhenSave(true);
                        object.saveInBackground(option, new SaveCallback() {
                            @Override
                            public void done(AVException e) {
                                if(e==null){
                                    //已经存在此城市的记录，则更新记录
                                    Log.d("update", "done: success");
                                }else{
                                    //不存在此城市记录，插入新记录
                                    AVObject object=new AVObject("Address");
                                    object.put("address",address.getAddress());
                                    object.put("cid",address.getCid());
                                    object.saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(AVException e) {
                                            if(e==null){
                                                Log.d("insert", "done: success");
                                            }
                                        }
                                    });
                                }
                            }
                        });
                    }
                });
            }
        }
    }
    /**
     * 根据所处的地理位置经纬度来请求城市id
     */
    public void requestCityId(final Address address){
        String cityUrl="https://search.heweather.com/find?location="+address.getAddress()+"&key=99e2cdb4b9b94fec84534aa48ac36c55";
        HttpUtil.sendOkHttpRequest(cityUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Toast.makeText(getActivity(),"获取城市id信息失败",
                                Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();
                String cityId=Utility.handleCityResponse(responseText);
                Log.d("天气ID", "onResponse: "+cityId);
                address.setCid(cityId);
                address.save();
            }
        });
    }
}
