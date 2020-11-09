package com.example.smartbulbcontroller;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
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
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {



    // Initializes Bluetooth adapter.
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean mScanning;
    BluetoothGatt bluetoothGatt;
    ScanCallback leScanCallback;


    //static variables
    private static final long SCAN_PERIOD = 10000;          // Stops scanning after 10 seconds.
    private static final int REQUEST_ENABLE_BT = 1111;
    private static final int REQUEST_PERMISSION = 2222;
    private static final int REQUEST_ENABLE_LOCATION = 3333;
    private static final String TAG = "okay";

    //variables
    ArrayList<BluetoothDevice> BTs = new ArrayList<>();
    private Handler handler = new Handler();
    EditText et_deviceList;
    boolean is_loaction_Enable = false;

    ///// temporary
    int limit = 0;  // for no. of calls for flow

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //if device does not support ble, app is of no use ==> it will close itself
        CheckIfDeviceSupportBLE();


        /////////////////////////////////   Method 1
        // getting UI components
        et_deviceList = findViewById(R.id.ETML_deviceList);

        // flow
        // CheckIfPermissionGranted();  then
        // CheckIfBTisEnabled() then
        // CheckIfLocationIsOn() then start bluetooth process
        FlowToScanForBLEDevices();
        //////////////////////////////////  Method 1 End


//        ConnectToBTDevice();

    }
    void FlowToScanForBLEDevices(){
        limit++;
        if (limit>5){
            return;
        }
        if (CheckIfPermissionGranted() ){
            // if bluetooth permissin granted granted
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();

           if (CheckIfBTisEnabled() && CheckIfLocationIsOn()){
               ////////// method 1
//               bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
//               ScanForBLEDevices();
               ///////// method 1 end

               ///////// method 2
               BluetoothAdapter.LeScanCallback mleScanCallback =  new BluetoothAdapter.LeScanCallback() {
                   @Override
                   public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                BTs.add(device);
                                Log.d(TAG, "onScanResult: BT Device found=>"+device.getName()+"  Addresss=>"+device.getAddress());
                                String rs  = "";
                                for (BluetoothDevice b : BTs){
                                    rs+=b.getName()+"\n";
                                }
                                et_deviceList.setText(rs);
                            }
                        });
                   }
               };
               Log.d(TAG, "FlowToScanForBLEDevices: le scan started=>"+bluetoothAdapter.startLeScan(new UUID[]{UUID.fromString("dfad74bb-6584-b699-4be5-6d8b8076ca97")},mleScanCallback));
               ////////
           }
        }
    }

    private boolean CheckIfLocationIsOn() {
        Log.d(TAG, "CheckIfLocationIsOn: called");
        /////////////   Method 1
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
        //////////  method 1 end
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
                        // ActivityCompat.shouldShowRequestPermissionRationale(this,s);
                return false;
            }
        }
        return true;
    }

    private void ConnectToBTDevice() {
        //connecting to first BT device GAtt server
        if (BTs.size()>0){
//            bluetoothGatt = BTs.get(0).connectGatt(this, false, gattCallback);
        }
        Toast.makeText(this, "Not Bluetooth Device found to connect ", Toast.LENGTH_SHORT).show();

    }

    void ScanForBLEDevices() {
        Log.d(TAG, "ScanForBLEDevices: called");
        Toast.makeText(this, "Scanning for BLE devices", Toast.LENGTH_SHORT).show();
        // Device scan callback.
        leScanCallback =
                new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);
                        BTs.add(result.getDevice());
                        Log.d(TAG, "onScanResult: BT Device found=>"+result.getDevice().getName());
                        String rs  = "";
                        for (BluetoothDevice b : BTs){
                            rs+=b.getName()+"\n";
                        }
                        et_deviceList.setText(rs);
                        // stop the scanning now
                        bluetoothLeScanner.stopScan(leScanCallback);
                    }
                };

        // trying to get only one result
        bluetoothLeScanner.startScan(leScanCallback);

//        if (!mScanning) {
//            // Stops scanning after a pre-defined scan period.
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mScanning = false;
//                    Toast.makeText(MainActivity.this, "10 second over", Toast.LENGTH_SHORT).show();
//                    bluetoothLeScanner.stopScan(leScanCallback);
//                }
//            }, SCAN_PERIOD);
//
//            Log.d(TAG, "ScanForBLEDevices: started scanning for devices");
//            mScanning = true;
//            bluetoothLeScanner.startScan(leScanCallback);
//            //for scanning specific UUIDs
//            //bluetoothLeScanner.startScan([''],leScanCallback);
//        } else {
//            mScanning = false;
//            bluetoothLeScanner.stopScan(leScanCallback);
//        }


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
        }
//        else if(requestCode == REQUEST_ENABLE_LOCATION && resultCode==RESULT_OK){
//            Log.d(TAG, "onActivityResult: location Enabled");
//            FlowToScanForBLEDevices();
//        }
        else if (requestCode == REQUEST_ENABLE_LOCATION && resultCode!=RESULT_OK){
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
        ///////////////
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
}