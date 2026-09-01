package com.monjaro.carplaynightv2;
import android.content.*;

public class UsbPermissionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (UsbNightController.ACTION_USB_PERMISSION.equals(intent.getAction())) {
            UsbNightController.onUsbPermissionResult(context, intent);
        }
    }
}
