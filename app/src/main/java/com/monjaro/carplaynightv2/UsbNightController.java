package com.monjaro.carplaynightv2;

import android.app.PendingIntent;
import android.content.*;
import android.hardware.usb.*;
import android.os.Build;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

public final class UsbNightController {
    public static final String ACTION_USB_PERMISSION =
            "com.monjaro.carplaynightv2.USB_PERMISSION";

    // Known CPC200-CCPA / Auto Box family identifiers seen in public reverse engineering.
    // We also accept any device with vendor-specific interface + bulk OUT endpoint.
    private static final int KNOWN_VID = 0x1314;
    private static final int KNOWN_PID = 0x1521;

    private static volatile Boolean pendingNight = null;
    private static volatile String lastStatus = "Ещё не запускалось";

    public static String getLastStatus() { return lastStatus; }

    public static void requestPermission(Context c) {
        UsbManager um = (UsbManager)c.getSystemService(Context.USB_SERVICE);
        UsbDevice d = findDevice(um);
        if (d == null) {
            lastStatus = "Carlinkit/CPC200 не найден по USB";
            broadcastStatus(c);
            return;
        }
        if (um.hasPermission(d)) {
            lastStatus = "Доступ USB уже выдан";
            broadcastStatus(c);
            return;
        }
        Intent i = new Intent(ACTION_USB_PERMISSION).setPackage(c.getPackageName());
        PendingIntent pi = PendingIntent.getBroadcast(
                c, 101, i,
                Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
        um.requestPermission(d, pi);
        lastStatus = "Запрошено разрешение USB";
        broadcastStatus(c);
    }

    public static boolean setNightMode(Context c, boolean night) {
        pendingNight = night;
        UsbManager um = (UsbManager)c.getSystemService(Context.USB_SERVICE);
        UsbDevice d = findDevice(um);
        if (d == null) {
            lastStatus = "Ошибка: Carlinkit/CPC200 не найден";
            broadcastStatus(c);
            return false;
        }

        if (!um.hasPermission(d)) {
            requestPermission(c);
            lastStatus = "Нет USB-разрешения. Нажми «Запросить доступ к Carlinkit»";
            broadcastStatus(c);
            return false;
        }

        UsbInterface intf = findInterface(d);
        if (intf == null) {
            lastStatus = "Ошибка: не найден vendor-specific USB interface";
            broadcastStatus(c);
            return false;
        }

        UsbEndpoint out = findBulkOut(intf);
        if (out == null) {
            lastStatus = "Ошибка: не найден Bulk OUT endpoint";
            broadcastStatus(c);
            return false;
        }

        UsbDeviceConnection conn = null;
        boolean claimed = false;
        try {
            conn = um.openDevice(d);
            if (conn == null) {
                lastStatus = "Ошибка: UsbManager.openDevice() вернул null";
                broadcastStatus(c);
                return false;
            }

            claimed = conn.claimInterface(intf, false);
            if (!claimed) {
                // force=true may detach a kernel driver, but cannot steal a userspace
                // interface already owned by AutoKit. We try it only once.
                claimed = conn.claimInterface(intf, true);
            }

            if (!claimed) {
                lastStatus =
                        "USB занят AutoKit. Отдельное приложение не может отправить команду " +
                        "в активную сессию.";
                broadcastStatus(c);
                return false;
            }

            // CPC200 protocol:
            // Header: magic 0x55AA55AA, payloadLen=4, type=0x08,
            // typeCheck=~0x08, all LE.
            // Command 16 = StartNightMode, 17 = StopNightMode.
            int cmd = night ? 16 : 17;
            byte[] packet = commandPacket(cmd);

            int sent = conn.bulkTransfer(out, packet, packet.length, 1500);
            if (sent == packet.length) {
                lastStatus = "Отправлено: " +
                        (night ? "StartNightMode (16)" : "StopNightMode (17)");
                broadcastStatus(c);
                return true;
            } else {
                lastStatus = "Ошибка bulkTransfer: " + sent + "/" + packet.length;
                broadcastStatus(c);
                return false;
            }
        } catch (Throwable t) {
            lastStatus = "USB ошибка: " + t.getClass().getSimpleName() +
                    ": " + String.valueOf(t.getMessage());
            broadcastStatus(c);
            return false;
        } finally {
            try {
                if (conn != null && claimed) conn.releaseInterface(intf);
            } catch (Throwable ignored) {}
            try {
                if (conn != null) conn.close();
            } catch (Throwable ignored) {}
        }
    }

    public static void onUsbPermissionResult(Context c, Intent intent) {
        boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
        lastStatus = granted ? "USB-разрешение выдано" : "USB-разрешение отклонено";
        broadcastStatus(c);
        Boolean n = pendingNight;
        if (granted && n != null) setNightMode(c, n);
    }

    private static UsbDevice findDevice(UsbManager um) {
        HashMap<String, UsbDevice> list = um.getDeviceList();
        UsbDevice fallback = null;
        for (UsbDevice d : list.values()) {
            if (d.getVendorId() == KNOWN_VID && d.getProductId() == KNOWN_PID) return d;
            if (findInterface(d) != null) fallback = d;
        }
        return fallback;
    }

    private static UsbInterface findInterface(UsbDevice d) {
        for (int i=0;i<d.getInterfaceCount();i++) {
            UsbInterface f = d.getInterface(i);
            // class 0xFF = vendor-specific; endpoint layout used by CPC200.
            if (f.getInterfaceClass() == UsbConstants.USB_CLASS_VENDOR_SPEC &&
                    findBulkOut(f) != null) {
                return f;
            }
        }
        return null;
    }

    private static UsbEndpoint findBulkOut(UsbInterface intf) {
        for (int i=0;i<intf.getEndpointCount();i++) {
            UsbEndpoint ep = intf.getEndpoint(i);
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                return ep;
            }
        }
        return null;
    }

    private static byte[] commandPacket(int cmd) {
        ByteBuffer b = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(0x55AA55AA);
        b.putInt(4);
        b.putInt(0x08);
        b.putInt(0x08 ^ 0xFFFFFFFF);
        b.putInt(cmd);
        return b.array();
    }

    private static void broadcastStatus(Context c) {
        Intent i = new Intent("com.monjaro.carplaynightv2.STATUS");
        i.setPackage(c.getPackageName());
        c.sendBroadcast(i);
    }
}
