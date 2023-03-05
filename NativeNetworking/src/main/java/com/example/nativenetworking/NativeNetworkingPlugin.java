package com.example.nativenetworking;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanCallback;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


public class NativeNetworkingPlugin {

    //////////////////////////////////////
    // SERVICE AND CHARACTERISTIC NAMES //
    //////////////////////////////////////

    private static final UUID CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    private static final UUID GAME_SERVICE = UUID.fromString("67c458d9-e20f-418a-b456-5c0c3813e0fa");
    private static final UUID NUMBER_OF_PLAYERS = UUID.fromString("c18239f7-c810-44ed-ae8d-bbc36f35e807");
    private static final UUID START_GAME = UUID.fromString("c740bb43-1f2f-4707-b994-94c35ec5f88c");
    private static final UUID NEXT_MAP = UUID.fromString("876f89f5-633d-45d8-9f89-8bc661761cbf");
    private static final UUID PLAYER_1_NAME = UUID.fromString("c9c17d68-b722-4af8-b5da-f695a5832fe8");
    private static final UUID PLAYER_2_NAME = UUID.fromString("14a3814a-6283-4c39-9488-1080103818bb");
    private static final UUID PLAYER_3_NAME = UUID.fromString("40c8e0ea-336d-479f-ab69-4340d9201037");
    private static final UUID PLAYER_4_NAME = UUID.fromString("96341701-e517-4f38-a8e3-dc03fd629ad4");

    private static final UUID GAME_DATA_SERVICE = UUID.fromString("951baba4-5da5-4223-8c58-cd050645608a");
    private static final UUID PLAYER_1_DATA = UUID.fromString("50e3a222-e55f-4df4-94d8-e8c64f756fda");
    private static final UUID PLAYER_2_DATA = UUID.fromString("615942fe-9852-41d4-807d-34f17fc7ecf0");
    private static final UUID PLAYER_3_DATA = UUID.fromString("54a87a82-011f-4fa0-8fe3-0689abed490f");
    private static final UUID PLAYER_4_DATA = UUID.fromString("e34294bd-ddef-4e8e-b6f0-1e9cfd32de8a");
    private static final UUID PLAYER_1_FINISH = UUID.fromString("a892d2e4-4c43-4285-81e4-8ea582f963b4");
    private static final UUID PLAYER_2_FINISH = UUID.fromString("cbc5c5bc-b59a-49d3-bb4b-df19e603280f");
    private static final UUID PLAYER_3_FINISH = UUID.fromString("1d0520f5-7612-4d8c-b50c-bfabcf666f0f");
    private static final UUID PLAYER_4_FINISH = UUID.fromString("af5b6936-a764-403e-a734-bb55080a820e");




    private static final List<UUID> PLAYER_NAMES = new ArrayList<UUID>() {
        {
            add(PLAYER_1_NAME);
            add(PLAYER_2_NAME);
            add(PLAYER_3_NAME);
            add(PLAYER_4_NAME);
        }
    };

    private static final List<UUID> PLAYER_DATA = new ArrayList<UUID>() {
        {
            add(PLAYER_1_DATA);
            add(PLAYER_2_DATA);
            add(PLAYER_3_DATA);
            add(PLAYER_4_DATA);
        }
    };

    private static final List<UUID> PLAYER_FINISH_TIMES = new ArrayList<UUID>() {
        {
            add(PLAYER_1_FINISH);
            add(PLAYER_2_FINISH);
            add(PLAYER_3_FINISH);
            add(PLAYER_4_FINISH);
        }
    };


    private static final HashMap<UUID, Integer> UUID_TO_PLAYER_INDEX = new HashMap<UUID, Integer>() {
        {
            put(PLAYER_1_NAME, 1);
            put(PLAYER_1_DATA, 1);
            put(PLAYER_1_FINISH, 1);
            put(PLAYER_2_NAME, 2);
            put(PLAYER_2_DATA, 2);
            put(PLAYER_2_FINISH, 2);
            put(PLAYER_3_NAME, 3);
            put(PLAYER_3_DATA, 3);
            put(PLAYER_3_FINISH, 3);
            put(PLAYER_4_NAME, 4);
            put(PLAYER_4_DATA, 4);
            put(PLAYER_4_FINISH, 4);
        }
    };



