package com.jswb.iotble;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.model.BleGattCharacter;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.model.BleGattService;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

import java.util.List;
import java.util.UUID;

public class MyBLEService extends Service {
    public static final int CMD_SCAN_START = 1;
    public static final int CMD_SCAN_STOP = 2;
    public static final int CMD_CONNECT_DEVICE = 3;
    private BluetoothClient mClient;
    private boolean isBLEOpen;
    private final BluetoothStateListener mBluetoothStateListener = new BluetoothStateListener() {
        @Override
        public void onBluetoothStateChanged(boolean openOrClosed) {
            isBLEOpen = openOrClosed;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mClient = new BluetoothClient(this);
        mClient.registerBluetoothStateListener(mBluetoothStateListener);
        if (!mClient.isBluetoothOpened()) {
            mClient.openBluetooth();
        } else {
            isBLEOpen = true;
        }
    }

    private SearchResult connectDevice;
    private UUID srvUUID, characterUUID;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int cmd = intent.getIntExtra("CMD", 0);
        switch (cmd) {
            case CMD_SCAN_START: startScanBLE();break;
            case CMD_SCAN_STOP: stopScanBLEDevice();break;
            case CMD_CONNECT_DEVICE: connectBLEDevice();break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startScanBLE() {
        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(3000, 3)
                .searchBluetoothClassicDevice(5000)
                .searchBluetoothLeDevice(2000).build();

        mClient.search(request, new SearchResponse() {
            @Override
            public void onSearchStarted() {
                Log.v("Leo", "Scan : Stared");
            }

            @Override
            public void onDeviceFounded(SearchResult device) {
                Log.v("Leo", device.getName() + "&" + device.getAddress());
                if (device.getName().equals("ORANGE")) {
                    connectDevice = device;

                    Intent intent = new Intent("MyBLEService");
                    intent.putExtra("mesg", "found device" + device.getName());
                    sendBroadcast(intent);

                    stopScanBLEDevice();
                }
            }

            @Override
            public void onSearchStopped() {
                Log.v("Leo", "scan : stoped");
            }

            @Override
            public void onSearchCanceled() {
                Log.v("Leo", "scan : canceled");
            }
        });
    }

    private void connectBLEDevice() {
        if (connectDevice == null) return;
        stopScanBLEDevice();

        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(3)
                .setConnectTimeout(30000)
                .setServiceDiscoverRetry(3)
                .setServiceDiscoverTimeout(20000).build();
        Log.v("Leo", "connecting...");

        mClient.connect(connectDevice.getAddress(), options, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile data) {
                Log.v("Leo", "code= " + code + ":" + Constants.REQUEST_SUCCESS);
                if(code == Constants.REQUEST_SUCCESS){
                    List<BleGattService> bleGattServices = data.getServices();
                    for(BleGattService bleGattService : bleGattServices){
                        UUID serviceUUID = bleGattService.getUUID();
                        Log.v("Leo", "Service UUID="+ serviceUUID.toString());
                        if(serviceUUID.toString().equals("")){
                            srvUUID = serviceUUID;
                        }

                        List<BleGattCharacter> bleGattCharacters = bleGattService.getCharacters();
                        for(BleGattCharacter bleGattCharacter : bleGattCharacters){
                            bleGattCharacter.getProperty();
                            Log.v("Leo", bleGattCharacter.getUuid().toString());

                            if(bleGattCharacter.getUuid().toString().equals("")){
                                characterUUID = bleGattCharacter.getUuid();
                            }
                        }
                    }

                    // Service:0000180f-0000-1000-8000-00805f9b34fb
                    // 00002a19-0000-1000-8000-00805f9b34fb
                    openNotify();

                    Intent intent = new Intent("MyBLEService");
                    intent.putExtra("mesg", "Connect success");
                    sendBroadcast(intent);
                }
            }
        });
    }

    private void openNotify(){
        mClient.notify(connectDevice.getAddress(), srvUUID, characterUUID, new BleNotifyResponse() {
            @Override
            public void onNotify(UUID service, UUID character, byte[] value) {
                Log.v("Leo", "receive");
                Intent intent = new Intent("MyBLEDevice");
                intent.putExtra("mseg", "電池目前電量:" + value[0] + "%");
                sendBroadcast(intent);
            }

            @Override
            public void onResponse(int code) {
                if(code == Constants.REQUEST_SUCCESS){

                }
            }
        });
    }

    private void stopScanBLEDevice() {
        mClient.stopSearch();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mClient.closeBluetooth();
        mClient.unregisterBluetoothStateListener(mBluetoothStateListener);
    }
}
