package com.yahya.hidmouse;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private BluetoothHidService hidService;
    private float lastX = 0, lastY = 0;
    private byte currentButtons = 0;
    private static final int PERMISSION_REQUEST_CODE = 202;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Android 11 (API 30) ve altı cihazlar için uygun izin kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 ve üzeri için
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_ADVERTISE
                }, PERMISSION_REQUEST_CODE);
            } else {
                initHidService();
            }
        } else {
            // Senin cihazın (Android 11) için konum izni üzerinden Bluetooth yetkilendirmesi
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, PERMISSION_REQUEST_CODE);
            } else {
                initHidService();
            }
        }

        View trackpad = findViewById(R.id.trackpad);
        View btnLeft = findViewById(R.id.btn_left_click);
        View btnRight = findViewById(R.id.btn_right_click);

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

    private void initHidService() {
        try {
            hidService = new BluetoothHidService(this);
        } catch (Exception e) {
            Toast.makeText(this, "HID Servisi Başlatılamadı!", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Bluetooth/Konum izni reddedildi. Uygulama çalışamaz.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
