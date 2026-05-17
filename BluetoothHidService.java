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
    private static final byte[] MOUSE_REPORT_DESCRIPTOR = {
        (byte) 0x05, (byte) 0x01, (byte) 0x09, (byte) 0x02, (byte) 0xa1, (byte) 0x01,
        (byte) 0x09, (byte) 0x01, (byte) 0xa1, (byte) 0x00, (byte) 0x05, (byte) 0x09,
        (byte) 0x19, (byte) 0x01, (byte) 0x29, (byte) 0x03, (byte) 0x15, (byte) 0x00,
        (byte) 0x25, (byte) 0x01, (byte) 0x95, (byte) 0x03, (byte) 0x75, (byte) 0x01,
        (byte) 0x81, (byte) 0x02, (byte) 0x95, (byte) 0x01, (byte) 0x75, (byte) 0x05,
        (byte) 0x81, (byte) 0x03, (byte) 0x05, (byte) 0x01, (byte) 0x09, (byte) 0x30,
        (byte) 0x09, (byte) 0x31, (byte) 0x15, (byte) 0x81, (byte) 0x25, (byte) 0x7f,
        (byte) 0x75, (byte) 0x08, (byte) 0x95, (byte) 0x02, (byte) 0x81, (byte) 0x06,
        (byte) 0xc0, (byte) 0xc0
    };

    public BluetoothHidService(Context context) {
        this.context = context;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
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

        BluetoothHidDeviceAppSdpSettings sdpSettings = new BluetoothHidDeviceAppSdpSettings(
                "YahyaHidMouse", "Yahya HID Controller", "Yahya",
                BluetoothHidDevice.SUBCLASS1_MOUSE, MOUSE_REPORT_DESCRIPTOR
        );

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

    public void sendInput(byte buttons, byte dx, byte dy) {
        if (bluetoothHidDevice != null && connectedDevice != null) {
            byte[] report = new byte[]{buttons, dx, dy, 0};
            bluetoothHidDevice.sendReport(connectedDevice, 0, report);
        }
    }
}

