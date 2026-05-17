package com.yahya.hidmouse;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private BluetoothHidService hidService;
    private float lastX = 0, lastY = 0;
    private byte currentButtons = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void coreCreate(Bundle savedInstanceState) {
        super.coreCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hidService = new BluetoothHidService(this);

        View trackpad = findViewById(R.id.trackpad);
        View btnLeft = findViewById(R.id.btn_left_click);
        View btnRight = findViewById(R.id.btn_right_click);

        // Trackpad parmak hareketi yakalama (Delta X ve Y hesaplama)
        trackpad.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;

                    // Değerleri byte sınırlarına (-127, 127) kısıtlıyoruz
                    byte moveX = (byte) Math.max(-127, Math.min(127, dx));
                    byte moveY = (byte) Math.max(-127, Math.min(127, dy));

                    hidService.sendInput(currentButtons, moveX, moveY);

                    lastX = event.getX();
                    lastY = event.getY();
                    break;
            }
            return true;
        });

        // Sol Tık Butonu
        btnLeft.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                currentButtons |= 0x01; // Sol tık bitini 1 yap
                hidService.sendInput(currentButtons, (byte) 0, (byte) 0);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                currentButtons &= ~0x01; // Sol tık bitini kaldır
                hidService.sendInput(currentButtons, (byte) 0, (byte) 0);
            }
            return false;
        });

        // Sağ Tık Butonu
        btnRight.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                currentButtons |= 0x02; // Sağ tık bitini 1 yap
                hidService.sendInput(currentButtons, (byte) 0, (byte) 0);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                currentButtons &= ~0x02; // Sağ tık bitini kaldır
                hidService.sendInput(currentButtons, (byte) 0, (byte) 0);
            }
            return false;
        });
    }
}

