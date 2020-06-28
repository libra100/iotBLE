package com.jswb.iotble;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int cmd = intent.getIntExtra("CMD", 0);
        switch (cmd) {
            case CMD_SCAN_START:
                startScanBLE();
                break;
            case CMD_SCAN_STOP:
                stopScanBLE();
                break;
            case CMD_CONNECT_DEVICE:
                break;
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
                if (device.getName().equals("A5")) {
                    connectDevice = device;

                    Intent intent = new Intent("MyBLEService");
                    intent.putExtra(" mesg", "found device");
                    sendBroadcast(intent);

                    stopScanBLE();
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

    private void stopScanBLE() {
        mClient.stopSearch();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mClient.closeBluetooth();
        mClient.unregisterBluetoothStateListener(mBluetoothStateListener);
    }
}