    //////////////////////
    // SHARED VARIABLES //
    //////////////////////



    private final Activity activity;

    private static final String TAG = "NativeNetworking";
    private static final String GAME_OBJECT_NAME = "Network Manager";

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private final static int REQUEST_ENABLE_BT = 1;
    private final String[] PERMISSIONS = {
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.BLUETOOTH_ADVERTISE,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            android.Manifest.permission.BLUETOOTH_SCAN
    };

    private String scanCode;
    private String myName;

    private int[] receivedPackets;
    private int[] sentPackets;



    //////////////////////
    // COMMON FUNCTIONS //
    //////////////////////



    public NativeNetworkingPlugin(Activity activity)
    {
        this.activity = activity;
    }

    public void CheckBluetoothAndPermissions()
    {
        bluetoothManager = activity.getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        activity.requestPermissions(PERMISSIONS, 1);
    }

    private byte[] intToByteArray(int i)
    {
        return new byte[] {(byte)i};
    }



    public byte[] stringToByteArray(String value)
    {
        return value.getBytes();
    }



    public String CreateJsonPlayerNameString(int index, String playerName, boolean isMe)
    {
        try {
            String jsonString = new JSONObject()
                    .put("playerIndex", index)
                    .put("playerName", playerName)
                    .put("isMe", isMe)
                    .toString();

            return jsonString;
        }
        catch (JSONException e) {return "";}
    }



    public String CreateJsonPlayerPositionString(int playerIndex, float positionX, float positionY, float velocityX, float velocityY, int gravityBit)
    {
        try {
            DecimalFormat df = new DecimalFormat("###.###");
            String jsonString = new JSONObject()
                    .put("pI", playerIndex)
                    .put("pX", df.format(positionX))
                    .put("pY", df.format(positionY))
                    .put("vX", df.format(velocityX))
                    .put("vY", df.format(velocityY))
                    .put("gB", df.format(gravityBit))
                    .toString();

            return jsonString;
        }
        catch (JSONException e) {return "";}
    }

    public String CreateFinishTimeString(int playerIndex, float finishTime)
    {
        try {
            DecimalFormat df = new DecimalFormat("###.###");
            String jsonString = new JSONObject()
                    .put("pI", playerIndex)
                    .put("fT", df.format(finishTime))
                    .toString();

            return jsonString;
        }
        catch (JSONException e) {return "";}
    }

    public void PlayerMovement(float positionX, float positionY, float velocityX, float velocityY, int gravityBit)
    {
        if (amHost) {
            BluetoothGattCharacteristic hostGameDataCharacteristic = bluetoothGattServer.getService(GAME_DATA_SERVICE).getCharacteristic(PLAYER_1_DATA);
            hostGameDataCharacteristic.setValue(CreateJsonPlayerPositionString(1, positionX, positionY, velocityX, velocityY, gravityBit));
            for (Integer key : clientDevices.keySet())
            {
                    bluetoothGattServer.notifyCharacteristicChanged(clientDevices.get(key), hostGameDataCharacteristic, false);
                    sentPackets[key-1]++;

            }
        }
        else {
            BluetoothGattCharacteristic myGameDataCharacteristic = bluetoothGattServices.get(gameDataServiceIndex).getCharacteristic(PLAYER_DATA.get(myIndex-1));
            myGameDataCharacteristic.setValue(CreateJsonPlayerPositionString(myIndex, positionX, positionY, velocityX, velocityY, gravityBit));
            bluetoothGatt.writeCharacteristic(myGameDataCharacteristic);
            for (int i = 0; i < 4; i++)
            {
                if (i + 1 != myIndex)
                {
                    sentPackets[i]++;
                }
            }
        }

    }

