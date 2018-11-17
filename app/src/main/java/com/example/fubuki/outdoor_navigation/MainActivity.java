package com.example.fubuki.outdoor_navigation;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.ArcOptions;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.TextOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.example.fubuki.outdoor_navigation.MyUtil.calculateAngle;
import static com.example.fubuki.outdoor_navigation.MyUtil.searchMinPoint;
import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static java.lang.Double.min;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,View.OnClickListener{

    private MapView mMapView = null;
    private BaiduMap mBaiduMap;

    private LocationClient locationClient;

    public MyLocationListenner myListener = new MyLocationListenner();

    boolean isFirstLoc = true; // 是否首次定位

    private SensorManager sm;
    //需要两个Sensor
    private Sensor aSensor;
    private Sensor mSensor;
    private float orientationX;
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];
    private float[] orientationValues = new float[3];

    private final String TAG = "Activity";

    private List<Object> locationTuple = new ArrayList<Object>(); //保存定位触发的三元组

    private GpsNode gpsPointSet;
    private GpsNode blindSearchGpsPointSet; //盲走阶段的GPS序列
    //蓝牙
    private List<String> bluetoothDevices = new ArrayList<String>(); //保存搜索到的列表
    private ArrayAdapter<String> arrayAdapter; //ListView的适配器

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    private BluetoothGatt bluetoothGatt;
    //bluetoothDevice是dervices中选中的一项 bluetoothDevice=dervices.get(i);
    private BluetoothGattService bluetoothGattService;
    private BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private BluetoothDevice bluetoothDevice;

    private List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();//存放扫描结果

    private AlertDialog.Builder builder;
    private AlertDialog alertDialog;

    private static final int UPDATE_STATUS= 1;
    private static final int DISCONN_BLE = 2;
    private static final int UPDATE_LIST = 3;
    private static final int TIMER_LOCATION = 4;
    private static final int NEW_DISTANCE = 5;
    private static final int MOVE_FORWARD = 6;
    private static final int NEW_SAMPLE = 7;
    private static final int TURN_AROUND = 8;
    private static final int TURN_REVERSE = 9;
    private static final int NO_POINT = 10;
    private static final int TOO_FAR = 11;
    private double rcvDis; //从终端接收回来的距离

    private int positionNumber;

    //定时器相关
    private final Timer locationTimer = new Timer();
    private TimerTask locationTask;
    private double currentLatitude,currentLongitude,firstLatitude,firstLongitude,lastNodeLatitude,lastNodeLongitude,prevSampleLatitude,prevSampleLongitude;

    //线程相关测试
    private class Token {
        private boolean flag;
        public Token() {
            setFlag(false);
        }
        public void setFlag(boolean flag) {
            this.flag = flag;
        }
        public boolean getFlag() {
            return flag;
        }
    }
    private Token token = null;

    private boolean toggleFlag = false;
    private boolean isFirstDistance = true;
    private boolean isSecondPoint = true;

    //画线测试
    private Polyline mPolyline;

    //反编码测试
    private static GeoCoder mSearch = null;

    private List<Double> distanceArray = new ArrayList<Double>();//存放接收的距离序列
    private static boolean isReverse = false;
    private static int delayCount = 0;

    private static double validDistance = 30;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button searchBtn = findViewById(R.id.getLocation);

        searchBtn.setOnClickListener(this);

        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();

        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);

        locationClient = new LocationClient(this);
        //注册监听
        locationClient.registerLocationListener(myListener);
        //定位配置信息
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);//定位请求时间间隔
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);

        locationClient.setLocOption(option);
        //开启定位
        locationClient.start();

        //传感器相关
        orientationX = 0;
        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sm.registerListener(mySensorListener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(mySensorListener, mSensor,SensorManager.SENSOR_DELAY_NORMAL);

        //更新显示数据的方法
        calculateOrientation();

        //蓝牙
        // 检查手机是否支持BLE，不支持则退出
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "您的设备不支持蓝牙BLE，将关闭", Toast.LENGTH_SHORT).show();
            finish();
        }

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);  // 弹对话框的形式提示用户开启蓝牙
        }

        Button searchBLEBtn = findViewById(R.id.searchBtn);
        searchBLEBtn.setOnClickListener(this);

        gpsPointSet = new GpsNode();
        blindSearchGpsPointSet = new GpsNode();
        positionNumber = 0;

        rcvDis = 0;

        //定时相关
        currentLatitude = 0;
        currentLongitude = 0;
        firstLatitude = 0;
        firstLongitude = 0;
        lastNodeLatitude = 0;
        lastNodeLongitude = 0;
        prevSampleLatitude = 0;
        prevSampleLongitude = 0;

        locationTask = new TimerTask() {
            @Override
            public void run() {

                if(!isFirstDistance && isSecondPoint) {
                    getSecondLocation();
                }
                if(!isFirstDistance) {
                    checkDistance();
                }
            }
        };
        locationTimer.schedule(locationTask, 1000, 2000);

        token = new Token();
        if(!token.getFlag())
            Log.e("A","the token flag value is null");
        else
            Log.e("A","the token flag value is"+token.getFlag());

        //mSearch = GeoCoder.newInstance();
       // mSearch.setOnGetGeoCodeResultListener(this);
    }

    final SensorEventListener mySensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticFieldValues = sensorEvent.values;
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = sensorEvent.values;
            calculateOrientation();
        }
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private  void calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
        SensorManager.getOrientation(R, orientationValues);
        if(orientationValues[0] < 0)
            orientationValues[0] = orientationValues[0] + (float)(2*Math.PI);
        // 要经过一次数据格式的转换，转换为度
        values[0] = (float) Math.toDegrees(orientationValues[0]);
        if(Math.abs(values[0] - orientationX) > 3.0){
            orientationX = values[0];
        }
    }

    //断开BLE连接
    private void disconnect_BLE(){
        bluetoothGatt.disconnect();
        Message tempMsg = new Message();
        tempMsg.what = DISCONN_BLE;
        handler.sendMessage(tempMsg);
    }

    private void checkDistance(){
        LatLng p1 = new LatLng(currentLatitude,currentLongitude);
        LatLng p2 = new LatLng(lastNodeLatitude,lastNodeLongitude);
        LatLng p3 = new LatLng(prevSampleLatitude,prevSampleLongitude);

        //Log.e(TAG,"前一次采样坐标："+prevSampleLatitude+"#"+prevSampleLongitude);
        //Log.e(TAG,"距离目标节点："+DistanceUtil.getDistance(p1,p2));
        //Log.e(TAG,"距离上次采样："+DistanceUtil.getDistance(p1,p3));
        if((DistanceUtil.getDistance(p1,p2) < 10 || DistanceUtil.getDistance(p1,p3) > 5) && prevSampleLatitude>0 && prevSampleLongitude>0){
            //Log.e(TAG,"检测到距离小于10米或大于旧的位置10米");
            if(bluetoothGattCharacteristic != null) {
                synchronized (token) {
                    try {
                        token.setFlag(true);
                        Log.e(TAG, "线程挂起");
                        token.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.e(TAG,"线程重新恢复 in checkDistance");
            //接收距离小于30才添加
            if(rcvDis < 30) {
                GpsPoint currentGpsPoint = new GpsPoint(currentLongitude, currentLatitude, orientationValues[0], rcvDis, gpsPointSet.getNodeNumber());
            /*double deltaD = Math.abs(gpsPointSet.getGpsPoint(gpsPointSet.getNodeNumber()-1).getDistance() - rcvDis);
            LatLng p4 = new LatLng(currentLatitude,currentLongitude);
            LatLng p5 = new LatLng(prevSampleLatitude,prevSampleLongitude);

            double distance = DistanceUtil.getDistance(p4,p5);
            Log.e(TAG,"deltaD:"+deltaD);
            Log.e(TAG,"distance:"+distance);
            if(deltaD < distance ){
                Log.e(TAG,"符合添加规则");
                //gpsPointSet.addGpsPoint(currentGpsPoint);
            }else{
                Log.e(TAG,"不符合添加规则，不更新");
            }*/
            /*if(gpsPointSet.getNodeNumber() > 3){
                if(MyUtil.judgeTrend(gpsPointSet,rcvDis)) {
                    Log.e("distance judge","反方向！");
                    Message tempMsg = new Message();
                    tempMsg.what = TURN_REVERSE;
                    handler.sendMessage(tempMsg);
                    return;
                }
            }*/
                gpsPointSet.addGpsPoint(currentGpsPoint);
                Log.e(TAG, "当前采样的GPS点相关信息：" + currentGpsPoint.getLatitude() + "#" + currentGpsPoint.getLongitude() + "#当前接收到的距离:" + rcvDis);
                if (gpsPointSet.getNodeNumber() > 2) {
                    //Point nodePosition = gpsPointSet.getNodePosition();
                    //Point nodePosition = gpsPointSet.varianceGetNodePosition();
                    Point nodePosition = gpsPointSet.countGetNodePosition(currentGpsPoint, rcvDis);
                    Log.e(TAG, "x:" + nodePosition.getX() + "   " + "y:" + nodePosition.getY());

                    if (isNaN(nodePosition.getY()) || isNaN(nodePosition.getX()) || nodePosition.getY() == 0.0 || nodePosition.getX() == 0.0) {
                        //Log.e(TAG,"计算出的距离是NaN，需要左拐或右拐");
                        //Message tempMsg = new Message();
                        //tempMsg.what = NO_POINT;
                        //handler.sendMessage(tempMsg);
                        return;
                    } else {
                        lastNodeLatitude = nodePosition.getY();
                        lastNodeLongitude = nodePosition.getX();
                    }
                    prevSampleLatitude = currentLatitude;
                    prevSampleLongitude = currentLongitude;

                    //TODO:要显示在屏幕上
                    mBaiduMap.clear();
                    //Node returnNode = gpsPointSet.getPoints();
                    /*Node returnNode = gpsPointSet.getReturnNode();
                    List<OverlayOptions> options = new ArrayList<OverlayOptions>();
                    for (int m = 0; m < returnNode.getSize(); m++) {
                        //Log.e(TAG,"第"+m+"个点："+returnNode.getPoint(m).getY()+"/"+returnNode.getPoint(m).getX());
                        LatLng point = new LatLng(returnNode.getPoint(m).getY(), returnNode.getPoint(m).getX());

                        BitmapDescriptor bitmap = BitmapDescriptorFactory
                                .fromResource(R.drawable.icon_temp);

                        OverlayOptions textOption1 = new TextOptions()
                                .bgColor(0xAAFFFF00)
                                .fontSize(24)
                                .fontColor(0xFFFF00FF)
                                .text(Integer.toString(m))
                                .position(point);
                        mBaiduMap.addOverlay(textOption1);

                        OverlayOptions option = new MarkerOptions()
                                .position(point)
                                .icon(bitmap);
                        options.add(option);
                    }
                    LatLng point = new LatLng(nodePosition.getY(), nodePosition.getX());
                    BitmapDescriptor bitmap;
                    bitmap = BitmapDescriptorFactory
                            .fromResource(R.drawable.icon_en);
                    OverlayOptions option = new MarkerOptions()
                            .position(point)
                            .icon(bitmap);
                    options.add(option);
                    mBaiduMap.addOverlays(options);*/
                LatLng point = new LatLng(nodePosition.getY(), nodePosition.getX());

                BitmapDescriptor bitmap;
                if(rcvDis > 5){
                    bitmap = BitmapDescriptorFactory
                            .fromResource(R.drawable.icon_temp);
                    /*Message tempMsg = new Message();
                    tempMsg.what = NEW_SAMPLE;
                    handler.sendMessage(tempMsg);*/

                }else{
                    bitmap = BitmapDescriptorFactory
                            .fromResource(R.drawable.icon_en);
                }

                OverlayOptions option = new MarkerOptions()
                        .position(point)
                        .icon(bitmap);

                mBaiduMap.addOverlay(option);

                    LatLng llText = new LatLng(nodePosition.getY(), nodePosition.getX());

//构建文字Option对象，用于在地图上添加文字
                    OverlayOptions textOption = new TextOptions()
                            .bgColor(0xAAFFFF00)
                            .fontSize(24)
                            .fontColor(0xFFFF00FF)
                            .text(Integer.toString(positionNumber++))
                            .position(llText);

//在地图上添加该文字对象并显示
                    mBaiduMap.addOverlay(textOption);
                } else {
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        locationClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);

    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();

    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    private void getLocation(){
        Log.e(TAG,"相关信息:"+currentLatitude+" "+currentLongitude+" "+orientationValues[0]+" "+rcvDis);
        GpsPoint currentGpsPoint = new GpsPoint(currentLongitude,currentLatitude,orientationValues[0],rcvDis, gpsPointSet.getNodeNumber());

        if(rcvDis < validDistance) {
            gpsPointSet.addGpsPoint(currentGpsPoint);
            Log.e(TAG, "当前采样的GPS点相关信息：" + currentGpsPoint.getLatitude() + "#" + currentGpsPoint.getLongitude() + "#当前接收到的距离:" + rcvDis);
            if (gpsPointSet.getNodeNumber() > 2) {
                //Point nodePosition = gpsPointSet.getNodePosition();
                //Point nodePosition = gpsPointSet.varianceGetNodePosition();
                Point nodePosition = gpsPointSet.countGetNodePosition(currentGpsPoint, rcvDis);
                //Log.e(TAG,"x:"+nodePosition.getX()+"   "+"y:"+nodePosition.getY());

                lastNodeLatitude = nodePosition.getY();
                lastNodeLongitude = nodePosition.getX();

                if (isSecondPoint) {
                    prevSampleLongitude = currentLongitude;
                    prevSampleLatitude = currentLatitude;
                    if (isNaN(lastNodeLatitude) || isNaN(lastNodeLongitude)) {
                        //Log.e(TAG,"第二次采样计算出NaN");
                        Message tempMsg = new Message();
                        tempMsg.what = TURN_AROUND;
                        handler.sendMessage(tempMsg);
                    }
                }

                //要显示在屏幕上
                mBaiduMap.clear();
                //Node returnNode = gpsPointSet.getPoints();
                Node returnNode = gpsPointSet.getReturnNode();
                List<OverlayOptions> options = new ArrayList<OverlayOptions>();
            /*LatLng testPoint = new LatLng(30.266996, 120.127323);
            LatLng testPoint2 = new LatLng(30.267996, 120.128323);
            LatLng llText1 = new LatLng(30.266996, 120.127323);
            LatLng llText2 = new LatLng(30.267996, 120.128323);
            BitmapDescriptor bitmapTest = BitmapDescriptorFactory
                    .fromResource(R.drawable.icon_temp);
            OverlayOptions option1 = new MarkerOptions()
                    .position(testPoint)
                    .icon(bitmapTest);
            options.add(option1);
            OverlayOptions option2 = new MarkerOptions()
                    .position(testPoint2)
                    .icon(bitmapTest);
            options.add(option2);


//构建文字Option对象，用于在地图上添加文字
            OverlayOptions textOption1 = new TextOptions()
                    .bgColor(0xAAFFFF00)
                    .fontSize(24)
                    .fontColor(0xFFFF00FF)
                    .text(Integer.toString(1))
                    .position(llText1);

//在地图上添加该文字对象并显示
            mBaiduMap.addOverlay(textOption1);
            OverlayOptions textOption2 = new TextOptions()
                    .bgColor(0xAAFFFF00)
                    .fontSize(24)
                    .fontColor(0xFFFF00FF)
                    .text(Integer.toString(2))
                    .position(llText2);

//在地图上添加该文字对象并显示
            mBaiduMap.addOverlay(textOption2);*/
                //for (int m = 0; m < returnNode.getSize(); m++) {
                LatLng point = new LatLng(nodePosition.getY(), nodePosition.getX());

                BitmapDescriptor bitmap = BitmapDescriptorFactory
                        .fromResource(R.drawable.icon_temp);

                OverlayOptions option = new MarkerOptions()
                        .position(point)
                        .icon(bitmap);

                mBaiduMap.addOverlay(option);
                    //Log.e(TAG,"第"+m+"个点："+returnNode.getPoint(m).getY()+"/"+returnNode.getPoint(m).getX());
                    /*LatLng point = new LatLng(returnNode.getPoint(m).getY(), returnNode.getPoint(m).getX());

                    OverlayOptions textOption1 = new TextOptions()
                            .bgColor(0xAAFFFF00)
                            .fontSize(24)
                            .fontColor(0xFFFF00FF)
                            .text(Integer.toString(m))
                            .position(point);
                    mBaiduMap.addOverlay(textOption1);

                    BitmapDescriptor bitmap = BitmapDescriptorFactory
                            .fromResource(R.drawable.icon_temp);

                    OverlayOptions option = new MarkerOptions()
                            .position(point)
                            .icon(bitmap);
                    options.add(option);
                }
                //mBaiduMap.addOverlays(options);
                LatLng point = new LatLng(nodePosition.getY(), nodePosition.getX());
                BitmapDescriptor bitmap;
                bitmap = BitmapDescriptorFactory
                        .fromResource(R.drawable.icon_en);
                OverlayOptions option = new MarkerOptions()
                        .position(point)
                        .icon(bitmap);
                options.add(option);
                mBaiduMap.addOverlays(options);
                LatLng llText = new LatLng(nodePosition.getY(), nodePosition.getX());

//构建文字Option对象，用于在地图上添加文字
                OverlayOptions textOption = new TextOptions()
                        .bgColor(0xAAFFFF00)
                        .fontSize(24)
                        .fontColor(0xFFFF00FF)
                        .text(Integer.toString(positionNumber++))
                        .position(llText);

//在地图上添加该文字对象并显示
                mBaiduMap.addOverlay(textOption);*/
            } else {
            }
        }
    }
    public void onClick(View view){
        switch(view.getId()){
            case R.id.getLocation:
                getLocation();
                break;
            case R.id.searchBtn: //蓝牙
                if(bluetoothGatt == null){
                    actionAlertDialog();
                }else{
                    disconnect_BLE();
                    bluetoothGatt = null;
                }
                break;
            default:
                break;
        }
    }

    private void actionAlertDialog(){
        View bottomView = View.inflate(MainActivity.this,R.layout.ble_devices,null);//填充ListView布局
        ListView lvDevices = (ListView) bottomView.findViewById(R.id.device_list);//初始化ListView控件
        arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                bluetoothDevices);
        lvDevices.setAdapter(arrayAdapter);
        lvDevices.setOnItemClickListener(this);

        builder= new AlertDialog.Builder(MainActivity.this)
                .setTitle("蓝牙列表").setView(bottomView);//在这里把写好的这个listview的布局加载dialog中
        alertDialog = builder.create();
        alertDialog.show();

        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.startScan(scanCallback);//android5.0把扫描方法单独弄成一个对象了（alt+enter添加），扫描结果储存在devices数组中。最好在startScan()前调用stopScan()。

        handler.postDelayed(runnable, 10000);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            bluetoothLeScanner.stopScan(scanCallback);
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        bluetoothDevice = devices.get(position);
        bluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    alertDialog.dismiss();

                    Message tempMsg = new Message();
                    tempMsg.what = UPDATE_STATUS;
                    handler.sendMessage(tempMsg);

                    try {
                        Thread.sleep(600);
                        Log.i(TAG, "Attempting to start service discovery:"
                                + gatt.discoverServices());
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        Log.i(TAG, "Fail to start service discovery:");
                        e.printStackTrace();
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    //setTitle("连接断开");
                }
                return;
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, final int status) {
                //此函数用于接收数据
                super.onServicesDiscovered(gatt, status);
                Log.d(TAG, "Hi discovered!");
                String service_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
                String characteristic_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";

                bluetoothGattService = bluetoothGatt.getService(UUID.fromString(service_UUID));
                bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(characteristic_UUID));

                if (bluetoothGattCharacteristic != null) {
                    gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true); //用于接收数据
                    for (BluetoothGattDescriptor dp : bluetoothGattCharacteristic.getDescriptors()) {
                        if (dp != null) {
                            if ((bluetoothGattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            } else if ((bluetoothGattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                                dp.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            }
                            gatt.writeDescriptor(dp);
                        }
                    }
                    Log.d(TAG, "服务连接成功");
                } else {
                    Log.d(TAG, "服务失败");
                    return;
                }
                return;
            }
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
                super.onCharacteristicChanged(gatt,characteristic);
                //发现服务后的响应函数
                byte[] bytesReceive = characteristic.getValue();
                String msgStr = new String(bytesReceive);
                //TODO:该正则匹配有问题，但目前可以忽视
                Pattern pattern = Pattern.compile("[(?<=addr|dis) (?=end)]+");
                String[] strs = pattern.split(msgStr);

                rcvDis = convertToDouble(strs[1],0);
                Log.e(TAG,"蓝牙接收到的距离："+rcvDis);

                if(distanceArray.size() > 100){
                    distanceArray.clear();
                }
                if(rcvDis > 0)
                    distanceArray.add(rcvDis);

                if(rcvDis > 70){
                    Message tempMsg = new Message();
                    tempMsg.what = TOO_FAR;
                    handler.sendMessage(tempMsg);
                }
                //盲走序列添加
                blindSearchGpsPointSet.addGpsPoint(new GpsPoint(currentLongitude,currentLatitude,orientationValues[0],rcvDis, gpsPointSet.getNodeNumber()));

                if(isReverse){
                    delayCount ++;
                    if(delayCount > 6) {
                        isReverse = false;
                        delayCount = 0;
                    }
                }else if(distanceArray.size()>5){
                    if(MyUtil.judgeTrend2(distanceArray)){
                        Message tempMsg = new Message();
                        tempMsg.what = TURN_REVERSE;
                        handler.sendMessage(tempMsg);
                        isReverse = true;
                    }
                }
                Message tempMsg = new Message();
                tempMsg.what = NEW_DISTANCE;
                handler.sendMessage(tempMsg);
                toggleFlag = !toggleFlag;

                if(token.getFlag()) {
                    synchronized (token) {
                        token.setFlag(false);
                        token.notifyAll();
                        Log.e(TAG,"线程重新启动");
                    }
                }

                if(isFirstDistance){
                    getLocation();
                    isFirstDistance = false;

                    firstLatitude = currentLatitude;
                    firstLongitude = currentLongitude;

                    prevSampleLatitude = currentLatitude;
                    prevSampleLongitude = currentLongitude;
                    Message tempMsg2 = new Message();
                    tempMsg2.what = MOVE_FORWARD;
                    handler.sendMessage(tempMsg2);
                }


                return;
            }
        });
    }
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult results) {
            super.onScanResult(callbackType, results);
            BluetoothDevice device = results.getDevice();
            if (!devices.contains(device)) {  //判断是否已经添加
                devices.add(device);//也可以添加devices.getName()到列表，这里省略            }
                // callbackType：回调类型
                // result：扫描的结果，不包括传统蓝牙        }
                bluetoothDevices.add(device.getName() + ":"
                        + device.getAddress() + "\n");
                //更新字符串数组适配器，显示到listview中
                Message tempMsg = new Message();
                tempMsg.what = UPDATE_LIST;
                handler.sendMessage(tempMsg);
            }
        }
    };
    public class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(orientationX).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);

            MyLocationConfiguration config = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, BitmapDescriptorFactory.fromResource(R.drawable.arrow));
            mBaiduMap.setMyLocationConfiguration(config);

            //获取并显示
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();

            LatLng ptCenter = new LatLng(currentLatitude, currentLongitude);
           // mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(ptCenter).newVersion(1).radius(500));

            if(lastNodeLatitude > 0.0 && lastNodeLongitude > 0.0){
                //mBaiduMap.clear();
                LatLng p1 = new LatLng(currentLatitude, currentLongitude);
                LatLng p2 = new LatLng(lastNodeLatitude, lastNodeLongitude);
                List<LatLng> points = new ArrayList<LatLng>();
                points.add(p1);
                points.add(p2);
                OverlayOptions ooPolyline = new PolylineOptions().width(10)
                        .color(0xAAFF0000).points(points);
                //mPolyline = (Polyline) mBaiduMap.addOverlay(ooPolyline);

                //重新画点？？？
                LatLng point = new LatLng(lastNodeLatitude, lastNodeLongitude);

                BitmapDescriptor bitmap;
                if(rcvDis > 5){
                    bitmap = BitmapDescriptorFactory
                            .fromResource(R.drawable.icon_temp);
                   // Message tempMsg = new Message();
                   // tempMsg.what = NEW_SAMPLE;
                   // handler.sendMessage(tempMsg);

                }else{
                    bitmap = BitmapDescriptorFactory
                            .fromResource(R.drawable.icon_en);
                }

                OverlayOptions option = new MarkerOptions()
                        .position(point)
                        .icon(bitmap);

                mBaiduMap.addOverlay(option);
                //end


                double angle = calculateAngle(lastNodeLatitude,lastNodeLongitude,currentLatitude,currentLongitude);
                List<LatLng> gpsTrace = new ArrayList<LatLng>();
                List<Integer> colors = new ArrayList<>();

                for(int m = 0;m<gpsPointSet.getNodeNumber();m++){
                    LatLng temp = new LatLng(gpsPointSet.getGpsPoint(m).getLatitude(), gpsPointSet.getGpsPoint(m).getLongitude());
                    gpsTrace.add(temp);
                    //Log.e(TAG,"第"+m+"个采样点："+gpsPointSet.getGpsPoint(m).getLatitude()+"/"+gpsPointSet.getGpsPoint(m).getLongitude());
                    if(m%4 == 0){
                        colors.add(Integer.valueOf(Color.BLUE));
                    }else if(m%4 == 1){
                        colors.add(Integer.valueOf(Color.RED));
                    }else if(m%4 == 2){
                        colors.add(Integer.valueOf(Color.YELLOW));
                    }else{
                        colors.add(Integer.valueOf(Color.GREEN));
                    }
                }
                OverlayOptions gpsPolyline = new PolylineOptions().width(10)
                        .colorsValues(colors).points(gpsTrace);
                mPolyline = (Polyline) mBaiduMap.addOverlay(gpsPolyline);
               /* MyLocationData nodeAngleData = new MyLocationData.Builder()
                        .accuracy(location.getRadius())
                        // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction((float)angle).latitude(location.getLatitude())
                        .longitude(location.getLongitude()).build();
                mBaiduMap.setMyLocationData(nodeAngleData);*/

               // MyLocationConfiguration nodeConfig = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, BitmapDescriptorFactory.fromResource(R.drawable.black_arrow));
                //mBaiduMap.setMyLocationConfiguration(nodeConfig);

            }
            //开机采集一个点
            if (isFirstLoc) {
                isFirstLoc = false;

                //GpsPoint currentGpsPoint = new GpsPoint(currentLongitude,currentLatitude,orientationValues[0],rcvDis);
                //gpsPointSet.addGpsPoint(currentGpsPoint);

                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }

        public void onReceivePoi(BDLocation poiLocation) {
        }
    }

    private void getSecondLocation(){
        LatLng p1 = new LatLng(currentLatitude,currentLongitude);
        LatLng p2 = new LatLng(firstLatitude,firstLongitude);

        double distance = DistanceUtil.getDistance(p1,p2);

        //Log.e(TAG,"fisrtDistance:"+firstLatitude+" "+firstLongitude);
        //Log.e(TAG,"currentDistance:"+currentLatitude+" "+currentLongitude);
        //Log.e(TAG,"实测距离"+distance);

        if(distance > 10.0){
            //TODO
            //此处是距离大于十米的操作
            //Log.e(TAG,"第二次采样！");
            getLocation();
          if(bluetoothGattCharacteristic != null) {
              synchronized (token) {
                  try {
                      token.setFlag(true);
                      Log.e(TAG, "线程挂起，等待接受蓝牙距离");
                      token.wait();
                  } catch (InterruptedException e) {
                      e.printStackTrace();
                  }
              }
          }
          isSecondPoint = false;
        }
    }

    private Handler handler = new Handler(){

        public void handleMessage(Message msg){
            Button searchBtn = findViewById(R.id.searchBtn);
            switch (msg.what){
                case UPDATE_STATUS:
                    searchBtn.setText("断开蓝牙");
                    searchBtn.setBackgroundResource(R.drawable.cancelbutton);
                    break;
                case DISCONN_BLE:
                    searchBtn.setText("搜索蓝牙");
                    searchBtn.setBackgroundResource(R.drawable.buttonshape);
                    break;
                case UPDATE_LIST:
                    arrayAdapter.notifyDataSetChanged();
                    break;
                case TIMER_LOCATION:
                    getSecondLocation();
                    break;
                case NEW_DISTANCE:
                    TextView distanceText = findViewById(R.id.distance);
                    distanceText.setText("距离信息："+rcvDis);
                    break;
                case MOVE_FORWARD:
                    Toast.makeText(MainActivity.this,"请随机走",Toast.LENGTH_LONG).show();
                    break;
                case NEW_SAMPLE:
                    Toast.makeText(MainActivity.this,"请走到下一个采样点",Toast.LENGTH_LONG).show();
                    break;
                case TURN_AROUND:
                    Toast.makeText(MainActivity.this,"请向左拐或右拐",Toast.LENGTH_LONG).show();
                    break;
                case TURN_REVERSE:
                    Toast.makeText(MainActivity.this,"正在逐渐偏离目标，请往回走",Toast.LENGTH_SHORT).show();
                    break;
                case NO_POINT:
                    Toast.makeText(MainActivity.this,"暂时算不出节点，请左拐或右拐",Toast.LENGTH_SHORT).show();
                    break;
                case TOO_FAR:
                    GpsPoint minBlindPoint = searchMinPoint(blindSearchGpsPointSet);
                    LatLng minPoint = new LatLng(minBlindPoint.getLatitude(),minBlindPoint.getLongitude());
                    //小于有效距离认定可靠，添加！
                    if(minBlindPoint.getDistance() < validDistance){
                        gpsPointSet.addGpsPoint(minBlindPoint);
                    }
                    mBaiduMap.clear();
                    BitmapDescriptor bitmap = BitmapDescriptorFactory
                            .fromResource(R.drawable.icon_temp);

                    OverlayOptions textOption1 = new TextOptions()
                            .bgColor(0xAAFFFF00)
                            .fontSize(24)
                            .fontColor(0xFFFF00FF)
                            .position(minPoint);
                    mBaiduMap.addOverlay(textOption1);

                    OverlayOptions option = new MarkerOptions()
                            .position(minPoint)
                            .icon(bitmap);
                    mBaiduMap.addOverlay(option);
                    Toast.makeText(MainActivity.this,"请回到提示点，换一个方向走",Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };//import android.os.Handler;

    //string转float
    public static double convertToDouble(String number, float defaultValue) {
        if (TextUtils.isEmpty(number)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(number);
        } catch (Exception e) {
            return defaultValue;
        }

    }

}
