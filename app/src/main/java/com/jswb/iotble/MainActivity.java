package com.jswb.iotble;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView mesg;
    private MyReceiver myReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        init();
    }

    private void init() {
        mesg = findViewById(R.id.mesg);

        myReceiver = new MyReceiver();
        registerReceiver(myReceiver, new IntentFilter("MyBLEService"));

        Intent intent = new Intent(this, MyBLEService.class);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(this, MyBLEService.class);
        stopService(intent);

        super.onDestroy();
    }

    public void ScanBLE(View view) {
        Intent intent = new Intent(this, MyBLEService.class);
        intent.putExtra("CMD", MyBLEService.CMD_SCAN_START);
        startService(intent);
    }

    public void stopScanBLE(View view) {
        Intent intent = new Intent(this, MyBLEService.class);
        intent.putExtra("CMD", MyBLEService.CMD_SCAN_STOP);
        startService(intent);
    }

    public void ConnectBLE(View view) {
        Intent intent = new Intent(this, MyBLEService.class);
        intent.putExtra("CMD", MyBLEService.CMD_CONNECT_DEVICE);
        startService(intent);
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String strMesg = intent.getStringExtra("mesg");
            mesg.setText(strMesg);
        }
    }
}