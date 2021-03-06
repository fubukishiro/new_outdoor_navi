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
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.Stroke;
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
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.example.fubuki.outdoor_navigation.MyUtil.convertToDouble;
import static com.example.fubuki.outdoor_navigation.MyUtil.findMinErrorPoint;
import static com.example.fubuki.outdoor_navigation.MyUtil.searchMinPoint;
import static java.lang.Double.MAX_VALUE;
import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static java.lang.Double.min;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,View.OnClickListener{

    private MapView mMapView = null;
    private BaiduMap mBaiduMap;

    private LocationClient locationClient;

    public MyLocationListenner myListener = new MyLocationListenner();

    private SensorManager sm;
    //需要两个Sensor
    private Sensor aSensor;
    private Sensor mSensor;
    private float orientationX;
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];
    private float[] orientationValues = new float[3];

    private final String TAG = "Activity";


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

    private boolean isFirstLoc = true;

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
    private static final int SHOW_POINT = 12;
    private static final int NEW_RSSI = 13;
    private static final int TURN_REVERSE_RSSI = 14;
    private static final int TURN_REVERSE_NEW = 15;

    private double rcvDis = 0; //从终端接收回来的距离
    private double rssi; //从终端接收回来的rssi

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

    private Polyline mPolyline;

    private List<Double> distanceArray = new ArrayList<Double>();//存放接收的距离序列
    private static boolean isReverse = false;
    private static int delayCount = 0;
    private static int delayRssiCount = 0;

    private static double validDistance = Double.MAX_VALUE;

    private boolean isFinal = false;

    private double minArrayDis;

    private List<Rssi> rssiArray = new ArrayList<Rssi>();//存放接收的距离序列

    private FileLogger mFileLogger = new FileLogger(); //用于数据存储


    private boolean isStartRecord = false;

    private boolean is_hint_rssi = false; //是否根据rssi提示过

    private List<Integer> rssiLostCount = new ArrayList<Integer>();

    private boolean isAgainFindDis = false; //是否重新找到距离不为0的判断

    private boolean isRssiCountReverse = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        //地图指南针方向计算
        calculateOrientation();

        //蓝牙相关
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

        //搜索蓝牙的按钮
        Button searchBLEBtn = findViewById(R.id.searchBtn);
        searchBLEBtn.setOnClickListener(this);

        gpsPointSet = new GpsNode(true);   //计算用的gps点序列
        blindSearchGpsPointSet = new GpsNode(false);  //盲走阶段的gps点序列
        positionNumber = 0;

        rcvDis = 0;

        //当前经纬度存储
        currentLatitude = 0;
        currentLongitude = 0;

        //用于记录第一次采集的GPS点的经纬度
        firstLatitude = 0;
        firstLongitude = 0;

        //上一次计算出的可能节电的经纬度
        lastNodeLatitude = 0;
        lastNodeLongitude = 0;

        //上一次采样点的经纬度
        prevSampleLatitude = 0;
        prevSampleLongitude = 0;

        //定时检测距离
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

        //用于线程挂起
        token = new Token();
        if(!token.getFlag())
            Log.e("A","the token flag value is null");
        else
            Log.e("A","the token flag value is"+token.getFlag());

        minArrayDis = Double.MAX_VALUE;

        //设定安全距离的按钮
        Button setDistanceBtn = findViewById(R.id.setValidDistance);
        setDistanceBtn.setOnClickListener(this);

        //开始记录数据的按钮
        Button startRecordBtn = findViewById(R.id.startRecord);
        startRecordBtn.setOnClickListener(this);

        //mFileLogger.initData();
        //DrawTestPoint();
    }

    //传感器监听初始化
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

    //计算指南针指向
    private void calculateOrientation() {
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

    //检查距离，判断是否采样
    private void checkDistance(){
        LatLng p1 = new LatLng(currentLatitude,currentLongitude);
        LatLng p2 = new LatLng(lastNodeLatitude,lastNodeLongitude);
        LatLng p3 = new LatLng(prevSampleLatitude,prevSampleLongitude);

        //当 当前位置距离计算出的可能节点位置小于10米 或者 距离上次采样点大于5米（已走五米）时，进行新一次的采样
        if((DistanceUtil.getDistance(p1,p2) < 10 || DistanceUtil.getDistance(p1,p3) > 5) && prevSampleLatitude>0 && prevSampleLongitude>0){
            //等待最新的蓝牙发送过来的距离
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

            //当接收距离小于有效阈值且不为0时，才把当前GPS添加进去
            if(rcvDis <= validDistance && rcvDis > 0) {
                GpsPoint currentGpsPoint = new GpsPoint(currentLongitude, currentLatitude, orientationValues[0], rcvDis, gpsPointSet.getNodeNumber());

                gpsPointSet.addGpsPoint(currentGpsPoint);
                //当计算用的采样gps序列中的点数大于2时，计算位置
                if (gpsPointSet.getNodeNumber() > 2) {

                    //TODO:计算节点位置的函数接口
                    //Point nodePosition = gpsPointSet.getNodePosition();
                    ArrayList<PosEstimation> posEstimationArray = new ArrayList<PosEstimation>();
                    posEstimationArray = gpsPointSet.getNodePosition(currentGpsPoint);

                    //使得posEstimationArray的第一个元素误差最小(rt)
                    int minErrorIndex = findMinErrorPoint(posEstimationArray);
                    PosEstimation temp;
                    temp = posEstimationArray.get(minErrorIndex);
                    posEstimationArray.set(minErrorIndex,posEstimationArray.get(0));
                    posEstimationArray.set(0, temp);

                    Point nodePosition = posEstimationArray.get(0).getEstimationPos();
                    Log.e(TAG,"计算出目标位置："+nodePosition.getY()+"#"+nodePosition.getX());
                    if (isNaN(nodePosition.getY()) || isNaN(nodePosition.getX()) || nodePosition.getY() == 0.0 || nodePosition.getX() == 0.0) {
                        //计算出的点是NaN或者0的时候，不更新位置
                        return;
                    } else {
                        lastNodeLatitude = nodePosition.getY();
                        lastNodeLongitude = nodePosition.getX();
                    }
                    prevSampleLatitude = currentLatitude;
                    prevSampleLongitude = currentLongitude;

                    mBaiduMap.clear();

                    LatLng point = new LatLng(nodePosition.getY(), nodePosition.getX());

                    List<OverlayOptions> options = new ArrayList<OverlayOptions>();

                    BitmapDescriptor bitmap;

                    //使得posEstimationArray的第一个元素及时显示(rt)
                    bitmap = BitmapDescriptorFactory
                            .fromResource(R.drawable.icon_temp);
                        OverlayOptions option = new MarkerOptions()
                                .position(point)
                                .icon(bitmap);
                        OverlayOptions errorCircle = new CircleOptions().fillColor(0x384d73b3)
                                .center(point).stroke(new Stroke(3,0x784d73b3)).radius((int)posEstimationArray.get(0).getPosError());
                        options.add(errorCircle);
                        options.add(option);

                    if(MyUtil.isWalkingStraight(blindSearchGpsPointSet)){
                        Log.e(TAG,"在走直线！");
                    }

                    //远距离下显示对称点
                    /*if(rcvDis > 40 && MyUtil.isWalkingStraight(blindSearchGpsPointSet)){
                        int currentGpsPointSetNum = blindSearchGpsPointSet.getNodeNumber();
                        LatLng symmetricP = MyUtil.getSymmetricPoint(blindSearchGpsPointSet.getGpsPoint(currentGpsPointSetNum-1),blindSearchGpsPointSet.getGpsPoint(currentGpsPointSetNum-2),nodePosition);
                        int symPosError = (int)MyUtil.calculatePositionError(nodePosEstimation.getReliablePoint(),new Point(symmetricP.longitude,symmetricP.latitude));

                        OverlayOptions symErrorCircle = new CircleOptions().fillColor(0x384d73b3)
                                .center(symmetricP).stroke(new Stroke(3,0x784d73b3)).radius(symPosError*10);
                        bitmap = BitmapDescriptorFactory
                                .fromResource(R.drawable.icon_temp);
                        OverlayOptions symOption = new MarkerOptions()
                                .position(symmetricP)
                                .icon(bitmap);
                        options.add(symErrorCircle);
                        options.add(symOption);
                    }*/
                    //显示多解
                    if(rcvDis > 20){
                        double minError = posEstimationArray.get(0).getPosError();
                        for(int i = 1;i<posEstimationArray.size();i++){
                            Point tempPos = posEstimationArray.get(i).getEstimationPos();
                            double tempError = posEstimationArray.get(i).getPosError();
                            bitmap = BitmapDescriptorFactory
                                    .fromResource(R.drawable.icon_temp);
                            if(isNaN(tempPos.getY()) || isNaN(tempPos.getX()))
                                continue;
                            //相差不大的时候才把结果加进去
                            if(Math.abs(tempError - minError)/minError < 0.5){
                                option = new MarkerOptions()
                                        .position(new LatLng(tempPos.getY(),tempPos.getX()))
                                        .icon(bitmap);
                                errorCircle = new CircleOptions().fillColor(0x384d73b3)
                                        .center(new LatLng(tempPos.getY(),tempPos.getX())).stroke(new Stroke(3,0x784d73b3)).radius((int)posEstimationArray.get(i).getPosError());

                                options.add(errorCircle);
                                options.add(option);
                            }
                        }
                    }
                    //当手机位置距离计算出的节点位置小于10米并且接收到的距离小于5米时，显示终点，否则显示绿色的gps点
                    if(rcvDis > 5){
                        bitmap = BitmapDescriptorFactory
                                .fromResource(R.drawable.icon_temp);
                    }else{
                        if(rcvDis >0 && DistanceUtil.getDistance(p1,new LatLng(nodePosition.getY(),nodePosition.getX())) < 10){
                            bitmap = BitmapDescriptorFactory
                                    .fromResource(R.drawable.icon_en);
                            isFinal = true;
                        }else{
                            bitmap = BitmapDescriptorFactory
                                    .fromResource(R.drawable.icon_temp);
                        }
                    }

                    if(!isFinal){
                        Message tempMsg = new Message();
                        tempMsg.what = SHOW_POINT;
                        handler.sendMessage(tempMsg);
                    }
                    option = new MarkerOptions()
                            .position(point)
                            .icon(bitmap);

                    options.add(option);

                    LatLng llText = new LatLng(nodePosition.getY(), nodePosition.getX());

                    //构建文字Option对象，用于在地图上添加文字
                    OverlayOptions textOption = new TextOptions()
                            .bgColor(0xAAFFFF00)
                            .fontSize(24)
                            .fontColor(0xFFFF00FF)
                            .text(Integer.toString(positionNumber++))
                            .position(llText);

                    options.add(textOption);

                    mBaiduMap.addOverlays(options);
                } else {}
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

    //此函数用于按钮点击，手动位置采集
    private void getLocation(){
        GpsPoint currentGpsPoint = new GpsPoint(currentLongitude,currentLatitude,orientationValues[0],rcvDis, gpsPointSet.getNodeNumber());

        if(rcvDis < validDistance && rcvDis > 0) {
            gpsPointSet.addGpsPoint(currentGpsPoint);
            //Log.e(TAG, "当前采样的GPS点相关信息：" + currentGpsPoint.getLatitude() + "#" + currentGpsPoint.getLongitude() + "#当前接收到的距离:" + rcvDis);
            if (gpsPointSet.getNodeNumber() > 2) {
                ArrayList<PosEstimation> posEstimationArray = new ArrayList<>();
                posEstimationArray = gpsPointSet.getNodePosition(currentGpsPoint);

                Point nodePosition = posEstimationArray.get(0).getEstimationPos();

                lastNodeLatitude = nodePosition.getY();
                lastNodeLongitude = nodePosition.getX();

                if (isSecondPoint) {
                    prevSampleLongitude = currentLongitude;
                    prevSampleLatitude = currentLatitude;
                    if (isNaN(lastNodeLatitude) || isNaN(lastNodeLongitude)) {
                        Message tempMsg = new Message();
                        tempMsg.what = TURN_AROUND;
                        handler.sendMessage(tempMsg);
                    }
                }

                //要显示在屏幕上
                mBaiduMap.clear();

                LatLng point = new LatLng(nodePosition.getY(), nodePosition.getX());

                BitmapDescriptor bitmap = BitmapDescriptorFactory
                        .fromResource(R.drawable.icon_temp);

                OverlayOptions option = new MarkerOptions()
                        .position(point)
                        .icon(bitmap);

                mBaiduMap.addOverlay(option);
            } else {
            }
        }
    }

    //UI上的按钮点击进行监听
    public void onClick(View view){
        switch(view.getId()){
            case R.id.searchBtn: //蓝牙
                if(bluetoothGatt == null){
                    actionAlertDialog();
                }else{
                    disconnect_BLE();
                    bluetoothGatt = null;
                }
                break;
            case R.id.setValidDistance:
                EditText msg = findViewById(R.id.validDis);
                String tmpStr = msg.getText().toString();
                validDistance = convertToDouble(tmpStr,0);
                break;
            case R.id.startRecord:
                isStartRecord = true;
                mFileLogger.initData("iLocRSSi");
                break;
            default:
                break;
        }
    }

    //蓝牙选择的窗口弹出
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
                        Log.i(TAG, "Fail to start service discovery:");
                        e.printStackTrace();
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                }
                return;
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, final int status) {
                //此函数用于接收蓝牙传来的数据
                super.onServicesDiscovered(gatt, status);

                //使用的蓝牙的UUID
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

            //接收到的蓝牙数据在这里做处理
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
                super.onCharacteristicChanged(gatt,characteristic);
                //发现服务后的响应函数
                byte[] bytesReceive = characteristic.getValue();
                String msgStr = new String(bytesReceive);

                Log.e(TAG,"接收到包："+msgStr);
                //拆分数据格式
                String[] strs = msgStr.split("#");
                //Log.e(TAG,"strs1:"+strs[0].length());

                boolean isDis = true;
                /*switch(strs[0]){
                    case "dis":
                        rcvDis = convertToDouble(strs[1].substring(0,strs[1].length()-5),0);
                        if(rcvDis==0){
                            rcvDis = convertToDouble(strs[1].substring(0,strs[1].length()-4),0);
                        }
                        Log.e(TAG,"接收到距离："+strs[1].substring(0,strs[1].length()-3));
                        isDis = true;
                        break;
                    case "rssi":
                        rssi = (int)convertToDouble(strs[1],0);
                        rssiArray.add(new Rssi(rssi,new Date()));
                        if(isStartRecord)
                            mFileLogger.writeTxtToFile(new Date().getTime()+"#"+rssi,mFileLogger.getFilePath(),mFileLogger.getFileName());
                        Log.e(TAG,"接收到rssi："+ rssi);
                        break;
                    default:
                        System.out.println("unknown");
                        break;
                }*/
                rcvDis = convertToDouble(strs[1],0);
                rssi = convertToDouble(strs[2],0);
                if(rcvDis == 0)
                    rcvDis = NaN;
                if(rssi == 0)
                    rssi = NaN;
                if(Double.isNaN(rcvDis)){
                    //nan方法判断
                    Log.e(TAG,"进入rssi count判断");
                    if(isRssiCountReverse){
                        delayRssiCount++;
                        if(delayRssiCount > 6) {
                            isRssiCountReverse = false;
                            delayRssiCount = 0;
                        }
                    }else if(rssiArray.size() > 7){
                        Log.e(TAG,"大于7个rssi，进入rssi count判断");
                        rssiLostCount.add(MyUtil.countRssiNanNumber(rssiArray,mFileLogger));
                        if(MyUtil.judgeCountTrend(rssiLostCount,mFileLogger)){
                            Message tempMsg = new Message();
                            tempMsg.what = TURN_REVERSE_RSSI;
                            handler.sendMessage(tempMsg);
                            //mFileLogger.writeTxtToFile("NaN的RSSI的提示",mFileLogger.getFilePath(),mFileLogger.getFileName());
                            isRssiCountReverse = true;
                        }
                    }
                }

                //if(isDis){
                    //距离序列大于100时清空
                    if(distanceArray.size() > 100){
                        distanceArray.clear();
                    }

                    distanceArray.add(rcvDis);

                    if(rcvDis > 0) {
                        //盲走序列采样
                        blindSearchGpsPointSet.addGpsPoint(new GpsPoint(currentLongitude, currentLatitude, orientationValues[0], rcvDis, gpsPointSet.getNodeNumber()));
                        if(isStartRecord)
                            mFileLogger.writeTxtToFile("GPS info:"+currentLatitude+"#"+currentLongitude+"#"+rcvDis,mFileLogger.getFilePath(),mFileLogger.getFileName());
                        isAgainFindDis = true;
                    }
                    //当接收到的蓝牙距离过大时，提示用户回到一个起始点
                    if(rcvDis > 70){
                        Message tempMsg = new Message();
                        tempMsg.what = TOO_FAR;
                        handler.sendMessage(tempMsg);
                    }

                    if(rcvDis < minArrayDis && rcvDis>0){
                        minArrayDis = rcvDis;
                        //validDistance = minArrayDis;
                        Log.e("least","当前动态阈值:"+validDistance);
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
                        //getLocation();
                        isFirstDistance = false;

                        firstLatitude = currentLatitude;
                        firstLongitude = currentLongitude;

                        prevSampleLatitude = currentLatitude;
                        prevSampleLongitude = currentLongitude;
                    }

                //}else{
                    Message tempMsg2 = new Message();
                    tempMsg2.what = NEW_RSSI;
                    handler.sendMessage(tempMsg);
                //}

                //判断蓝牙距离的趋势，若逐渐远离则提示用户往回走
                if(rcvDis > 0 && isDis){
                    if(isReverse){
                        delayCount ++;
                        Log.e(TAG,"当前delaycount："+delayCount);
                        if(delayCount > 6) {
                            isReverse = false;
                            delayCount = 0;
                        }
                    }else if(distanceArray.size()>5){
                        if(MyUtil.judgeTrend(distanceArray,mFileLogger)){
                            Message tempMsg3 = new Message();
                            tempMsg3.what = TURN_REVERSE;
                            handler.sendMessage(tempMsg);
                            isReverse = true;
                        }
                    }

                    if(is_hint_rssi)
                        is_hint_rssi = false;

                }else if(rcvDis == 0 && rssi < 0){
                    if(!is_hint_rssi){
                        if(rssiArray.size() > 8){
                            Log.e(TAG,"进入rssi时间戳判断");
                            if(MyUtil.judgeTimeStamp(rssiArray)){
                                Log.e(TAG,"rssi提示");
                                Message tempMsg4 = new Message();
                                tempMsg4.what = TURN_REVERSE_RSSI;
                                handler.sendMessage(tempMsg);
                                isReverse = true;
                                is_hint_rssi = true;
                            }
                        }
                    }

                }

                //用于从收到距离移动到没收到距离的情况
                if(MyUtil.countDisNanNumber(distanceArray,mFileLogger) > 2 && isAgainFindDis){
                    Message tempMsg5 = new Message();
                    tempMsg5.what = TURN_REVERSE_NEW;
                    handler.sendMessage(tempMsg);
                    //mFileLogger.writeTxtToFile("从收到距离到没收到距离的提示",mFileLogger.getFilePath(),mFileLogger.getFileName());
                    isAgainFindDis = false;
                }


                isDis = false;
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
                devices.add(device);

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

            //Log.e(TAG,"当前位置:"+currentLongitude+"#"+currentLatitude);
            if(lastNodeLatitude > 0.0 && lastNodeLongitude > 0.0){

                LatLng point = new LatLng(lastNodeLatitude, lastNodeLongitude);

                BitmapDescriptor bitmap;
                if(rcvDis > 5){
                    bitmap = BitmapDescriptorFactory
                            .fromResource(R.drawable.icon_temp);

                }else if(!Double.isNaN(rcvDis)){
                    bitmap = BitmapDescriptorFactory
                            .fromResource(R.drawable.icon_en);
                }else{
                    bitmap = BitmapDescriptorFactory
                            .fromResource(R.drawable.icon_temp);
                }

                OverlayOptions option = new MarkerOptions()
                        .position(point)
                        .icon(bitmap);

                mBaiduMap.addOverlay(option);
                //end

                List<LatLng> gpsTrace = new ArrayList<LatLng>();
                List<Integer> colors = new ArrayList<>();

                //画出计算用的GPS序列的轨迹
                /*for(int m = 0;m<gpsPointSet.getNodeNumber();m++){
                    LatLng temp = new LatLng(gpsPointSet.getGpsPoint(m).getLatitude(), gpsPointSet.getGpsPoint(m).getLongitude());
                    gpsTrace.add(temp);

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
                mPolyline = (Polyline) mBaiduMap.addOverlay(gpsPolyline);*/

            }

            if (isFirstLoc) {
                isFirstLoc = false;

                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(20.0f);
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

        if(distance > 10.0){
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
                    searchBtn.setBackgroundResource(R.drawable.bluetooth);
                    break;
                case DISCONN_BLE:
                    searchBtn.setBackgroundResource(R.drawable.bluetooth_red);
                    break;
                case UPDATE_LIST:
                    arrayAdapter.notifyDataSetChanged();
                    break;
                case TIMER_LOCATION:
                    getSecondLocation();
                    break;
                case NEW_DISTANCE:
                    TextView distanceText = findViewById(R.id.distance);
                    /*if(rcvDis > 70.0){
                        distanceText.setText("信号强度：无信号");
                    }else if(rcvDis > 50.0){
                        distanceText.setText("信号强度：弱");
                    }else if(rcvDis > 30.0){
                        distanceText.setText("信号强度：中");
                    }else{
                        distanceText.setText("信号强度：强");
                    }*/
                    distanceText.setText("接收距离:"+rcvDis);
                    TextView rssiText = findViewById(R.id.rssi);
                    rssiText.setText("接收到的rssi:"+rssi);
                    TextView locationText = findViewById(R.id.location);
                    locationText.setText("当前位置:"+currentLatitude+"#"+currentLongitude);
                    if(rcvDis < 10.0 && rcvDis >0){
                        Toast.makeText(MainActivity.this,"到达节点附近，请四处张望，寻找节点",Toast.LENGTH_LONG).show();
                    }
                    break;
                case MOVE_FORWARD:
                    Toast.makeText(MainActivity.this,"请四处走动，寻找信号强度强的地方",Toast.LENGTH_LONG).show();
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
                case SHOW_POINT:
                    //TextView pointNumberText = findViewById(R.id.samplePoint);
                    //pointNumberText.setText("采样点的个数:"+gpsPointSet.getNodeNumber());
                    break;
                case TOO_FAR:
                    GpsPoint minBlindPoint = searchMinPoint(blindSearchGpsPointSet);
                    LatLng minPoint = new LatLng(minBlindPoint.getLatitude(),minBlindPoint.getLongitude());
                    //小于有效距离认定可靠，添加
                    if(minBlindPoint.getDistance() < validDistance){
                        gpsPointSet.addGpsPoint(minBlindPoint);
                    }

                    if(minBlindPoint.getDistance() < 70.0 && minBlindPoint.getDistance() > 0.0){
                        //Toast.makeText(MainActivity.this,"请在地图上寻找有信号的位置再重新寻找",Toast.LENGTH_SHORT).show();
                        mBaiduMap.clear();
                        BitmapDescriptor bitmap = BitmapDescriptorFactory
                                .fromResource(R.drawable.hint_point);

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
                    }
                    break;
                case NEW_RSSI:
                    //TextView rssiText = findViewById(R.id.rssi);
                    //rssiText.setText("接收到的rssi:"+rssi);
                    break;
                case TURN_REVERSE_RSSI:
                    Toast.makeText(MainActivity.this,"请换一个方向寻找接收得到距离的地方",Toast.LENGTH_SHORT).show();
                    break;
                case TURN_REVERSE_NEW:
                    Toast.makeText(MainActivity.this,"正在从有距离走到没有距离",Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };//import android.os.Handler;

/*    private void DrawTestPoint(){
        //LatLng point = new LatLng(30.308767, 120.091941);
        //LatLng linePoint = new LatLng(30.307767, 120.091741);
        //LatLng linePoint2 = new LatLng(30.309567, 120.091541);
        GpsPoint linePoint = new GpsPoint(120.091741,30.307767,orientationValues[0],0,0);
        GpsPoint linePoint2 = new GpsPoint(120.091541,30.309567,orientationValues[0],0,1);
        Point p = new Point(120.091941,30.308767);

        BitmapDescriptor bitmap;

        LatLng symmetricP = MyUtil.getSymmetricPoint(linePoint,linePoint2,p);
        List<OverlayOptions> options = new ArrayList<OverlayOptions>();

        bitmap = BitmapDescriptorFactory
                .fromResource(R.drawable.icon_temp);

        OverlayOptions option = new MarkerOptions()
                .position(new LatLng(p.getY(),p.getX()))
                .icon(bitmap);

        OverlayOptions option2 = new MarkerOptions()
                .position(new LatLng(linePoint.getLatitude(),linePoint.getLongitude()))
                .icon(bitmap);

        OverlayOptions option3 = new MarkerOptions()
                .position(new LatLng(linePoint2.getLatitude(),linePoint2.getLongitude()))
                .icon(bitmap);

        OverlayOptions option4 = new MarkerOptions()
                .position(symmetricP)
                .icon(bitmap);

        OverlayOptions errorCircle = new CircleOptions().fillColor(0x384d73b3)
                .center(new LatLng(p.getY(),p.getX())).stroke(new Stroke(3,0x784d73b3)).radius(20);

        options.add(option);
        options.add(errorCircle);
        options.add(option2);
        options.add(option3);
        options.add(option4);
        Log.e(TAG,"对称点："+symmetricP.latitude+"#"+symmetricP.longitude);
        //mBaiduMap.addOverlay(errorCircle);
        mBaiduMap.addOverlays(options);

    }*/
}
