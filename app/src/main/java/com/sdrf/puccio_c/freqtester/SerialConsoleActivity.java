/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.sdrf.puccio_c.freqtester;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.icu.math.BigDecimal;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Monitors a single {@link UsbSerialPort} instance, showing all data
 * received.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SerialConsoleActivity extends Activity {

    private final String TAG = SerialConsoleActivity.class.getSimpleName();

    /**
     * Driver instance, passed in statically via
     * {@link #show(Context, UsbSerialPort)}.
     *
     * <p/>
     * This is a devious hack; it'd be cleaner to re-create the driver using
     * arguments passed in with the {@link #startActivity(Intent)} intent. We
     * can get away with it because both activities will run in the same
     * process, and this is a simple demo.
     */
    private static UsbSerialPort    sPort = null;

    protected TextView                mTitleTextView;
    protected TextView                mDumpTextView;
    protected ScrollView              mScrollView;
    protected EditText                mFreqInput;
    protected Button                  mStart;
    protected ImageButton             mReset;
    protected Spinner                 mSpinner;
    protected BigInteger                 mFreq;
    protected static Double           mFreqmult;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
            SerialConsoleActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SerialConsoleActivity.this.updateReceivedData(data);
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serial_console);
        mTitleTextView = (TextView) findViewById(R.id.demoTitle);
        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);
        mFreqInput = (EditText) findViewById(R.id.Freq);
        mStart = (Button) findViewById(R.id.start);
        mReset = (ImageButton) findViewById(R.id.reset);

        mReset.setImageResource(R.drawable.reset);
        addItemsOnSpinner();
        addListenerOnSpinnerItemSelection();
        mFreqInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId== EditorInfo.IME_ACTION_DONE||actionId==EditorInfo.IME_ACTION_NEXT) {
                    prepareCommand();
                    return true;
                }
                return false;
            }
        });
        mStart.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
//                        Toast.makeText(getApplicationContext(),
//                                "Spinner : " + String.valueOf(mSpinner.getSelectedItem()),
//                                Toast.LENGTH_SHORT).show();
                        prepareCommand();
                    }
                });
        mReset.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        resetBoard();
                    }
                });
    }

    public void addItemsOnSpinner() {

        mSpinner = (Spinner) findViewById(R.id.Unit);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(SerialConsoleActivity.this,
                R.array.Units, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
    }

    public void addListenerOnSpinnerItemSelection() {
        mSpinner = (Spinner) findViewById(R.id.Unit);
        mSpinner.setOnItemSelectedListener(new SpinnerActivity());
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }
        finish();
    }

    void showStatus(TextView theTextView, String theLabel, boolean theValue){
        String msg = theLabel + ": " + (theValue ? "enabled" : "disabled") + "\n";
        theTextView.append(msg);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resumed, port=" + sPort);
        if (sPort == null) {
            mTitleTextView.setText("No serial device.");
        } else {
            final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

//            usbManager.requestPermission(sPort.getDriver().getDevice(), mPermissionIntent);
            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
                mTitleTextView.setText("Opening device failed");
                return;
            }

            try {
                sPort.open(connection);
                sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                showStatus(mDumpTextView, "CD  - Carrier Detect", sPort.getCD());
                showStatus(mDumpTextView, "CTS - Clear To Send", sPort.getCTS());
                showStatus(mDumpTextView, "DSR - Data Set Ready", sPort.getDSR());
                showStatus(mDumpTextView, "DTR - Data Terminal Ready", sPort.getDTR());
                showStatus(mDumpTextView, "DSR - Data Set Ready", sPort.getDSR());
                showStatus(mDumpTextView, "RI  - Ring Indicator", sPort.getRI());
                showStatus(mDumpTextView, "RTS - Request To Send", sPort.getRTS());

//                int i[] = {2, 77, 240, 120};                    // some value between 0 and 255


            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                mTitleTextView.setText("Error opening device: " + e.getMessage());
                try {
                    sPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sPort = null;
                return;
            }
            mTitleTextView.setText("Serial device: " + sPort.getClass().getSimpleName());
        }
        onDeviceStateChange();
    }

    public void prepareCommand()
    {
        String  Freqtxt = mFreqInput.getText().toString();
        if (Freqtxt.matches(""))
        {
            Toast.makeText(getApplicationContext(),
                    "Error you should put a value",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        else {
            try {
                Double tmp = Double.parseDouble(Freqtxt) * mFreqmult;
                mFreq = BigDecimal.valueOf(tmp).toBigInteger();
            } catch (Exception e) {
                return;
            }
            if (mFreq.longValue() > 100000000000L)
                Toast.makeText(getApplicationContext(),
                        "Value is too high",
                        Toast.LENGTH_SHORT).show();
            else {
                Toast.makeText(getApplicationContext(),
                        String.valueOf(mFreq),
                        Toast.LENGTH_SHORT).show();

                Log.i("mFreq", String.format("%d", mFreq));
            }
            sendCommand();
        }
    }

    public void sendCommand(){

        byte[] msg = Utils.intToByteArray(mFreq, 30);

        Log.d(TAG, mFreq.toString());

        for (byte b : msg) {
            System.out.println(Integer.toBinaryString(b & 255 | 256).substring(1));
        }
        for (int index = 0; index < msg.length; index++) {
            Log.i("Byte", String.format("0x%20x", msg[index]));
        }
            try {
                sPort.write(msg, 10);
        } catch (IOException e) {
            Log.e(TAG, "Write Error");
        }
    }

    public void resetBoard(){
        byte[] msg = Utils.intToByteArray(BigInteger.valueOf(0), 5);
        try{
            sPort.write(msg, 10);
        } catch (IOException e) {
            Log.e(TAG, "Write Error");
        }
    }
    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void updateReceivedData(byte[] data) {
        final String message = "Read " + data.length + " bytes: \n"
                + HexDump.dumpHexString(data) + "\n\n";
        mDumpTextView.append(message);
        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
    }

    /**
     * Starts the activity, using the supplied driver instance.
     *
     * @param context
     * @param driver
     */
    static void show(Context context, UsbSerialPort port) {
        sPort = port;
        final Intent intent = new Intent(context, SerialConsoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

}
