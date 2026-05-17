package com.yahya.hidmouse;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

@SuppressLint("MissingPermission")
public class BluetoothHidService {

    private final Context context;
    private BluetoothHidDevice bluetoothHidDevice;
    private BluetoothDevice connectedDevice;
    private boolean isRegistered = false;

    // Standart USB/Bluetooth Mouse Tanımlayıcısı (Report Descriptor)
    // Bilgisayara 3 butonlu ve X/Y eksenli bir mouse olduğumuzu bildirir.
    private static final byte[] MOUSE_REPORT_DESCRIPTOR = {
        (byte) 0x05, (byte) 0x01, // USAGE_PAGE (Generic Desktop)
        (byte) 0x09, (byte) 0x02, // USAGE (Mouse)
        (byte) 0xa1, (byte) 0x01, // COLLECTION (Application)
        (byte) 0x09, (byte) 0x01, //   USAGE (Pointer)
        (byte) 0xa1, (byte) 0x00, //   COLLECTION (Physical)
        (byte) 0x05, (byte) 0x09, //     USAGE_PAGE (Button)
        (byte) 0x19, (byte) 0x01, //     USAGE_MINIMUM (Button 1)
        (byte) 0x29, (byte) 0x03, //     USAGE_MAXIMUM (Button 3)
        (byte) 0x15, (byte) 0x00, //     LOGICAL_MINIMUM (0)
        (byte) 0x25, (byte) 0x01, //     LOGICAL_MAXIMUM (1)
        (byte) 0x95, (byte) 0x03, //     REPORT_COUNT (3)
        (byte) 0x75, (byte) 0x01, //     REPORT_SIZE (1)
        (byte) 0x81, (byte) 0x02, //     INPUT (Data,Var,Abs)
        (byte) 0x95, (byte) 0x01, //     REPORT_COUNT (1)
        (byte) 0x75, (byte) 0x05, //     REPORT_SIZE (5)
        (byte) 0x81, (byte) 0x03, //     INPUT (Cnst,Var,Abs)
        (byte) 0x05, (byte) 0x01, //     USAGE_PAGE (Generic Desktop)
        (byte) 0x09, (byte) 0x30, //     USAGE (X)
        (byte) 0x09, (byte) 0x31, //     USAGE (Y)
        (byte) 0x15, (byte) 0x81, //     LOGICAL_MINIMUM (-127)
        (byte) 0x25, (byte) 0x7f, //     LOGICAL_MAXIMUM (127)
        (byte) 0x75, (byte) 0x08, //     REPORT_SIZE (8)
        (byte) 0x95, (byte) 0x02, //     REPORT_COUNT (2)
        (byte) 0x81, (byte) 0x06, //     INPUT (Data,Var,Rel)
        (byte) 0xc0,              //   END_COLLECTION
        (byte) 0xc0               // END_COLLECTION
    };

    public BluetoothHidService(Context context) {
        this.context = context;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            // Android'in HID Device profil proxy'sine bağlanıyoruz
            adapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE);
        }
    }

    private final BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bluetoothHidDevice = (BluetoothHidDevice) proxy;
                registerApp();
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bluetoothHidDevice = null;
                isRegistered = false;
            }
        }
    };

    private void registerApp() {
        if (bluetoothHidDevice == null || isRegistered) return;

        // Cihazın SDP (Service Discovery Protocol) ayarlarını yapıyoruz
        BluetoothHidDeviceAppSdpSettings sdpSettings = new BluetoothHidDeviceAppSdpSettings(
                "YahyaHidMouse", 
                "Yahya HID Controller", 
                "Yahya",
                BluetoothHidDevice.SUBCLASS1_MOUSE, 
                MOUSE_REPORT_DESCRIPTOR
        );

        // Uygulamayı sisteme bir HID çevre birimi olarak kaydediyoruz
        bluetoothHidDevice.registerApp(sdpSettings, null, null, context.getMainExecutor(), hidCallback);
    }

    private final BluetoothHidDevice.Callback hidCallback = new BluetoothHidDevice.Callback() {
        @Override
        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
            isRegistered = registered;
        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            if (state == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device;
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                connectedDevice = null;
            }
        }
    };

    /**
     * Ekranda oluşan hareket ve tıklama verilerini byte paketine dönüştürüp gönderir.
     * @param buttons Buton durumları (Bit 0: Sol Tık, Bit 1: Sağ Tık)
     * @param dx X eksenindeki değişim miktarı (-127 ile 127 arası)
     * @param dy Y eksenindeki değişim miktarı (-127 ile 127 arası)
     */
    public void sendInput(byte buttons, byte dx, byte dy) {
        if (bluetoothHidDevice != null && connectedDevice != null) {
            // Veri paketi: [Butonlar, Delta X, Delta Y, Wheel/Scroll (0)]
            byte[] report = new byte[]{buttons, dx, dy, 0};
            bluetoothHidDevice.sendReport(connectedDevice, 0, report);
        }
    }
}

