package com.sdrf.puccio_c.freqtester;

/**
 * Created by puccio_c on 4/10/18.
 */

public class Utils {

    static public byte[] intToByteArray (final int integer, final int start) {
        byte[] result = new byte[5];

        result[0] = (byte)start;
        result[1] = (byte)((integer & 0xFF000000) >> 24);
        result[2] = (byte)((integer & 0x00FF0000) >> 16);
        result[3] = (byte)((integer & 0x0000FF00) >> 8);
        result[4] = (byte)(integer & 0x000000FF);

        return result;
    }
}