    public void FinishedRace(float finishTime)
    {
        if (amHost) {
            String json = CreateFinishTimeString(1, finishTime);
            bluetoothGattServer.getService(GAME_DATA_SERVICE).getCharacteristic(PLAYER_1_FINISH).setValue(json);
            NewPlayerFinished(json);
        }
        else {
            BluetoothGattCharacteristic myFinishTimeCharacteristic = bluetoothGattServices.get(gameDataServiceIndex).getCharacteristic(PLAYER_FINISH_TIMES.get(myIndex-1));
            String json = CreateFinishTimeString(myIndex, finishTime);
            myFinishTimeCharacteristic.setValue(json);
            bluetoothGatt.writeCharacteristic(myFinishTimeCharacteristic);
            //pingStart = Instant.now();
            //Log.d("Sent", "1");
            UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "PlayerFinishTime", json);
        }

    }



    public void LeaveGame()
    {
        if (amHost)
        {
            bluetoothGattServer.close();
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
            amHost = false;

            Log.d(TAG, "Packets Sent to Player 2: " + sentPackets[1]);
            Log.d(TAG, "Packets Sent to Player 3: " + sentPackets[2]);
            Log.d(TAG, "Packets Sent to Player 4: " + sentPackets[3]);
            Log.d(TAG, "Packets Received from Player 2: " + receivedPackets[1]);
            Log.d(TAG, "Packets Received from Player 3: " + receivedPackets[2]);
            Log.d(TAG, "Packets Received from Player 4: " + receivedPackets[3]);
        }
        else
        {
            if (bluetoothGatt != null)
            {
                bluetoothGatt.disconnect();

                for (int i = 0; i < 4; i++)
                {
                    if (i + 1 != myIndex)
                    {
                        Log.d(TAG, "Packets Sent to Player" + (i+1) + " : " + sentPackets[i]);
                        Log.d(TAG, "Packets Received from Player" + (i+1) + " : " + receivedPackets[i]);
                    }
                }
            }
            if (scanning)
            {
                bluetoothLeScanner.stopScan(scanCallback);
                scanning = false;
                connecting = false;
            }
        }

        UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "LeaveGame", "");
    }



    ////////////////////
    // HOST FUNCTIONS //
    ////////////////////



    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothGattServer bluetoothGattServer;

    private int addServiceFails;

    private boolean amHost;

    private HashMap<Integer, BluetoothDevice> clientDevices;
    private HashMap<BluetoothDevice, Integer> clientDeviceIndexes;
    private HashMap<BluetoothDevice, Boolean> clientJoiningInProgress;
    private int NumberOfPlayers;
    private int numberOfPlayersJoiningInProgress;
    private int numberOfPlayersFinished;
    private boolean isGameEnding;

    private HashMap<BluetoothDevice, Boolean> clientsFinished;

    public void AdvertiseForPeripherals(String code, String name)
    {
        scanCode = code;
        myName = name;
        addServiceFails = 0;
        amHost = true;
        numberOfPlayersJoiningInProgress = 0;
        sentPackets = new int[4];
        receivedPackets = new int[4];

        bluetoothGattServer = bluetoothManager.openGattServer(activity, bluetoothGattServerCallback);
        bluetoothGattServer.addService(gameServiceConstructor());
    }

    private UUID byteArraytoUUID(byte[] bytes)
    {
        byte[] tooAdd = new byte[16-bytes.length];
        byte[] result = new byte[16];
        System.arraycopy(bytes, 0, result, 0, bytes.length);
        System.arraycopy(tooAdd, 0, result, bytes.length, 13);
        ByteBuffer bb = ByteBuffer.wrap(result);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

    AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
            .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY )
            .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
            .build();




    private AdvertiseData createAdvertisingPacket (String code)
    {
        UUID codeUuid = byteArraytoUUID(stringToByteArray(code));

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(codeUuid))
                .setIncludeDeviceName(false)
                .build();

        return advertiseData;
    }




    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);

        }

        @Override
        public void onStartFailure(int errorCode) {
            UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "NetworkErrorLog", "1");
        }
    };




    private final BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback()
    {

        @Override
        public void onConnectionStateChange(BluetoothDevice bluetoothDevice, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED && NumberOfPlayers <= 4)
            {
                bluetoothGattServer.setPreferredPhy(bluetoothDevice, BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_LE_2M_MASK, BluetoothDevice.PHY_OPTION_NO_PREFERRED);
                for (int i = 2; i <= 4; i++)
                {
                    if (!clientDevices.containsKey(i))
                    {
                        //Log.d(TAG, "Player index: " + i);
                        clientDevices.put(i, bluetoothDevice);
                        clientDeviceIndexes.put(bluetoothDevice, i);
                        NumberOfPlayers++;
                        if (++numberOfPlayersJoiningInProgress == 1)
                        {
                            UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "DisableGameStart", "");
                        }
                        break;
                    }
                }
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                int playerIndex = clientDeviceIndexes.get(bluetoothDevice);
                clientDeviceIndexes.remove(bluetoothDevice);
                clientDevices.remove(playerIndex);
                NumberOfPlayers--;
                if (clientJoiningInProgress.get(bluetoothDevice))
                {
                    if (--numberOfPlayersJoiningInProgress == 0 && NumberOfPlayers != 1)
                    {
                        UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "ReadyToStart", "");
                    }
                }

                if (NumberOfPlayers == 1)
                {
                    UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "DisableGameStart", "");
                }

                bluetoothGattServer.getService(GAME_SERVICE).getCharacteristic(PLAYER_NAMES.get(playerIndex-1)).setValue("A");

                for (Integer key : clientDevices.keySet())
                {
                    bluetoothGattServer.notifyCharacteristicChanged(clientDevices.get(key), bluetoothGattServer.getService(GAME_SERVICE).getCharacteristic(PLAYER_NAMES.get(playerIndex-1)), true);
                }

                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "RemovePlayer", Integer.toString(playerIndex));

                if (clientsFinished.containsKey(bluetoothDevice))
                {
                    clientsFinished.remove(bluetoothDevice);
                    numberOfPlayersFinished--;
                }

                if (NumberOfPlayers == numberOfPlayersFinished)
                {
                    EndGame();
                }
                else if (!isGameEnding && numberOfPlayersFinished >= NumberOfPlayers/2.0f)
                {
                    isGameEnding = true;
                    UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "StartEndGameCountdown", "");
                }
            }
        }



        @Override
        public void onServiceAdded (int status, BluetoothGattService service) {
            if (status == BluetoothGatt.GATT_SUCCESS && service.getUuid().equals(GAME_SERVICE)) {
                bluetoothGattServer.addService(gameDataServiceConstructor());
            }
            else if (status == BluetoothGatt.GATT_SUCCESS && service.getUuid().equals(GAME_DATA_SERVICE))
            {
                bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
                clientDevices = new HashMap<>();
                clientDeviceIndexes = new HashMap<>();
                clientJoiningInProgress = new HashMap<>();
                bluetoothGattServer.getService(GAME_SERVICE).getCharacteristic(PLAYER_1_NAME).setValue(myName);
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "NewPlayer", CreateJsonPlayerNameString(1, myName, true));
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "SetNextLevel", String.valueOf(1));
                NumberOfPlayers = 1;
                bluetoothLeAdvertiser.startAdvertising(advertiseSettings, createAdvertisingPacket(scanCode), advertiseCallback);
            }
            else if (status != BluetoothGatt.GATT_SUCCESS)
            {
                if (++addServiceFails == 3)
                {
                    UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "NetworkErrorLog", "2");
                }
                else
                {
                    bluetoothGattServer.addService(service);
                }
            }
        }



        @Override
        public void onCharacteristicReadRequest (BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic)
        {
            //Log.d("Received", "" + clientDeviceIndexes.get(device));
            if (characteristic.getUuid().equals(NUMBER_OF_PLAYERS))
            {
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, intToByteArray(clientDeviceIndexes.get(device)));
            }
            else if (characteristic.getUuid().equals(NEXT_MAP))
            {
                clientJoiningInProgress.put(device, false);
                numberOfPlayersJoiningInProgress--;
                if (numberOfPlayersJoiningInProgress == 0)
                {
                    UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "ReadyToStart", "");
                }
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, characteristic.getValue());
            }
            else
            {
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, characteristic.getValue());
            }
        }



        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            characteristic.setValue(value);
            if (PLAYER_NAMES.contains(characteristic.getUuid()))
            {
                //Log.d("Received", "" + clientDeviceIndexes.get(device));
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "NewPlayer", CreateJsonPlayerNameString(UUID_TO_PLAYER_INDEX.get(characteristic.getUuid()), new String(value), false));
                for (Integer key : clientDevices.keySet())
                {
                    if (!clientDevices.get(key).equals(device))
                    {
                        bluetoothGattServer.notifyCharacteristicChanged(clientDevices.get(key), characteristic, true);
                    }
                }
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, stringToByteArray(myName));
            }
            else if (PLAYER_DATA.contains(characteristic.getUuid()))
            {
                //Log.d("Game Packet Received", "" + clientDeviceIndexes.get(device));
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "UpdatePlayerPosition", new String(value));
                receivedPackets[UUID_TO_PLAYER_INDEX.get(characteristic.getUuid())-1]++;
                for (Integer key : clientDevices.keySet())
                {
                    if (!clientDevices.get(key).equals(device))
                    {
                        bluetoothGattServer.notifyCharacteristicChanged(clientDevices.get(key), characteristic, false);
                    }
                }
            }
            else if (PLAYER_FINISH_TIMES.contains(characteristic.getUuid()))
            {
                //Log.d("Received", "" + clientDeviceIndexes.get(device));
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
                NewPlayerFinished(new String(value));
                clientsFinished.put(device, true);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value)
        {
            descriptor.setValue(value);
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
        }

    };


    private void NewPlayerFinished(String json)
    {
        UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "PlayerFinishTime", json);

        if (++numberOfPlayersFinished == NumberOfPlayers)
        {
            EndGame();
        }
        else if (!isGameEnding && numberOfPlayersFinished >= NumberOfPlayers/2.0)
        {
            isGameEnding = true;
            UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "StartEndGameCountdown", "");
        }
    }

    public void StartGame()
    {
        numberOfPlayersFinished = 0;
        isGameEnding = false;
        clientsFinished = new HashMap<>();
        for (int i = 0; i < 4; i++)
        {
            BluetoothGattCharacteristic finishTimeCharacteristic = bluetoothGattServer.getService(GAME_DATA_SERVICE).getCharacteristic(PLAYER_FINISH_TIMES.get(i));
            finishTimeCharacteristic.setValue(CreateFinishTimeString(i+1, 0.0f));
        }
        BluetoothGattCharacteristic startGameCharacteristic = bluetoothGattServer.getService(GAME_SERVICE).getCharacteristic(START_GAME);
        startGameCharacteristic.setValue(new byte[]{(byte)1});
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
        for (Integer key : clientDevices.keySet())
        {
            bluetoothGattServer.notifyCharacteristicChanged(clientDevices.get(key), startGameCharacteristic, true);
        }
        UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "StartGame", "");
    }



    public void EndGame()
    {
        BluetoothGattCharacteristic startGameCharacteristic = bluetoothGattServer.getService(GAME_SERVICE).getCharacteristic(START_GAME);
        startGameCharacteristic.setValue(new byte[]{(byte)0});
        UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "EndGame", "");
        for (Integer key : clientDevices.keySet())
        {
            bluetoothGattServer.notifyCharacteristicChanged(clientDevices.get(key), startGameCharacteristic, true);
        }
    }

    public void RestartAdvertising()
    {
        bluetoothLeAdvertiser.startAdvertising(advertiseSettings, createAdvertisingPacket(scanCode), advertiseCallback);
    }

    public void SelectLevel(int levelIndex)
    {
        BluetoothGattCharacteristic levelCharacteristic = bluetoothGattServer.getService(GAME_SERVICE).getCharacteristic(NEXT_MAP);
        levelCharacteristic.setValue(String.valueOf(levelIndex));
        UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "SetNextLevel", String.valueOf(levelIndex));
        for (Integer key : clientDevices.keySet())
        {
            bluetoothGattServer.notifyCharacteristicChanged(clientDevices.get(key), levelCharacteristic, true);
        }
    }

    private BluetoothGattService gameServiceConstructor()
    {
        BluetoothGattService bluetoothGattService = new BluetoothGattService(GAME_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        bluetoothGattService.addCharacteristic(new BluetoothGattCharacteristic(NUMBER_OF_PLAYERS, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ));

        BluetoothGattCharacteristic startGameCharacteristic = new BluetoothGattCharacteristic(START_GAME, BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        startGameCharacteristic.setValue(new byte[]{(byte)0});
        bluetoothGattService.addCharacteristic(startGameCharacteristic);

        BluetoothGattCharacteristic nextMapCharacteristic = new BluetoothGattCharacteristic(NEXT_MAP, BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        nextMapCharacteristic.setValue("1");
        bluetoothGattService.addCharacteristic(nextMapCharacteristic);

        for (int i = 0; i < 4; i++)
        {
            BluetoothGattCharacteristic playerINameCharacteristic = new BluetoothGattCharacteristic(PLAYER_NAMES.get(i), BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
            playerINameCharacteristic.setValue("A");
            bluetoothGattService.addCharacteristic(playerINameCharacteristic);

        }

        return bluetoothGattService;
    }



    private BluetoothGattService gameDataServiceConstructor()
    {
        BluetoothGattService bluetoothGattService = new BluetoothGattService(GAME_DATA_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        for (int i = 0; i < 4; i++)
        {
            BluetoothGattCharacteristic playerIDataCharacteristic = new BluetoothGattCharacteristic(PLAYER_DATA.get(i), BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE);
            bluetoothGattService.addCharacteristic(playerIDataCharacteristic);

            BluetoothGattCharacteristic playerIFinishCharacteristic = new BluetoothGattCharacteristic(PLAYER_FINISH_TIMES.get(i), BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ , BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
            playerIFinishCharacteristic.setValue("A");
            bluetoothGattService.addCharacteristic(playerIFinishCharacteristic);
        }

        return bluetoothGattService;
    }



    //////////////////////
    // CLIENT FUNCTIONS //
    //////////////////////



    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private List<BluetoothGattService> bluetoothGattServices;

    private List<BluetoothGattCharacteristic> readQueue;
    private List<BluetoothGattCharacteristic> notificationQueue;

    private boolean scanning;
    private int myIndex;
    private boolean connecting;

    private int discoverServiceFails;

    private int gameServiceIndex;
    private int gameDataServiceIndex;

    private Instant instant;
    private Instant pingStart;





    public void ScanForCentral(String code, String name)
    {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        myName = name;
        scanCode = code;
        //Log.d(TAG, byteArraytoUUID(stringToByteArray(code)).toString());
        connecting = false;
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.startScan(null, scanSettings, scanCallback);
            scanning = true;
            sentPackets = new int[4];
            receivedPackets = new int[4];
        }
        else
        {
            UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "NetworkErrorLog", "3");
        }
    }



    ScanSettings scanSettings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(0L)
            .build();



    private final ScanCallback scanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            List<ParcelUuid> uuids = result.getScanRecord().getServiceUuids();
            if (uuids != null) {
                for (ParcelUuid uuid : uuids) {
                    if (byteArraytoUUID(stringToByteArray(scanCode)).equals(uuid.getUuid()) && !connecting)
                    {
                        //Log.d(TAG, uuid.toString());
                        //Log.d(TAG, byteArraytoUUID(stringToByteArray(scanCode)).toString());
                        device.connectGatt(activity, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE, BluetoothDevice.PHY_LE_2M);
                        connecting = true;
                    }
                }
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "NetworkErrorLog", "4");
        }
    };




    BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {



        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                if (newState == BluetoothProfile.STATE_CONNECTED)
                {
                    bluetoothGatt = gatt;
                    bluetoothLeScanner.stopScan(scanCallback);
                    gatt.discoverServices();
                }
                else if (newState != BluetoothProfile.STATE_CONNECTING)
                {
                    UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "LeaveGame", "");
                    gatt.close();
                    for (int i = 0; i < 4; i++)
                    {
                        if (i + 1 != myIndex)
                        {
                            Log.d(TAG, "Packets Sent to Player" + (i+1) + " : " + sentPackets[i]);
                            Log.d(TAG, "Packets Received from Player" + (i+1) + " : " + receivedPackets[i]);
                        }
                    }
                }
            }
            else
            {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "NetworkErrorLog", "5");
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "LeaveGame", "");
                gatt.close();
                for (int i = 0; i < 4; i++)
                {
                    if (i + 1 != myIndex)
                    {
                        Log.d(TAG, "Packets Sent to Player" + (i+1) + " : " + sentPackets[i]);
                        Log.d(TAG, "Packets Received from Player" + (i+1) + " : " + receivedPackets[i]);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            instant = Instant.now();
            //Log.d("Received", "1: " +  (instant.getNano() - pingStart.getNano()));
            if (characteristic.getUuid().equals(NUMBER_OF_PLAYERS))
            {
                myIndex = characteristic.getValue()[0];
                BluetoothGattCharacteristic myNameCharacteristic = bluetoothGattServices.get(gameServiceIndex).getCharacteristic(PLAYER_NAMES.get(myIndex-1));
                myNameCharacteristic.setValue(myName);
                gatt.writeCharacteristic(myNameCharacteristic);
                //pingStart = Instant.now();
                //Log.d("Sent", "1");
                notificationQueue = new ArrayList<>();
            }
            else if (PLAYER_NAMES.contains(characteristic.getUuid()))
            {
                if (!(new String(characteristic.getValue())).equals("A"))
                {
                    int userIndex = UUID_TO_PLAYER_INDEX.get(characteristic.getUuid());
                    UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "NewPlayer", CreateJsonPlayerNameString(userIndex, new String(characteristic.getValue()), false));
                    bluetoothGatt.setCharacteristicNotification(bluetoothGattServices.get(gameDataServiceIndex).getCharacteristic(PLAYER_DATA.get(userIndex-1)), true);
                    notificationQueue.add(bluetoothGattServices.get(gameDataServiceIndex).getCharacteristic(PLAYER_DATA.get(userIndex-1)));
                }

                gatt.setCharacteristicNotification(readQueue.get(0), true);
                notificationQueue.add(readQueue.remove(0));
                if (readQueue.size() != 0) {
                    gatt.readCharacteristic(readQueue.get(0));
                    //pingStart = Instant.now();
                    //Log.d("Sent", "1");
                }
            }
            else if (characteristic.getUuid().equals(NEXT_MAP))
            {
                // Set next map
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "SetNextLevel", new String(characteristic.getValue()));
                bluetoothGatt.setCharacteristicNotification(readQueue.get(0), true);
                notificationQueue.add(readQueue.remove(0));
                //StartSettingNotifications();
                subscribeToCharacteristic(bluetoothGattServices.get(gameServiceIndex).getCharacteristic(START_GAME), bluetoothGatt, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                bluetoothGatt.setCharacteristicNotification(bluetoothGattServices.get(gameServiceIndex).getCharacteristic(START_GAME), true);

            }
            else if (PLAYER_FINISH_TIMES.contains(characteristic.getUuid()))
            {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "PlayerFinishTime", new String(characteristic.getValue()));

                if (readQueue.size() != 0)
                {
                    bluetoothGatt.readCharacteristic(readQueue.remove(0));
                    //pingStart = Instant.now();
                    //Log.d("Sent", "1");
                }
                else
                {
                    UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "EndGame", "");
                }
            }
        }

        @Override
        public void onCharacteristicChanged (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            if (PLAYER_NAMES.contains(characteristic.getUuid())) {
                if ((new String(characteristic.getValue())).equals("A"))
                {
                    int userIndex = UUID_TO_PLAYER_INDEX.get(characteristic.getUuid());
                    bluetoothGatt.setCharacteristicNotification(bluetoothGattServices.get(gameDataServiceIndex).getCharacteristic(PLAYER_DATA.get(userIndex-1)), false);
                    subscribeToCharacteristic(bluetoothGattServices.get(gameDataServiceIndex).getCharacteristic(PLAYER_DATA.get(userIndex-1)), bluetoothGatt, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "RemovePlayer", Integer.toString(userIndex));
                }
                else
                {
                    int userIndex = UUID_TO_PLAYER_INDEX.get(characteristic.getUuid());
                    bluetoothGatt.setCharacteristicNotification(bluetoothGattServices.get(gameDataServiceIndex).getCharacteristic(PLAYER_DATA.get(userIndex-1)), true);
                    subscribeToCharacteristic(bluetoothGattServices.get(gameDataServiceIndex).getCharacteristic(PLAYER_DATA.get(userIndex-1)), bluetoothGatt, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "NewPlayer", CreateJsonPlayerNameString(userIndex, new String(characteristic.getValue()), false));
                }
            }
            else if (characteristic.getUuid().equals(START_GAME))
            {
                if (characteristic.getValue()[0] == 1)
                {
                    UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "StartGame", "");
                }
                else
                {
                    GetFinishTimes();
                    bluetoothGatt.readCharacteristic(readQueue.remove(0));
                    //pingStart = Instant.now();
                    //Log.d("Sent", "1");
                }
            }
            else if (PLAYER_DATA.contains(characteristic.getUuid())) {
                //Log.d("Received", "" + UUID_TO_PLAYER_INDEX.get(characteristic.getUuid()));
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "UpdatePlayerPosition", new String(characteristic.getValue()));
                receivedPackets[UUID_TO_PLAYER_INDEX.get(characteristic.getUuid())-1]++;
            }
            else if (characteristic.getUuid().equals(NEXT_MAP))
            {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "SetNextLevel", new String(characteristic.getValue()));
            }
        }

        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            instant = Instant.now();
            //Log.d("Received", "1: " +  (instant.getNano() - pingStart.getNano()));
            if (characteristic.getUuid().equals(PLAYER_NAMES.get(myIndex-1))) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "NewPlayer", CreateJsonPlayerNameString(myIndex, myName, true));
                GetPlayerNames();
                gatt.readCharacteristic(readQueue.get(0));
                //pingStart = Instant.now();
                //Log.d("Sent", "1");
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            if (notificationQueue.size() != 0 && PLAYER_DATA.contains(notificationQueue.get(0).getUuid()))
            {
                subscribeToCharacteristic(notificationQueue.remove(0), bluetoothGatt, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            }
            else if (notificationQueue.size() != 0)
            {
                subscribeToCharacteristic(notificationQueue.remove(0), bluetoothGatt, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            }
            else
            {
                gatt.requestMtu(185);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bluetoothGattServices = gatt.getServices();

                for (int i = 0; i < bluetoothGattServices.size(); i++) {
                    if (bluetoothGattServices.get(i).getUuid().equals(GAME_SERVICE))
                    {
                        gameServiceIndex = i;
                        gatt.readCharacteristic(bluetoothGattServices.get(gameServiceIndex).getCharacteristic(NUMBER_OF_PLAYERS));
                        //pingStart = Instant.now();
                        //Log.d("Sent", "1");
                    }
                    else if (bluetoothGattServices.get(i).getUuid().equals(GAME_DATA_SERVICE))
                    {
                        gameDataServiceIndex = i;
                    }
                }
            }
            else
            {
                if (++discoverServiceFails == 3)
                {
                    UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "NetworkErrorLog", "6");
                }
                else
                {
                    gatt.discoverServices();
                }
            }
        }

        @Override
        public void onServiceChanged (BluetoothGatt gatt)
        {
            gatt.disconnect();
        }
    }
    ;

    private void subscribeToCharacteristic(BluetoothGattCharacteristic characteristic, BluetoothGatt gatt, byte[] value)
    {
        BluetoothGattDescriptor cccDescriptor =  characteristic.getDescriptor(CCC_DESCRIPTOR_UUID);
        if (cccDescriptor != null)
        {
            cccDescriptor.setValue(value);
            gatt.writeDescriptor(cccDescriptor);
        }
        else if (notificationQueue.size() != 0)
        {
            gatt.requestMtu(185);
            notificationQueue.clear();
        }

    }

    private void StartSettingNotifications()
    {
        for (int i = 1; i <= 4; i++) {
            if (i != myIndex) {
                notificationQueue.add(bluetoothGattServices.get(gameServiceIndex).getCharacteristic(PLAYER_DATA.get(i-1)));
                notificationQueue.add(bluetoothGattServices.get(gameServiceIndex).getCharacteristic(PLAYER_NAMES.get(i-1)));
            }
        }
    }

    public void GetPlayerNames() {
        readQueue = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            if (i != myIndex) {
                readQueue.add(bluetoothGattServices.get(gameServiceIndex).getCharacteristic(PLAYER_NAMES.get(i-1)));
            }
        }
        readQueue.add(bluetoothGattServices.get(gameServiceIndex).getCharacteristic(NEXT_MAP));
    }

    public void GetFinishTimes() {
        readQueue = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            if (i != myIndex) {
                readQueue.add(bluetoothGattServices.get(gameDataServiceIndex).getCharacteristic(PLAYER_FINISH_TIMES.get(i-1)));
            }
        }
    }
}
