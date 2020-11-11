package com.example.smartbulbcontroller;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    // Initializes Bluetooth adapter.
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    BluetoothGatt bluetoothGatt;
    ScanCallback leScanCallback;

    private Button onButton;
    private Button OffButton;
    private Button OnBeepButton;
    private Button OffBeepButton;
    TextView TemperatureNotify;
    private ProgressDialog progressDialog;
    private BluetoothAdapter.LeScanCallback mleScanCallback;
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";

//    df5302f9-537b-aed4-0618-1ebe135a1000 - Chandu
//    df9f381b-e0b6-5564-8528-b4c0ab556265 = chottu
//    dfc06d52-2190-322b-b6d3-39b595b463 = mine

    public static final String bleApp = "df9f381b-e0b6-5564-8528-b4c0ab556265";
    public static final String bulb = "FB959362-F26E-43A9-927C-7E17D8FB2D8D";
    public static final String temp = "0CED9345-B31F-457D-A6A2-B3DB9B03E39A";
    public static final String beep = "EC958823-F26E-43A9-927C-7E17D8F32A90";

    //static variables
    private static final int REQUEST_ENABLE_BT = 1111;
    private static final int REQUEST_PERMISSION = 2222;
    private static final int REQUEST_ENABLE_LOCATION = 3333;
    private static final String TAG = "okay";

    //variables
    ArrayList<BluetoothDevice> BTs = new ArrayList<>();
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //if device does not support ble, app is of no use ==> it will close itself
        CheckIfDeviceSupportBLE();
        onButton = findViewById(R.id.OnButton);
        onButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) v
                        .getTag();
                characteristic.setValue(new byte[] { (byte) 0x03 });
                if (bluetoothGatt.writeCharacteristic(characteristic)) {
                    Log.d("demo","Bulb switched on");
                }
            }
        });

        OffButton = findViewById(R.id.OffButton);
        OffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) v
                        .getTag();
                characteristic.setValue(new byte[] { (byte) 0x00 });
                if (bluetoothGatt.writeCharacteristic(characteristic)) {
                    Log.d("demo","Bulb switched off");
                }
            }
        });

        OnBeepButton =  findViewById(R.id.onBeepButton);
        OnBeepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) v
                        .getTag();
                characteristic.setValue(new byte[] { (byte) 0x03 });
                if (bluetoothGatt.writeCharacteristic(characteristic)) {
                    Log.d("demo","Beep sound is on");
                }
            }
        });

        OffBeepButton = findViewById(R.id.offBeepButton);
        OffBeepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) v
                        .getTag();
                characteristic.setValue(new byte[] { (byte) 0x00 });
                if (bluetoothGatt.writeCharacteristic(characteristic)) {
                    Log.d("demo","Beep sound is off");
                }
            }
        });
        TemperatureNotify= findViewById(R.id.temperatureText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FlowToScanForBLEDevices();
    }


    void FlowToScanForBLEDevices(){
        if (CheckIfPermissionGranted() ){
            // if bluetooth permissin granted granted
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
           if (CheckIfBTisEnabled() && CheckIfLocationIsOn()){
               mleScanCallback =  new BluetoothAdapter.LeScanCallback() {
                   @Override
                   public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("demo", "got the device finally!");
                                Log.d(TAG, "onScanResult: BT Device found=>"+device.getName()+"  Addresss=>"+device.getAddress());
                                bluetoothAdapter.stopLeScan(mleScanCallback);
                                bluetoothGatt = device.connectGatt(MainActivity.this, false, gattCallback);
                            }
                        });
                   }
               };
               showProgressBarDialog();
               Handler handler = new Handler();
               handler.postDelayed(new Runnable() {
                   @Override
                   public void run() {
                       bluetoothAdapter.stopLeScan(mleScanCallback);
                       hideProgressBarDialog();
                       if(bluetoothGatt == null){
                           Toast.makeText(MainActivity.this, "Sorry no device found", Toast.LENGTH_SHORT).show();
                       }
                   }
               }, 20000);
               Log.d(TAG, "FlowToScanForBLEDevices: le scan started=>"+
                       bluetoothAdapter.startLeScan(new UUID[]{UUID.fromString(bleApp)},
                               mleScanCallback));
           }
        }
    }

    private final BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    Log.d("demo",gatt.getServices().toString());
                    Log.d("demo","OnConnectionStateChange");
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                bluetoothGatt.discoverServices());
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        hideProgressBarDialog();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this, "Device disconnected", Toast.LENGTH_SHORT).show();
                                TemperatureNotify.setText("");
                            }
                        });
                        Log.i(TAG, "Disconnected from GATT server.");
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    Log.d("demo","onServicesDiscovered");
                    for (BluetoothGattService service : gatt.getServices()) {
                        if ((service == null) || (service.getUuid() == null)) {
                            continue;
                        }

                        if (bleApp.equalsIgnoreCase(service
                                .getUuid().toString())) {
                            onButton.setTag(service.getCharacteristic(UUID
                                            .fromString(bulb)));
                            OffButton.setTag(service.getCharacteristic(UUID
                                            .fromString(bulb)));
                            OnBeepButton.setTag(service.getCharacteristic(UUID
                                            .fromString(beep)));
                            OffBeepButton.setTag(service.getCharacteristic(UUID
                                            .fromString(beep)));
                            TemperatureNotify.setTag(service.getCharacteristic(UUID
                                    .fromString(temp)));

                            BluetoothGattCharacteristic characteristic =service.getCharacteristic(UUID
                                    .fromString(temp));

                            gatt.setCharacteristicNotification(characteristic,true);
                            gatt.readCharacteristic(characteristic);
                        }
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    Log.d("demo", "onCharacteristicRead");
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (temp.equalsIgnoreCase(characteristic.getUuid().toString())) {
                            final String tempValue = characteristic.getStringValue(0);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Log.d("demo", "temperature value" + tempValue);
                                    TemperatureNotify.setText(tempValue + " F");
                                }
                            });
                        }
                        hideProgressBarDialog();
                    }else{
                        hideProgressBarDialog();
                    }
                }

                @Override
                // Characteristic notification
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    if (temp.equalsIgnoreCase(characteristic.getUuid().toString())) {
                        final String tempValue = characteristic.getStringValue(0);
                        Log.d("demo","temperature value on notify"+tempValue);
                        runOnUiThread(new Runnable() {
                            public void run() {
//                                Log.d("demo","temperature value on notify"+tempValue);
                                TemperatureNotify.setText(tempValue + " F");
                            };
                        });
                    }
                    hideProgressBarDialog();
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                }
            };

    private boolean CheckIfLocationIsOn() {
        Log.d(TAG, "CheckIfLocationIsOn: called");
        try {
            int t = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            Log.d(TAG, "CheckIfLocationIsOn: location setting mode=>"+t);
            if (t==0){
                Toast.makeText(this, "Please Turn on Location", Toast.LENGTH_LONG).show();
                Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(enableLocationIntent, REQUEST_ENABLE_LOCATION);
                return false;
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

    private Boolean CheckIfPermissionGranted() {
        Log.d(TAG, "CheckIfPermissionGranted: called");
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_ADMIN,Manifest.permission.ACCESS_FINE_LOCATION};
        for(String s : permissions){
            if (ContextCompat.checkSelfPermission(this, s) == PackageManager.PERMISSION_DENIED) {
                Log.d(TAG, "CheckIfPermissionGranted: "+s+" denied");
                ActivityCompat.requestPermissions(this,
                        new String[]{s},
                        REQUEST_PERMISSION);
                return false;
            }
        }
        return true;
    }

    private boolean CheckIfBTisEnabled() {
        Log.d(TAG, "CheckIfBTisEnabled: called");
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "onActivityResult: called");
        super.onActivityResult(requestCode, resultCode, data);
        ///////////////
        if (requestCode == REQUEST_PERMISSION && resultCode==RESULT_OK){
            Log.d(TAG, "onActivityResult: Permissions Granted");
            Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show();
            FlowToScanForBLEDevices();
        }else if (requestCode == REQUEST_PERMISSION && resultCode!=RESULT_OK){
            Log.d(TAG, "onActivityResult: App will not work Permissions");
            Toast.makeText(this, "App will not work without Permissions", Toast.LENGTH_SHORT).show();
            finish();
        } else if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            Log.d(TAG, "Bluetooth enable successful");
            Toast.makeText(this, "Bluetooth enable successful", Toast.LENGTH_SHORT).show();
            FlowToScanForBLEDevices();
        } else if (requestCode == REQUEST_ENABLE_BT && resultCode != RESULT_OK) {
            Log.d(TAG, "onActivityResult: App does not work with Bluetooth disabled");
            Toast.makeText(this, "App does not work with Bluetooth disabled", Toast.LENGTH_SHORT).show();
            finish();
        } else if (requestCode == REQUEST_ENABLE_LOCATION && resultCode!=RESULT_OK){
            // again check if setting is changed
            try {
                int t = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
                Log.d(TAG, "CheckIfLocationIsOn: location setting mode=>"+t);
                if (t==0){
                    Log.d(TAG, "onActivityResult: App will not work without Location on");
                    Toast.makeText(this, "App will not work without Location on", Toast.LENGTH_SHORT).show();
                    finish();
                }else{
                    Log.d(TAG, "onActivityResult: location setting changed to enabled");
                    Toast.makeText(this, "Location is enabled", Toast.LENGTH_SHORT).show();
                    FlowToScanForBLEDevices();
                }
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void CheckIfDeviceSupportBLE() {
        Log.d(TAG, "CheckIfDeviceSupportBLE: called");
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public void showProgressBarDialog()
    {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public void hideProgressBarDialog()
    {
        progressDialog.dismiss();
    }
}