package com.yahya.hidmouse;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private BluetoothHidService hidService;
    private BluetoothAdapter bluetoothAdapter;
    private float lastX = 0, lastY = 0;
    private byte currentButtons = 0;
    private static final int PERMISSION_REQUEST_CODE = 202;

    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    private ArrayList<String> deviceNames = new ArrayList<>();
    private ArrayAdapter<String> listAdapter;
    
    private ListView listView;
    private TextView txtStatus;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        Button btnScan = findViewById(R.id.btn_scan);
        listView = findViewById(R.id.device_list);
        txtStatus = findViewById(R.id.txt_status);
        View trackpad = findViewById(R.id.trackpad);
        View btnLeft = findViewById(R.id.btn_left_click);
        View btnRight = findViewById(R.id.btn_right_click);

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        listView.setAdapter(listAdapter);

        // İzin kontrol mekanizması (Android 11 Redmi uyumlu)
        checkPermissionsAndInit();

        // Cihazları Listele Butonu tetikleyicisi
        btnScan.setOnClickListener(v -> showPairedDevices());

        // Listeden cihaza tıklandığında bağlanma tetikleyicisi
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (hidService != null && position < deviceList.size()) {
                BluetoothDevice selectedDevice = deviceList.get(position);
                @SuppressLint("MissingPermission") String name = selectedDevice.getName();
                txtStatus.setText("Durum: Bağlanılıyor -> " + name);
                hidService.connectToDevice(selectedDevice);
                Toast.makeText(MainActivity.this, name + " Cihazına HID İsteği Gönderildi!", Toast.LENGTH_SHORT).show();
            }
        });

        // Trackpad hareket kodları
        if (trackpad != null) {
            trackpad.setOnTouchListener((v, event) -> {
                if (hidService == null) return false;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = event.getX();
                        lastY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getX() - lastX;
                        float dy = event.getY() - lastY;

                        byte moveX = (byte) Math.max(-127, Math.min(127, dx));
                        byte moveY = (byte) Math.max(-127, Math.min(127, dy));

                        hidService.sendInput(currentButtons, moveX, moveY);

                        lastX = event.getX();
                        lastY = event.getY();
                        break;
                }
                return true;
            });
        }

        // Tıklama buton kodları
        if (btnLeft != null) {
            btnLeft.setOnTouchListener((v, event) -> {
                if (hidService == null) return false;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    currentButtons |= 0x01;
                    hidService.sendInput(currentButtons, (byte) 0, (byte) 0);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    currentButtons &= ~0x01;
                    hidService.sendInput(currentButtons, (byte) 0, (byte) 0);
                }
                return false;
            });
        }

        if (btnRight != null) {
            btnRight.setOnTouchListener((v, event) -> {
                if (hidService == null) return false;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    currentButtons |= 0x02;
                    hidService.sendInput(currentButtons, (byte) 0, (byte) 0);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    currentButtons &= ~0x02;
                    hidService.sendInput(currentButtons, (byte) 0, (byte) 0);
                }
                return false;
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void showPairedDevices() {
        if (bluetoothAdapter == null) return;
        
        deviceList.clear();
        deviceNames.clear();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceList.add(device);
                deviceNames.add(device.getName() + "\n" + device.getAddress());
            }
            listAdapter.notifyDataSetChanged();
            txtStatus.setText("Durum: Eşleşmiş Cihazlar Listelendi");
        } else {
            txtStatus.setText("Durum: Eşleşmiş Cihaz Bulunamadı!");
        }
    }

    private void checkPermissionsAndInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_ADVERTISE
                }, PERMISSION_REQUEST_CODE);
            } else {
                initHidService();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, PERMISSION_REQUEST_CODE);
            } else {
                initHidService();
            }
        }
    }

    private void initHidService() {
        try {
            hidService = new BluetoothHidService(this);
            txtStatus.setText("Durum: Bluetooth HID Hazır");
        } catch (Exception e) {
            txtStatus.setText("Durum: Servis Başlatma Hatası!");
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initHidService();
            } else {
                txtStatus.setText("Durum: İzin Reddedildi!");
            }
        }
    }
}
