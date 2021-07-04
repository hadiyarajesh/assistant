package com.hadiarajesh.assistant;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

/**
 * Created by Rajesh on 25-01-2018.
 */

public class Toggle_Service {
    String return_result;
    String toggleService(Context context, String s) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        String lowerS = s.toLowerCase();

        if (lowerS.contains("wi-fi")) {
            if (lowerS.contains("on")) {
                wifi.setWifiEnabled(true);
                return_result = "Wifi is Enabled";
            }
            else{
                wifi.setWifiEnabled(false);
                return_result = "Wifi is Disabled";
            }
        }
        else if (lowerS.contains("bluetooth")) {
            if (lowerS.contains("on")) {
                bluetoothAdapter.enable();
                return_result = "Bluetooth is Enabled";
            }
            else{
                bluetoothAdapter.disable();
                return_result = "Bluetooth is Disabled";
            }
        }
        else if (lowerS.contains("data")) {
            return_result = "Toggling mobile data is not allowed";
        }
        else
            return_result = "Sorry, I didn't understand that";

        return return_result;
    }
}