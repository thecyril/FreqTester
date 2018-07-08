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

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.icu.math.BigDecimal;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerialConsoleActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = SerialConsoleActivity.class.getSimpleName();

    private static UsbSerialPort    sPort = null;

    public static final int FILE_CODE = 1;


    protected TextView              mTitleTextView;
    protected TextView              mDumpTextView;
    protected TextView              mPath;
    protected EditText              mOuput;
    protected ScrollView            mScrollView;
    protected EditText              mFreqInput;
    protected EditText              mFreqStart;
    protected EditText              mFreqStop;
    protected EditText              mAmpinput;
    protected EditText              mDelay;
    protected EditText              mCorrection;
    protected Button                mStart;
    protected Button                mReset;
    protected ImageButton           mErase;
    protected ImageButton           mPlay;
    protected ImageButton           mStop;
    protected ImageButton           mLoop;
    protected Boolean               mBoucle;
    protected Spinner               mSpinner;
    protected Spinner               mSpinnerStep;
    protected Spinner               mSpinnerDbm;
    protected static Double         mFreqmult;
    protected static Integer        mNbDiv;
    protected Toolbar               mToolbar;
    protected ActionBar             mActionBar;
    protected Button                mDecrease;
    protected Button                mIncrease;
    protected Button                mDecreaseamp;
    protected Button                mIncreaseamp;
    protected Button                mSetAmp;
    protected Button                mCalib;
    protected CheckBox              mTone;
    protected NumberPicker          np;
    protected BigInteger            mFreq;
    protected BigDecimal            mNb;
    protected BigDecimal            mAmp;
    protected BigDecimal            mCorrectedamp;
    protected static BigDecimal     mDbm;
    protected Integer               mVnp;
    protected Boolean               isOn;
    private Timer                   timer;
    private TimerTask               timerTask;
    private Switch                  mSwRf;
    private Switch                  mSwRef;
    private Handler                 handler = new Handler();
    private Runnable                runnable;
    private String                  mMessage = "";
    protected LinkedHashMap<BigInteger, BigDecimal> mTable;
    protected LinkedHashMap<BigInteger, BigDecimal> mTension;


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
        mTitleTextView  = (TextView) findViewById(R.id.demoTitle);
        mDumpTextView   = (TextView) findViewById(R.id.consoleText);
        mPath           = (TextView) findViewById(R.id.file);
        mOuput          = (EditText) findViewById(R.id.output);
        mScrollView     = (ScrollView) findViewById(R.id.demoScroller);
        mFreqInput      = (EditText) findViewById(R.id.Freq);
        mFreqStart      = (EditText) findViewById(R.id.Start);
        mFreqStop       = (EditText) findViewById(R.id.Stop);
        mDelay          = (EditText) findViewById(R.id.Delay);
        mCorrection     = (EditText) findViewById(R.id.correction);
        mStart          = (Button) findViewById(R.id.start);
        mReset          = (Button) findViewById(R.id.reset);
        mSetAmp         = (Button) findViewById(R.id.setamp);
        mToolbar        = (Toolbar) findViewById(R.id.my_toolbar);
        mDecrease       = (Button) findViewById(R.id.decrease);
        mIncrease       = (Button) findViewById(R.id.increase);
        mDecreaseamp    = (Button) findViewById(R.id.decreaseamp);
        mIncreaseamp    = (Button) findViewById(R.id.increaseamp);
        mCalib          = (Button) findViewById(R.id.calib);
        mErase          = (ImageButton) findViewById(R.id.erase);
        mPlay           = (ImageButton) findViewById(R.id.play);
        mStop           = (ImageButton) findViewById(R.id.stop);
        mLoop           = (ImageButton) findViewById(R.id.loop);
        np              = (NumberPicker) findViewById(R.id.np);
        mSwRf           = (Switch) findViewById(R.id.RF);
        mSwRef          = (Switch) findViewById(R.id.Ref);
        mAmpinput       = (EditText) findViewById(R.id.Amp);
        mTone           = (CheckBox) findViewById(R.id.tone);


        sPort = MainActivity.mPort;
        np.setMinValue(1);
        np.setMaxValue(1000);
        np.setWrapSelectorWheel(true);
        np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal){
                //Display the newly selected number from picker
                mVnp = newVal;
                Log.d(TAG, mVnp.toString());
            }
        });
        isOn = false;
        mVnp = 1;
        mAmp = BigDecimal.ZERO;
        mCorrectedamp = BigDecimal.ZERO;
        mNb = BigDecimal.ZERO;
        mFreq = BigInteger.ZERO;
        mBoucle = false;
        displayInt(0, mFreqStart);
        displayInt(0, mFreqStop);
        displayInt(800, mDelay);
        display(mNb, mFreqInput);
        display(mAmp, mAmpinput);
        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(true);
        addItemsOnSpinner();
        addListenerOnSpinnerItemSelection();
        mFreqInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId== EditorInfo.IME_ACTION_DONE||actionId==EditorInfo.IME_ACTION_NEXT) {
                    SDRFUtils.sendCommand(BigInteger.valueOf(0), 31, 1, sPort);
                    sendFreq(mFreqInput, 33);
                    rfstate();
                    return true;
                }
                return false;
            }
        });
        mAmpinput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId== EditorInfo.IME_ACTION_DONE||actionId==EditorInfo.IME_ACTION_NEXT) {
                    sendAmp(mAmpinput, 32);
                    return true;
                }
                return false;
            }
        });
        mSwRf.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ((isOn = isChecked)) {
                    SDRFUtils.sendCommand(BigInteger.valueOf(1), 31, 1, sPort);
                } else {
                    SDRFUtils.sendCommand(BigInteger.valueOf(0), 31, 1, sPort);
                }
            }
        });
        mSwRef.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SDRFUtils.sendCommand(BigInteger.valueOf(1), 28, 1, sPort);
                } else {
                    SDRFUtils.sendCommand(BigInteger.valueOf(0), 28, 1, sPort);
                }
            }
        });
        mStart.setOnClickListener(this);
        mReset.setOnClickListener(this);
        mDecrease.setOnClickListener(this);
        mIncrease.setOnClickListener(this);
        mIncreaseamp.setOnClickListener(this);
        mDecreaseamp.setOnClickListener(this);
        mErase.setOnClickListener(this);
        mPlay.setOnClickListener(this);
        mStop.setOnClickListener(this);
        mLoop.setOnClickListener(this);
        mSetAmp.setOnClickListener(this);
        mCalib.setOnClickListener(this);
        mTone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if ( isChecked )
                {
                    Intent i = new Intent(getApplicationContext(), USBChooserActivity.class);
                    startActivity(i);
                }

            }
        });;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#66BB6A"));
        }
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.start:
                SDRFUtils.sendCommand(BigInteger.valueOf(0), 31, 1, sPort);
                sendFreq(mFreqInput, 33);
                rfstate();
                break;
            case R.id.reset:
                resetBoard();
                break;
            case R.id.home:
                finish();
                break;
            case R.id.erase:
                display(BigDecimal.valueOf(0), mFreqInput);
                break;
            case R.id.play:
                mNb = BigDecimal.valueOf(SDRFUtils.ParseFreq(mFreqStart));
                resetBoard();
                startTimer();
                break;
            case R.id.stop:
                mBoucle = false;
                stopTimer();
                break;
            case R.id.loop:
                mBoucle = true;
                mNb = BigDecimal.valueOf(SDRFUtils.ParseFreq(mFreqStart));
                resetBoard();
                startTimer();
                break;
            case R.id.calib:
                openFolder();
                break;
            case R.id.increase:
                if (!(mNb = BigDecimal.valueOf(SDRFUtils.ParseFreq(mFreqInput))).equals(BigDecimal.valueOf(-101D)))
                    mNb = mNb.add(SDRFUtils.division(mVnp, mNbDiv, mFreqmult));
                else
                    return;
                display(mNb, mFreqInput);
                SDRFUtils.sendCommand(BigInteger.valueOf(0), 31, 1, sPort);
                sendFreq(mFreqInput, 33);
                rfstate();
                break;
            case R.id.decrease:
                if (!(mNb = BigDecimal.valueOf(SDRFUtils.ParseFreq(mFreqInput))).equals(BigDecimal.valueOf(-101D)) && mNb.signum() > 0)
                    mNb = mNb.subtract(SDRFUtils.division(mVnp, mNbDiv, mFreqmult));
                else
                    return;
                display(mNb, mFreqInput);
                SDRFUtils.sendCommand(BigInteger.valueOf(0), 31, 1, sPort);
                sendFreq(mFreqInput, 33);
                rfstate();
                break;
            case R.id.setamp:
                sendAmp(mAmpinput, 32);
                break;
            case R.id.increaseamp:
                if (!(mAmp = BigDecimal.valueOf(SDRFUtils.ParseFreq(mAmpinput))).equals(BigDecimal.valueOf(-101D)))
                    mAmp = mAmp.add(mDbm);
                else
                    return;
                display(mAmp, mAmpinput);
                sendAmp(mAmpinput, 32);
                break;
            case R.id.decreaseamp:
                if ((mAmp = BigDecimal.valueOf(SDRFUtils.ParseFreq(mAmpinput))).compareTo(BigDecimal.valueOf(-100D)) > 0)
                    mAmp = mAmp.subtract(mDbm);
                else
                    return;
                display(mAmp, mAmpinput);
                sendAmp(mAmpinput, 32);
                break;
        }
    }

    public void rfstate()
    {
        if (isOn)
            SDRFUtils.sendCommand(BigInteger.valueOf(1), 31, 1, sPort);
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.tone:
                if (checked) {
                    Toast.makeText(getApplicationContext(), "checked", Toast.LENGTH_SHORT).show();
                }
            else
                // Remove the meat
                break;
        }
    }

    public void openFolder()
    {
        Intent i = new Intent(this, FilePickerActivity.class);

        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(i, FILE_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        try
        {
            if (requestCode == FILE_CODE && resultCode == -1)
            {
                List<Uri> files = Utils.getSelectedFilesFromResult(data);
                for (Uri uri : files)
                {
                    File file = Utils.getFileForUri(uri);
                    InputStreamReader is1 = new InputStreamReader(new FileInputStream(file));
                    InputStreamReader is2 = new InputStreamReader(getAssets()
                            .open("vga_ctrl_voltage_att.csv"));

                    if ((mTable = SDRFUtils.csvRead(is1)) == null)
                        Toast.makeText(getApplicationContext(), "Wrong Csv", Toast.LENGTH_SHORT).show();
                    mTension = SDRFUtils.csvRead(is2);

                    mPath.setText(file.getPath().substring(file.getPath().lastIndexOf("/") + 1));
                    Toast.makeText(getApplicationContext(), file.getPath(), Toast.LENGTH_SHORT).show();
                }
            }
        }
        catch (Exception ex) {
            Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopTimer(){
        handler.removeCallbacks(runnable);
    }

    //To start timer
    public void startTimer() {
        handler.post(runnable = new Runnable() {
            public void run(){
                BigDecimal stop = BigDecimal.valueOf(SDRFUtils.ParseFreq(mFreqStop));
                if (mNb.compareTo(stop) > 0) {
                    if (mBoucle.booleanValue() == true)
                        mNb = BigDecimal.valueOf(SDRFUtils.ParseFreq(mFreqStart));
                    else
                        stopTimer();
                    }
                    else {
                    display(mNb, mFreqInput);
                    sendFreq(mFreqInput, 34);
                    mNb = mNb.add(SDRFUtils.division(mVnp, mNbDiv, mFreqmult));
                }
                handler.postDelayed(runnable, SDRFUtils.ParseFreq(mDelay).longValue());
            }
        });
    }

    private void display(BigDecimal number, TextView tv) {
        tv.setText("" + number);
    }

    private void displayInt(Integer number, TextView tv) {
        tv.setText("" + number);
    }

    public void addItemsOnSpinner() {

        mSpinner = (Spinner) findViewById(R.id.Unit);
        mSpinnerStep = (Spinner) findViewById(R.id.Unit2);
        mSpinnerDbm = (Spinner) findViewById(R.id.Dbm);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(SerialConsoleActivity.this,
                R.array.Units, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> DbmAdapter = ArrayAdapter.createFromResource(SerialConsoleActivity.this,
                R.array.dbm, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        DbmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinnerStep.setAdapter(adapter);
        mSpinnerDbm.setAdapter(DbmAdapter);
    }

    public void addListenerOnSpinnerItemSelection() {
        mSpinner = (Spinner) findViewById(R.id.Unit);
        mSpinner.setOnItemSelectedListener(new SpinnerActivity());
        mSpinnerStep = (Spinner) findViewById(R.id.Unit2);
        mSpinnerStep.setOnItemSelectedListener(new SpinnerActivity());
        mSpinnerDbm = (Spinner) findViewById(R.id.Dbm);
        mSpinnerDbm.setOnItemSelectedListener(new SpinnerActivity());
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
//            sPort = null;
        }
        //finish();
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
                sPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

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
            mTitleTextView.setText("Serial device: " + sPort.getDriver().getDevice().getProductName());
        }
        onDeviceStateChange();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

        }
    }

    public void sendAmp(EditText input, Integer nb)
    {
        BigInteger tension = BigInteger.ZERO;
        if ((mAmp = BigDecimal.valueOf(SDRFUtils.ParseFreq(input))).doubleValue() < -5-101D)
        {
            Toast.makeText(getApplicationContext(),
                    "value is to low",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        else {
            try {
                if (mTable != null && mTension != null)
                    display((mCorrectedamp = SDRFUtils.ParseAmp(mTable, mFreq)), mCorrection);
                mAmp = mAmp.subtract(mCorrectedamp);
                tension = SDRFUtils.ParseTension(mTension, mAmp);
                Log.d("Tension", tension.toString());
            } catch (Exception e) {
                return;
            }
            if (mAmp.longValue() > 30L)
                Toast.makeText(getApplicationContext(),
                        "Value is too high",
                        Toast.LENGTH_SHORT).show();
            else if (mAmp.longValue() < -50L)
                Toast.makeText(getApplicationContext(),
                        "Value is too Low",
                        Toast.LENGTH_SHORT).show();
            else {
                Toast.makeText(getApplicationContext(),
                        String.valueOf(tension),
                        Toast.LENGTH_SHORT).show();

                Log.i("Correct amp", mCorrectedamp.toString());
            }
            SDRFUtils.sendCommand(tension, nb, 2, sPort);
        }
    }

    public void sendFreq(EditText input, Integer nb)
    {
        if ((mNb = BigDecimal.valueOf(SDRFUtils.ParseFreq(input))).doubleValue() < -101D)
        {
            Toast.makeText(getApplicationContext(),
                    "Error you should enter a value",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        else {
            try {
                mFreq = mNb.multiply(BigDecimal.valueOf(mFreqmult)).toBigInteger();
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
            sendAmp(mAmpinput, 32);
            SDRFUtils.sendCommand(mFreq, nb, 8, sPort);
        }
    }

    public void resetBoard(){
        if (sPort == null)
            return;
        byte[] msg = SDRFUtils.intToByteArray(BigInteger.valueOf(0), 5, 2);
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

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.item, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
       if(item.getItemId()== android.R.id.home)
           finish();
        switch (item.getItemId()) {
            case R.id.home:
                finish();
                return true;
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
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
        mOuput.setText(HexDump.dumpHexString(data));
        mMessage = HexDump.dumpHexString(data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
        }
        stopTimer();
//        finish();
    }
}
