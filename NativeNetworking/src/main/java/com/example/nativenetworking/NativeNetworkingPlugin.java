package com.example.nativenetworking;

import static androidx.core.app.ActivityCompat.requestPermissions;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.content.pm.PackageManager;
import android.net.Network;
import android.nfc.Tag;
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


import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.unity3d.player.UnityPlayer;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


public class NativeNetworkingPlugin {
    private static final String TAG = "NativeNetworking";
    private static final String GAME_OBJECT_NAME = "Network Manager";
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGattServer bluetoothGattServer;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private List<BluetoothGattService> bluetoothGattServices;
    private BluetoothGatt bluetoothGatt;
    private final static int REQUEST_ENABLE_BT = 1;
    private NetworkInfo networkInfo;
    private String[] PERMISSIONS = {
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.BLUETOOTH_ADVERTISE,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        android.Manifest.permission.BLUETOOTH_SCAN
    };
    private int myIndex;
    private String scanCode;
    private String myName;
    private int NumberOfPlayers;
    private int GameServiceIndex;
    private boolean GameStarted;
    private HashMap<UUID, Integer> UuidDictionary;
    private Boolean connecting;
    private List<BluetoothGattCharacteristic> readQueue;
    private Boolean waitingForResponse;



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

        activity.requestPermissions(PERMISSIONS, 1);
        GameStarted = false;
        waitingForResponse = false;
    }

    public void scanForCentral(String code, String name)
    {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        myName = name;
        if (bluetoothLeScanner != null) {
            scanCode = code;
            connecting = false;
            UUIDDictionaryCreator();
            bluetoothLeScanner.startScan(null, scanSettings, scanCallback);
            Log.d(TAG, "scan started");
        }  else {
            Log.e(TAG, "could not get scanner object");
        }
    }

    public void stopScanForCentral()
    {
        bluetoothLeScanner.stopScan(scanCallback);
    }

    public void advertiseForPeripherals(String code, String name)
    {
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        myName = name;
        if (bluetoothLeAdvertiser != null)
        {
            ParcelUuid parcelUuid = new ParcelUuid(UUID.nameUUIDFromBytes(stringToByteArray(code)));
            AdvertiseData advertiseData = new AdvertiseData.Builder()
                    .addServiceUuid(parcelUuid)
                    .setIncludeDeviceName(false)
                    .build();

            bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
            Log.d(TAG, "advertise started");
        }
        else
        {
            Log.e(TAG, "could not get advertiser object");
        }
    }

    private final ScanCallback scanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            List<ParcelUuid> uuids = result.getScanRecord().getServiceUuids();
            if (uuids != null) {
                for (ParcelUuid uuid : uuids) {
                    if ((new ParcelUuid(UUID.nameUUIDFromBytes(stringToByteArray(scanCode)))).equals(uuid) && !connecting)
                    {
                        device.connectGatt(activity, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE, BluetoothDevice.PHY_LE_2M);
                        connecting = true;
                    }
                }
            }

            //
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

    private void UUIDDictionaryCreator() {
        UuidDictionary = new HashMap<>();
        for (int i = 1; i <= 8; i++) {
            UuidDictionary.put(UUID.nameUUIDFromBytes(new byte[]{(byte)(i)}), i);
        }
    }

    private BluetoothGattService ServiceInitialisation(String hostName, UUID uuid)
    {
        BluetoothGattService bluetoothGattService = new BluetoothGattService(uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic bluetoothGattCharacteristic = new BluetoothGattCharacteristic(UUID.nameUUIDFromBytes(new byte[]{(byte)(1)}), BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
        bluetoothGattCharacteristic.setValue(hostName);
        bluetoothGattService.addCharacteristic(bluetoothGattCharacteristic);
        UuidDictionary = new HashMap<>();
        UuidDictionary.put(UUID.nameUUIDFromBytes(new byte[]{(byte)(1)}), 1);

        for (int i = 2; i <= 8; i++)
        {
            bluetoothGattCharacteristic = new BluetoothGattCharacteristic(UUID.nameUUIDFromBytes(new byte[]{(byte)(i)}), BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
            bluetoothGattService.addCharacteristic(bluetoothGattCharacteristic);
            UuidDictionary.put(UUID.nameUUIDFromBytes(new byte[]{(byte)(i)}), i);
        }

        bluetoothGattCharacteristic = new BluetoothGattCharacteristic(UUID.nameUUIDFromBytes(stringToByteArray("Number")), BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
        bluetoothGattCharacteristic.setValue(floatToByteArray(1));
        bluetoothGattService.addCharacteristic(bluetoothGattCharacteristic);

        myIndex = 1;
        return bluetoothGattService;
    }

    BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED && networkInfo == null) {
                    // We successfully connected, proceed with service discovery
                    networkInfo = new NetworkInfo(gatt);
                    bluetoothLeScanner.stopScan(scanCallback);
                    gatt.discoverServices();
                    Log.d(TAG, "Connected to Server");
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // We successfully disconnected on our own request
                    gatt.close();
                } else {
                    // We're CONNECTING or DISCONNECTING, ignore for now
                }
            } else {
                // An error happened...figure out what happened!
                gatt.close();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "Received Response");
            if (!waitingForResponse) {
                myIndex = new BigInteger(characteristic.getValue()).intValue();
                Log.d(TAG, "My index is :" + myIndex);
                networkInfo.playerInformation.put(myIndex, networkInfo.NewUser(myName));
                bluetoothGattServices.get(GameServiceIndex).getCharacteristic(UUID.nameUUIDFromBytes(new byte[]{(byte)(myIndex)})).setValue(myName);
                gatt.writeCharacteristic(bluetoothGattServices.get(GameServiceIndex).getCharacteristic(UUID.nameUUIDFromBytes(new byte[]{(byte)(myIndex)})));
                waitingForResponse = true;
            }
            else {
                if (!(new String(characteristic.getValue())).equals("N/A")) {
                    networkInfo.playerInformation.put(UuidDictionary.get(characteristic.getUuid()), networkInfo.NewUser(new String(characteristic.getValue())));
                    Log.d(TAG, "New Player: " + new String(characteristic.getValue()));
                    readQueue.remove(0);
                    if (readQueue.size() != 0) {
                        gatt.readCharacteristic(readQueue.get(0));
                    }
                }
                else {
                    while (readQueue.size() > 0) {
                        gatt.setCharacteristicNotification(readQueue.remove(0), true);
                    }
                }

            }
        }

        @Override
        public void onCharacteristicChanged (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (UuidDictionary.get(characteristic.getUuid()) != null) {
                int userIndex = UuidDictionary.get(characteristic.getUuid());
                networkInfo.playerInformation.put(userIndex, networkInfo.NewUser(new String(characteristic.getValue())));
                Log.d(TAG, "New Player: " + new String(characteristic.getValue()));
            }
        }

        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            Log.d(TAG, "Reading this bitch");
            GetPlayerNames();
            gatt.readCharacteristic(readQueue.get(0));
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bluetoothGattServices = gatt.getServices();
                for (int i = 0; i < 3; i++) {
                    if (bluetoothGattServices.get(i).getUuid().equals(UUID.nameUUIDFromBytes(stringToByteArray("Players")))) {
                        Log.d(TAG, "Found Game Services");
                        GameServiceIndex = i;
                        gatt.readCharacteristic(bluetoothGattServices.get(GameServiceIndex).getCharacteristic(UUID.nameUUIDFromBytes(stringToByteArray("Number"))));
                    }
                }

                Log.d(TAG, "Services: " + bluetoothGattServices.size());
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
    }
    ;

    public void GetPlayerNames() {
        readQueue = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            if (i != myIndex) {
                readQueue.add(bluetoothGattServices.get(GameServiceIndex).getCharacteristic(UUID.nameUUIDFromBytes(new byte[]{(byte)i})));
            }
        }
    }

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
            bluetoothGattServer = bluetoothManager.openGattServer(activity, bluetoothGattServerCallback);
            bluetoothGattServer.addService(ServiceInitialisation(myName, UUID.nameUUIDFromBytes(stringToByteArray("Players"))));
            NumberOfPlayers = 1;
            networkInfo = new NetworkInfo(null);
            networkInfo.ChangePlayerName(1, myName);
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

    private final BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice bluetoothDevice, int status, int newState) {
            bluetoothGattServer.setPreferredPhy(bluetoothDevice, BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_OPTION_NO_PREFERRED);

        }

        @Override
        public void onCharacteristicReadRequest (BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic)
        {
            Log.d(TAG, "Read Request Incoming");
            if (characteristic == bluetoothGattServer.getService(UUID.nameUUIDFromBytes(stringToByteArray("Players"))).getCharacteristic(UUID.nameUUIDFromBytes(stringToByteArray("Number")))) {
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[]{(byte)(++NumberOfPlayers)});
                networkInfo.playerInformation.put(NumberOfPlayers, networkInfo.NewUser("Player " + NumberOfPlayers));
                networkInfo.AddDevice(NumberOfPlayers, device);
                Log.d(TAG, "Sent Response");
            }
            else {
                if (networkInfo.playerInformation.get(UuidDictionary.get(characteristic.getUuid())) != null) {
                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, stringToByteArray(networkInfo.playerInformation.get(UuidDictionary.get(characteristic.getUuid())).PlayerName));
                }
                else {
                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, stringToByteArray("N/A"));
                }
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d(TAG, "Write request received");
            if (!GameStarted) {
                characteristic.setValue(value);
                Log.d(TAG, "Number: " + UuidDictionary.get(characteristic.getUuid()) + ", Name: " + new String(value));
                networkInfo.ChangePlayerName(UuidDictionary.get(characteristic.getUuid()), new String(value));
                System.out.println(Arrays.asList(networkInfo.playerInformation));
                Log.d(TAG,"Client name is: " + new String(value));
                for (int i = 2; i <= networkInfo.playerInformationSize; i++) {
                    bluetoothGattServer.notifyCharacteristicChanged(networkInfo.playerInformation.get(i).bluetoothDevice, characteristic, true);
                }
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, stringToByteArray(myName));
            }
        }
    };


    private class NetworkInfo
    {
        HashMap<Integer, PlayerInfo> playerInformation;
        int playerInformationSize;

        private NetworkInfo(BluetoothGatt m_bluetoothGatt) {
            playerInformation = new HashMap<Integer, PlayerInfo>();
            playerInformation.put(1, new PlayerInfo(m_bluetoothGatt, "Host"));
            playerInformationSize = 1;
        }

        public void ChangePlayerName(int index, String name){
            playerInformation.get(index).PlayerName = name;
        }

        public PlayerInfo NewUser(String name) {
            playerInformationSize++;
            return new PlayerInfo(null, name);
        }

        public void AddDevice(int index, BluetoothDevice device) {
            playerInformation.get(index).bluetoothDevice = device;
        }

        private class PlayerInfo
        {
            public BluetoothGatt PlayerGattObject;
            private int playerIndex;
            public String PlayerName;
            public BluetoothDevice bluetoothDevice;
            public float PlayerPositionX;
            public float PlayerPositionY;
            public float PlayerVelocityX;
            public float PlayerVelocityY;
            public float TimeSinceRaceStart;


            public PlayerInfo(BluetoothGatt playerGattObject, String name)
            {
                PlayerGattObject = playerGattObject;
                PlayerName = name;
                PlayerPositionX = 0.0f;
                PlayerPositionY = 0.0f;
                PlayerVelocityX = 0.0f;
                PlayerVelocityY = 0.0f;
                TimeSinceRaceStart = 0.0f;
            }
        }
    }

    public byte[] floatToByteArray(float value)
    {
        int intBits =  Float.floatToIntBits(value);
        return new byte[] {(byte) (intBits >> 24), (byte) (intBits >> 16), (byte) (intBits >> 8), (byte) (intBits) };
    }

    public byte[] stringToByteArray(String value)
    {
        return value.getBytes();
    }

}
