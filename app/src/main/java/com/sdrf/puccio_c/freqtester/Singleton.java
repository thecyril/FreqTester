package com.sdrf.puccio_c.freqtester;

import com.hoho.android.usbserial.driver.UsbSerialPort;

public class Singleton {
    private static Singleton Instance = null;

    private static UsbSerialPort port = null;

    private Singleton() { }

    public static Singleton getInstance() {
        if (Instance == null)
            Instance = new Singleton();
        return Instance;
    }

    public UsbSerialPort getPort() {
        return port;
    }

    public void setPort(UsbSerialPort port1) {
        port = port1;
    }

}
