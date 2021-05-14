package com.fpvout.digiview;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements UsbDeviceListener {
    private static final int VENDOR_ID = 11427;
    private static final int PRODUCT_ID = 31;
    UsbDeviceBroadcastReceiver usbDeviceBroadcastReceiver;
    UsbManager usbManager;
    UsbDevice usbDevice;
    UsbMaskConnection mUsbMaskConnection;
    VideoReaderExoplayer mVideoReader;
    boolean usbConnected = false;
    SurfaceView fpvView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide top bar and status bar
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        // Prevent screen from sleeping
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        fpvView = findViewById(R.id.fpvView);

        mUsbMaskConnection = new UsbMaskConnection();
        mVideoReader = new VideoReaderExoplayer(fpvView, this);
        usbDeviceBroadcastReceiver = initUsbBroadcastReceiver(this);
        initConnection();
    }

    private void initConnection() {
        // Return early if USB is already connected
        if ( usbConnected ) {
            return;
        }

        // If the device is already present, then connect right away
        if (searchDevice()) {
            connect();
        }
        else {
            // Notify the user that we're trying to set up the USB connection, the
            // usbDeviceBroadcastReceiver will handle attach events
            Toast.makeText(
                    getApplicationContext(),
                    "waiting for usb connection...",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void usbDeviceApproved(UsbDevice device) {
        Log.d("USB", "usbDevice approved");
        usbDevice = device;
        Toast.makeText(getApplicationContext(), "usb attached", Toast.LENGTH_SHORT).show();
        connect();
    }

    @Override
    /**
     * Handles a USB device being attached
     */
    public void usbDeviceAttached() {
        if (searchDevice()) {
            connect();
        }
    }

    @Override
    public void usbDeviceDetached() {
        Log.d("USB", "usbDevice detached");
        Toast.makeText(getApplicationContext(), "usb detached", Toast.LENGTH_SHORT).show();
        this.onStop();
    }

    /**
     * Searches for the proper USB device attached to the system and returns true if it is found.
     * Will prompt the user for required permissions if found and permission has not previously been
     * granted.
     * Also sets `usbDevice` to the discovered device if found.
     * @return boolean
     */
    private boolean searchDevice() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList.size() <= 0) {
            usbDevice = null;
            return false;
        }

        for(UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == VENDOR_ID && device.getProductId() == PRODUCT_ID) {
                if (usbManager.hasPermission(device)) {
                    usbDevice = device;
                    return true;
                }

                // We don't have permission granted already, so request it.  When the user responds,
                // the UsbDeviceBroadcastReceiver will handle it
                PendingIntent permissionIntent = PendingIntent.getBroadcast(
                        this,
                        0,
                        new Intent(UsbDeviceBroadcastReceiver.ACTION_USB_PERMISSION),
                        0);
                usbManager.requestPermission(device, permissionIntent);
            }
        }

        return false;
    }

    private void connect(){
        usbConnected = true;
        mUsbMaskConnection.setUsbDevice(usbManager.openDevice(usbDevice), usbDevice);
        mUsbMaskConnection.start();
        mVideoReader.start(mUsbMaskConnection.mInputStream);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (usbConnected) {

        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (searchDevice() && !usbConnected) {
            Log.d("RESUME_USB_CONNECTED", "not connected");
            connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        mUsbMaskConnection.stop();
        mVideoReader.stop();
        usbConnected = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mUsbMaskConnection.stop();
        mVideoReader.stop();
        usbConnected = false;
    }

    /**
     * Initializes the broadcast receiver, hooks up handling of broadcasts the system is interested
     * in to the provided UsbDeviceListener
     */
    private UsbDeviceBroadcastReceiver initUsbBroadcastReceiver(UsbDeviceListener listener) {
        UsbDeviceBroadcastReceiver receiver = new UsbDeviceBroadcastReceiver(listener);

        IntentFilter filter = new IntentFilter(UsbDeviceBroadcastReceiver.ACTION_USB_PERMISSION);
        registerReceiver(usbDeviceBroadcastReceiver, filter);
        IntentFilter filterDetached = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbDeviceBroadcastReceiver, filterDetached);
        IntentFilter filterAttached = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(usbDeviceBroadcastReceiver, filterAttached);

        return receiver;
    }
}