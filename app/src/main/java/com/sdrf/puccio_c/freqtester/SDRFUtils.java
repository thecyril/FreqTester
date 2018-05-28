package com.sdrf.puccio_c.freqtester;


import android.icu.math.BigDecimal;
import android.util.Log;
import android.widget.EditText;

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

    static public BigDecimal calc_moy(BigDecimal val1, BigDecimal val2) {
        BigDecimal res;
        BigDecimal ten = new BigDecimal(10D);
        res = (BigDecimal.valueOf(StrictMath.pow(ten.doubleValue() , val1.divide(ten, 3, BigDecimal.ROUND_HALF_EVEN).doubleValue()))
                .add(BigDecimal.valueOf(StrictMath.pow(ten.doubleValue(), val2.divide(ten, 3, BigDecimal.ROUND_HALF_EVEN).doubleValue()))));
        res = res.divide(new BigDecimal(2D), 3, BigDecimal.ROUND_HALF_EVEN);
        return res;
    }

    static public BigDecimal ParseAmp(LinkedHashMap table, BigInteger freq) {
        BigInteger  pfreq = BigInteger.ZERO;
        BigDecimal  pamp = BigDecimal.ZERO;
        BigDecimal  lastamp = BigDecimal.ZERO;
        Set set = table.entrySet();

        // Displaying elements of LinkedHashMap
        Iterator iterator = set.iterator();
        if (iterator.hasNext()) {
            Map.Entry me = (Map.Entry) iterator.next();
            if (freq.compareTo((BigInteger) me.getKey()) >= 0) {
                iterator = set.iterator();
                while (iterator.hasNext() && pfreq.compareTo(freq) < 0) {
                    me = (Map.Entry) iterator.next();
                    pfreq = (BigInteger) me.getKey();
                    lastamp = pamp;
                    pamp = (BigDecimal) me.getValue();
                }
               if (pamp.signum() != 0 && lastamp.signum() != 0 && pfreq.compareTo(freq) != 0)
                    pamp = BigDecimal.valueOf(10D).multiply(BigDecimal.valueOf(StrictMath.log10(calc_moy(lastamp, pamp).doubleValue())));
                System.out.print("Key is: " + pfreq +
                        " Value is: " + pamp + " Last amp is: " + lastamp + "\n");
            }
        }
        BigDecimal scaled = pamp.setScale(1, BigDecimal.ROUND_HALF_EVEN);
        return scaled;
    }

    static public BigDecimal Bigdivision(BigDecimal val, Double Freq){
        BigDecimal  res;
        BigDecimal  calc;

        calc = BigDecimal.valueOf(Freq).divide(BigDecimal.valueOf(1000000000), 9, BigDecimal.ROUND_HALF_UP);
        Log.d("$$$$$$$$$$$$$$$$$$$$$$$$$", calc.toString());
        res = val;
        res = res.divide(calc, 9, BigDecimal.ROUND_HALF_UP);
        return res;
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
            return -1D;
        }
        else {
            try {
                tmp = Double.parseDouble(Freqtxt);
            } catch (Exception e) {
                return -1D;
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


        //        int integer = integere.intValue();
        //result[0] = (byte)((integer & 0xFF000000) >> 24);
        //result[1] = (byte)((integer & 0x00FF0000) >> 16);
        //result[2] = (byte)((integer & 0x0000FF00) >> 8);
        //result[3] = (byte)(integer & 0x000000FF)
//
        //System.arraycopy(startbyte, 0, result, 0, startbyte.length);
//
        //for(i = 0; i != 9 - converted.length; i++)
        //    System.arraycopy(zero, 0, result, i, zero.length);
//
        //System.arraycopy(converted, 0, result, i, converted.length);
        return result;
    }
}
