package com.example.tongmin.myble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.tongmin.myble.util.DebugFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private ListView list ;
    private Button bt ;
    private BluetoothLeService mBluetoothLeService;
    private static final long SCAN_PERIOD = 10000;
    private Handler mHandler = new Handler();
    private boolean mScanning ;
    private DeviceAdapter deviceAdapter ;
    private ArrayList<BluetoothDevice> listDevice  = new ArrayList<BluetoothDevice>();
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        //从BluetoothLeService中获取的对象
        mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
        if (!mBluetoothLeService.initialize()) {
            finish();
        }
        Log.e("xhc","mBluetoothLeService 服务"+mBluetoothLeService);
        // Automatically connects to the device upon successful start-up initialization.
        //在这里自动连接的蓝牙
//        mBluetoothLeService.connect(mDeviceAddress);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.e("xhc","连接断开");
        mBluetoothLeService = null;
    }
};
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!listDevice.contains(device)){
                                //不重复添加
                                listDevice.add(device);
                                deviceAdapter.setListDevice(listDevice);
                            }
                        }
                    });
                }
            };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        findViewById();
        init();
    }

    private void findViewById(){
        list = (ListView)findViewById(R.id.list);
        bt = (Button)findViewById(R.id.scan);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BluetoothDevice device = listDevice.get(position);
        if(mBluetoothLeService != null){

            boolean flag = mBluetoothLeService.connect(device.getAddress());

        }
    }

    private void init(){
        deviceAdapter = new DeviceAdapter(this);
        list.setAdapter(deviceAdapter);
        list.setOnItemClickListener(this);
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this,"不支持蓝牙4.0", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if(mBluetoothAdapter == null){
            Toast.makeText(this,"获取失败!", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(true);
            }
        });

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        startService(gattServiceIntent);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }
    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    // Handles various events fired by the Service.
// ACTION_GATT_CONNECTED: connected to a GATT server.
// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
// ACTION_DATA_AVAILABLE: received data from the device. This can be a
// result of read or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                //连接的状态
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //断开连接的状态
            } else if (BluetoothLeService.
                    ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //找到services被发现的状态
                // Show all the supported services and characteristics on the
                // user interface.
                //在这里找到要要读的特征值或者需要通知的特征值
                LogServiceAndChara(mBluetoothLeService.getSupportedGattServices());

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //接收到数据的状态
                Log.e("xhc","接收到的数据"+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void LogServiceAndChara(List<BluetoothGattService> gattServices){
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
//        StringBuilder str = new StringBuilder();
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
//            str.append("---------Service--------");
//            String uuid = gattService.getUuid().toString();
//            str.append(uuid+"\n\r");
//            String name = SampleGattAttributes.lookup(uuid, "不知道");
//            str.append(name+"\n\r");
            if(gattService.getUuid().toString().equals(SampleGattAttributes.DEVICE_SERVICE)){
                //找到了这个服务
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                for(BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics){
                    if(gattCharacteristic.getUuid().toString().equals(SampleGattAttributes.DEVICE_CHARACTER)){
                        //找到了对应的服务
                        Log.e("xhc","找到了对应的服务");
                         if((gattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY)>0){
                            mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,true);
                             mNotifyCharacteristic = gattCharacteristic;
                             Log.e("xhc","直接通知");
                         }
                        else if((gattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_READ)>0){
                            if(mNotifyCharacteristic != null){
                                mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,false);
                                mNotifyCharacteristic = null;
                            }
                             Log.e("xhc","read读取");
                             mBluetoothLeService.readCharacteristic(gattCharacteristic);
                         }
                    }
                }
            }


        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }
}
