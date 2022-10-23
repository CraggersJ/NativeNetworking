package com.example.nativenetworking;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.content.*;

import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.ScanSettings;


import com.unity3d.player.UnityPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NativeNetworkingPlugin {
    private static final String TAG = "NativeNetworking";
    private static final String GAME_OBJECT_NAME = "Network Manager";
    private boolean scanning;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private final static int REQUEST_ENABLE_BT = 1;
    private static byte[] serviceUUID = {0x64, 0x23};
    private static byte[] serviceData = {0x61, 0x62, 0x63, 0x64};


    private Activity activity;

    public NativeNetworkingPlugin(Activity activity)
    {
        this.activity = activity;
        Log.d(TAG, "Initialized NativeNetworkingPlugin class");

        bluetoothManager = activity.getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void scanForPeripherals()
    {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner != null) {
            List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
            ParcelUuid parcelUuid = new ParcelUuid(UUID.nameUUIDFromBytes(serviceUUID));
            ScanFilter scanFilter = new ScanFilter.Builder()
                    .setServiceData(parcelUuid, serviceData)
                    .build();
            scanFilters.add(scanFilter);
            bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback);
            Log.d(TAG, "scan started");
        }  else {
            Log.e(TAG, "could not get scanner object");
        }
    }

    public void advertiseForCentral()
    {
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (bluetoothLeAdvertiser != null) {
            ParcelUuid parcelUuid = new ParcelUuid(UUID.nameUUIDFromBytes(serviceUUID));
            AdvertiseData advertiseData = new AdvertiseData.Builder()
                    .addServiceData(parcelUuid, serviceData)
                    .setIncludeDeviceName(false)
                    .build();

            bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
            Log.d(TAG, "advertise started");
        }  else {
            Log.e(TAG, "could not get advertiser object");
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device.getName() != null) {
                Log.d(TAG, "Found Something");
                String name = device.getName();
                Log.d(TAG, name);
            }
            else
            {

                Log.d(TAG, "Code");
                Log.d(TAG, new String(result.getScanRecord().getServiceData(new ParcelUuid(UUID.nameUUIDFromBytes(serviceUUID)))));
            }

            // ...do whatever you want with this found device
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            // Ignore for now
        }

        @Override
        public void onScanFailed(int errorCode) {
            // Ignore for now
            Log.d(TAG, "Scan Failed");
        }
    };

    ScanSettings scanSettings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(0L)
            .build();

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.v(TAG, "onStartSuccess");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "Advertising onStartFailure: " + errorCode);
            super.onStartFailure(errorCode);
        }
    };

    AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
            .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
            .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
            .build();
}
