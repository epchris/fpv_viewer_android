package com.fpvout.digiview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

/**
 * Listens for broadcasts related to USB events, like USB Permissions have been granted and notifies
 * the listener with the approved device.  Also listens for "usb detached" events and notifies the
 * listener of those.
 */
public class UsbDeviceBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_USB_PERMISSION = "com.example.ijdfpvviewer.USB_PERMISSION";
    private final UsbDeviceListener listener;

    public UsbDeviceBroadcastReceiver(UsbDeviceListener listener ){
        this.listener = listener;
    }

    /**
     * Processes intents from the Android system, reacts to specific ones related to USB that we're
     * interested in
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)) {
            handleUsbPermission(intent);
        }
        else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            listener.usbDeviceDetached();
        }
        else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            listener.usbDeviceAttached();
        }
    }

    /**
     * When the user responds to the permission request this code is notified to handle their
     * response.  If they've granted permission we should be able to fetch the device that they
     * approved, which we then notify the listener with.
     * @param intent
     */
    private void handleUsbPermission(Intent intent) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            if(device != null){
                Log.d("UsbDeviceBroadcastReceiver", "Usb device approved");
                listener.usbDeviceApproved(device);
            }
        }
    }
}
