package com.sdrf.puccio_c.freqtester;


import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.icu.math.BigDecimal;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by puccio_c on 4/10/18.
 */

public class SDRFUtils {

    private static final String TAG = SDRFUtils.class.getSimpleName();;

    static public LinkedHashMap<BigInteger, BigDecimal> csvRead(InputStreamReader file) {
        LinkedHashMap<BigInteger, BigDecimal> table;

        try {
            CSVReader reader = new CSVReader(file);
            table = new LinkedHashMap<>();// create prices map

            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                System.out.println(nextLine[0] + ", " + nextLine[1]);
                table.put(new BigInteger(nextLine[0]), new BigDecimal(nextLine[1]));

            }
            return table;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    static public UsbSerialPort usbConnect(UsbSerialPort port, TextView mTitleTextView, Context context) {
        if (port == null)  {
            mTitleTextView.setText("No serial device.");
        } else {
            final UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

//            usbManager.requestPermission(port.getDriver().getDevice(), mPermissionIntent);
            UsbDeviceConnection connection = usbManager.openDevice(port.getDriver().getDevice());
            if (connection == null) {
                mTitleTextView.setText("Opening device1 failed");
                return null;
            }
            try {
                port.open(connection);
                port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);


            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                mTitleTextView.setText("Error opening device: " + e.getMessage());
                try {
                    port.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                port = null;
                return port;
            }
            mTitleTextView.setText("Serial device: " + port.getDriver().getDevice().getProductName());
        }
        return port;
    }
    
    static public void sendCommand(BigInteger val, int nb, int bits, UsbSerialPort port){

        byte[] msg = SDRFUtils.intToByteArray(val, nb, bits);

        Log.d("SendCMD", val.toString());

        for (byte b : msg) {
            System.out.println(Integer.toBinaryString(b & 255 | 256).substring(1));
        }
        for (int index = 0; index < msg.length; index++) {
            Log.i("Byte", String.format("0x%20x", msg[index]));
        }
        try {
            port.write(msg, 10);
        } catch (IOException e) {
            Log.e("SendCMD", "Write Error");
            return;
        }
    }

    static public BigDecimal calc_moy(BigDecimal val1, BigDecimal val2) {
        BigDecimal res;
        BigDecimal ten = BigDecimal.valueOf(10D);
        res = (BigDecimal.valueOf(StrictMath.pow(ten.doubleValue() , val1.divide(ten, 3, BigDecimal.ROUND_HALF_EVEN).doubleValue()))
                .add(BigDecimal.valueOf(StrictMath.pow(ten.doubleValue(), val2.divide(ten, 3, BigDecimal.ROUND_HALF_EVEN).doubleValue()))));
        res = res.divide(BigDecimal.valueOf(2D), 3, BigDecimal.ROUND_HALF_EVEN);
        return res;
    }

    static public BigDecimal calc_val(BigDecimal val1, BigDecimal val2){
        BigDecimal ten = BigDecimal.valueOf(10D);
        BigDecimal res = BigDecimal.valueOf(StrictMath.pow(ten.doubleValue() , val1.divide(ten, 3, BigDecimal.ROUND_HALF_EVEN).doubleValue()))
                        .subtract(BigDecimal.valueOf(StrictMath.pow(ten.doubleValue(), val2.divide(ten, 3, BigDecimal.ROUND_HALF_EVEN).doubleValue())));
        return res;
    }

    static public BigInteger calc_tens(BigDecimal amp, BigDecimal valmin, BigDecimal valmax, BigInteger tensmin, BigInteger tensmax){
        BigInteger res;
        BigDecimal valcalc = calc_val(amp, valmin).divide(calc_val(valmax, valmin));
        valcalc = valcalc.multiply(new BigDecimal (tensmax.subtract(tensmin)));
        valcalc = valcalc.add(new BigDecimal (tensmin));
        res = valcalc.setScale(1, BigDecimal.ROUND_HALF_EVEN).toBigInteger();
        return res;
    }

    static public BigDecimal calc_att(BigDecimal freq, BigDecimal attmin, BigDecimal attmax, BigDecimal freqmin, BigDecimal freqmax){
        BigDecimal valcalc;
        BigDecimal ten = BigDecimal.valueOf(10D);
        valcalc = (freq.subtract(freqmin)).divide((freqmax.subtract(freqmin)), 3, BigDecimal.ROUND_HALF_EVEN);
        valcalc = valcalc.multiply(calc_val(attmax, attmin));
        valcalc = valcalc.add(BigDecimal.valueOf(StrictMath.pow(ten.doubleValue() , attmin.divide(ten, 3, BigDecimal.ROUND_HALF_EVEN).doubleValue())));
        valcalc = ten.multiply(BigDecimal.valueOf(StrictMath.log10(valcalc.doubleValue())));
        return valcalc;
    }

    static public BigInteger ParseTension(LinkedHashMap table, BigDecimal amp) {
        BigDecimal  valmin = BigDecimal.ZERO;
        BigDecimal  valmax = BigDecimal.ZERO;
        BigInteger  tensmax = BigInteger.ZERO;
        BigInteger  tensmin = BigInteger.ZERO;
        Set set = table.entrySet();

        // Displaying elements of LinkedHashMap
        if (amp.compareTo(BigDecimal.ZERO) < 0)
            amp = amp.multiply(BigDecimal.valueOf(-1L));
        Iterator iterator = set.iterator();
        if (iterator.hasNext()) {
            Map.Entry me = (Map.Entry) iterator.next();
            if (amp.compareTo((BigDecimal)me.getValue()) >= 0) {
                iterator = set.iterator();
                while (iterator.hasNext() && valmax.compareTo(amp) < 0) {
                    me = (Map.Entry) iterator.next();
                    valmin = valmax;
                    valmax = (BigDecimal) me.getValue();
                    tensmin = tensmax;
                    tensmax = (BigInteger) me.getKey();
                }
                System.out.print("Key is: " + valmax + " last key: " + valmin +
                        " Value is: " + tensmax + " Last amp is: " + tensmin + "\n");
                BigInteger scaled = calc_tens(amp, valmin, valmax, tensmin, tensmax);
                return scaled;
            }
        }
        return BigInteger.ZERO;
    }

    static public BigDecimal ParseAmp(LinkedHashMap table, BigInteger freq) {
        BigDecimal  attmin = BigDecimal.ZERO;
        BigDecimal  attmax = BigDecimal.ZERO;
        BigInteger  freqmax = BigInteger.ZERO;
        BigInteger  freqmin = BigInteger.ZERO;
        Set set = table.entrySet();

        // Displaying elements of LinkedHashMap
        Iterator iterator = set.iterator();
        if (iterator.hasNext()) {
            Map.Entry me = (Map.Entry) iterator.next();
            if (freq.compareTo((BigInteger) me.getKey()) >= 0) {
                iterator = set.iterator();
                while (iterator.hasNext() && freqmax.compareTo(freq) < 0) {
                    me = (Map.Entry) iterator.next();
                    freqmin = freqmax;
                    freqmax = (BigInteger) me.getKey();
                    attmin = attmax;
                    attmax = (BigDecimal) me.getValue();
                }
                if (attmax.signum() != 0 && attmin.signum() != 0 && freqmax.compareTo(freq) != 0)
                    attmax = calc_att(new BigDecimal(freq), attmin, attmax, new BigDecimal(freqmin), new BigDecimal(freqmax));
                System.out.print("Key is: " + freqmax + " last key: " + freqmin +
                        " Value is: " + attmax + " Last amp is: " + attmin + "\n");
                BigDecimal scaled = attmax.setScale(1, BigDecimal.ROUND_HALF_EVEN);
                return scaled;
            }
        }
        return BigDecimal.ZERO;
    }

    static public BigDecimal division(Integer numberpick, Integer unit, Double Freq){
        BigDecimal  res;
        BigDecimal  calc;

        calc = BigDecimal.valueOf(unit).multiply(BigDecimal.valueOf(Freq)).divide(BigDecimal.valueOf(1000000000), 9, BigDecimal.ROUND_HALF_UP);
        res = BigDecimal.valueOf(numberpick);
        res = res.divide(calc, 9, BigDecimal.ROUND_HALF_UP);
        return res;
    }

    static public Double ParseFreq(EditText txt)
    {
        Double tmp = 0D;
        String  Freqtxt = txt.getText().toString();
        if (Freqtxt.matches(""))
        {
            return -101D;
        }
        else {
            try {
                tmp = Double.parseDouble(Freqtxt);
            } catch (Exception e) {
                return -101D;
            }
        }
        return (tmp);
    }

    static public byte[] intToByteArray (final BigInteger value, final int start, int bits) {
        byte[] startbyte = new byte[1];
        final byte[] converted = value.toByteArray();

        startbyte[0] = (byte)start;

        byte[] zero = new byte[1];
        zero[0] = (byte)0;

        ByteBuffer bb = ByteBuffer.allocate(bits + 1);
        bb.put(startbyte);
        for(int i = 0; i != bits - converted.length; i++)
            bb.put(zero);
        bb.put(converted);
        byte[] result = bb.array();

        return result;
    }
}
