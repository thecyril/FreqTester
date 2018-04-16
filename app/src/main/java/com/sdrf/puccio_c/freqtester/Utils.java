package com.sdrf.puccio_c.freqtester;

import android.util.Log;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Created by puccio_c on 4/10/18.
 */

public class Utils {

    static public byte[] intToByteArray (final BigInteger integer, final int start) {
        byte[] startbyte = new byte[1];
        final byte[] converted = integer.toByteArray();

        startbyte[0] = (byte)start;

        byte[] zero = new byte[1];
        zero[0] = (byte)0;

        ByteBuffer bb = ByteBuffer.allocate(9);
        bb.put(startbyte);
        for(int i = 0; i != 8 - converted.length; i++)
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
