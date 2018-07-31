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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.icu.math.BigDecimal;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
    private static UsbSerialPort    sPort2 = null;

    public static final int FILE_CODE = 1;


    protected TextView              mTitleTextView;
    protected TextView              mUsbDevice2;
    protected TextView              mDumpTextView;
    protected TextView              mPath;
    protected TextView              mPath2;
    protected EditText              mOuput;
    protected ScrollView            mScrollView;
    protected EditText              mFreqInput;
    protected EditText              mFreqInput2;
    protected EditText              mFreqStart;
    protected EditText              mFreqStop;
    protected EditText              mAmpinput;
    protected EditText              mAmpinput2;
    protected EditText              mVainput;
    protected EditText              mDelay;
    protected EditText              mCorrection;
    protected EditText              mCorrection2;
    protected Button                mStart;
    protected Button                mSetVa;
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
    protected Button                mDecrease2;
    protected Button                mIncrease;
    protected Button                mIncrease2;
    protected Button                mDecreaseamp;
    protected Button                mIncreaseamp;
    protected Button                mDecreaseamp2;
    protected Button                mIncreaseamp2;
    protected Button                mDecreaseVa;
    protected Button                mIncreaseVa;
    protected Button                mSetAmp;
    protected Button                mCalib;
    protected Button                mCalib2;
    protected CheckBox              mTone;
    protected NumberPicker          np;
    protected BigInteger            mFreq;
    protected BigInteger            mFreq2;
    protected BigInteger            mVa;
    protected BigDecimal            mNb;
    protected BigDecimal            mAmp;
    protected BigDecimal            mCorrectedamp;
    protected BigDecimal            mCorrectedamp2;
    protected static BigDecimal     mDbm;
    protected Integer               mVnp;
    protected Boolean               isOn;
    protected Boolean               is2tone;
    private Timer                   timer;
    private TimerTask               timerTask;
    private Switch                  mSwRf;
    private Switch                  mSwRef;
    private Handler                 handler = new Handler();
    private Runnable                runnable;
    private String                  mMessage = "";
    protected LinkedHashMap<BigInteger, BigDecimal> mTable;
    protected LinkedHashMap<BigInteger, BigDecimal> mTable2;
    protected LinkedHashMap<BigInteger, BigDecimal> mTension;


    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService mExecutor2 = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;
    private SerialInputOutputManager mSerialIoManager2;

    //to make the communication working you need to add a listener to the incoming data.

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
    private final SerialInputOutputManager.Listener mListener2 =
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
    final BroadcastReceiver detachReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED))
                finish();
        }
    };

    //this is the first called function when you open the app
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serial_console);


        IntentFilter filters = new IntentFilter();
        filters.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(detachReceiver, filters);

        mTitleTextView  = findViewById(R.id.demoTitle);
        mUsbDevice2     = findViewById(R.id.usbdevice2);
        mDumpTextView   = findViewById(R.id.consoleText);
        mPath           = findViewById(R.id.file);
        mPath2          = findViewById(R.id.file2);
        mOuput          = findViewById(R.id.output);
        mScrollView     = findViewById(R.id.demoScroller);
        mFreqInput      = findViewById(R.id.Freq);
        mFreqInput2     = findViewById(R.id.Freq2);
        mFreqStart      = findViewById(R.id.Start);
        mFreqStop       = findViewById(R.id.Stop);
        mDelay          = findViewById(R.id.Delay);
        mCorrection     = findViewById(R.id.correction);
        mCorrection2    = findViewById(R.id.correction2);
        mStart          = findViewById(R.id.start);
        mReset          = findViewById(R.id.reset);
        mSetAmp         = findViewById(R.id.setamp);
        mSetVa          = findViewById(R.id.setva);
        mToolbar        = findViewById(R.id.my_toolbar);
        mDecrease       = findViewById(R.id.decrease);
        mDecrease2      = findViewById(R.id.decrease2);
        mIncrease       = findViewById(R.id.increase);
        mIncrease2      = findViewById(R.id.increase2);
        mDecreaseamp    = findViewById(R.id.decreaseamp);
        mIncreaseamp    = findViewById(R.id.increaseamp);
        mDecreaseamp2   = findViewById(R.id.decreaseamp2);
        mIncreaseamp2   = findViewById(R.id.increaseamp2);
        mDecreaseVa     = findViewById(R.id.decreaseva);
        mIncreaseVa     = findViewById(R.id.increaseva);
        mCalib          = findViewById(R.id.calib);
        mCalib2         = findViewById(R.id.calib2);
        mErase          = findViewById(R.id.erase);
        mPlay           = findViewById(R.id.play);
        mStop           = findViewById(R.id.stop);
        mLoop           = findViewById(R.id.loop);
        np              = findViewById(R.id.np);
        mSwRf           = findViewById(R.id.RF);
        mSwRef          = findViewById(R.id.Ref);
        mAmpinput       = findViewById(R.id.Amp);
        mAmpinput2      = findViewById(R.id.Amp2);
        mVainput        = findViewById(R.id.vainput);
        mTone           = findViewById(R.id.Amplitude);


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
        mUsbDevice2.setText("");
        is2tone = false;
        isOn = false;
        mVnp = 1;
        mAmp = BigDecimal.ZERO;
        mCorrectedamp = BigDecimal.ZERO;
        mCorrectedamp2 = BigDecimal.ZERO;
        mNb = BigDecimal.ZERO;
        mFreq = BigInteger.ZERO;
        mVa = BigInteger.ZERO;
        mBoucle = false;
        displayInt(0, mFreqStart);
        displayInt(0, mFreqStop);
        displayInt(800, mDelay);
        display(mNb, mFreqInput);
        display(mAmp, mAmpinput);
        display(mAmp, mAmpinput2);
        displayInt(mVa.intValue(), mVainput);
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
                    sendFreq(mFreqInput, 33);
                    rfstate();
                    return true;
                }
                return false;
            }
        });
        mFreqInput2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId== EditorInfo.IME_ACTION_DONE||actionId==EditorInfo.IME_ACTION_NEXT && is2tone && sPort2 != null) {
                    sendFreq2(mFreqInput2, 33);
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
                    sendAmp(mAmpinput, 32, sPort);
                    return true;
                }
                return false;
            }
        });
        mAmpinput2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId== EditorInfo.IME_ACTION_DONE||actionId==EditorInfo.IME_ACTION_NEXT) {
                    if (sPort2 != null && is2tone)
                        sendAmp2(mAmpinput2, 32, sPort2);
                    return true;
                }
                return false;
            }
        });
        mVainput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId== EditorInfo.IME_ACTION_DONE||actionId==EditorInfo.IME_ACTION_NEXT) {
                    sendVa(mVainput, 35, sPort);
                    if (sPort2 != null && is2tone)
                        sendVa(mVainput, 35, sPort2);
                    return true;
                }
                return false;
            }
        });
        mSwRf.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ((isOn = isChecked)) {
                    SDRFUtils.sendCommand(BigInteger.valueOf(1), 31, 1, sPort);
                    if (sPort2 != null && is2tone)
                        SDRFUtils.sendCommand(BigInteger.valueOf(1), 31, 1, sPort2);
                } else {
                    SDRFUtils.sendCommand(BigInteger.valueOf(0), 31, 1, sPort);
                    if (sPort2 != null && is2tone)
                        SDRFUtils.sendCommand(BigInteger.valueOf(0), 31, 1, sPort2);
                }
            }
        });
        mSwRef.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SDRFUtils.sendCommand(BigInteger.valueOf(1), 28, 1, sPort);
                    if (sPort2 != null && is2tone)
                        SDRFUtils.sendCommand(BigInteger.valueOf(1), 28, 1, sPort2);
                } else {
                    SDRFUtils.sendCommand(BigInteger.valueOf(0), 28, 1, sPort);
                    if (sPort2 != null && is2tone)
                        SDRFUtils.sendCommand(BigInteger.valueOf(0), 28, 1, sPort2);
                }
            }
        });
        mStart.setOnClickListener(this);
        mReset.setOnClickListener(this);
        mSetVa.setOnClickListener(this);
        mDecrease.setOnClickListener(this);
        mDecrease2.setOnClickListener(this);
        mIncrease.setOnClickListener(this);
        mIncrease2.setOnClickListener(this);
        mIncreaseamp.setOnClickListener(this);
        mDecreaseamp.setOnClickListener(this);
        mIncreaseamp2.setOnClickListener(this);
        mDecreaseamp2.setOnClickListener(this);
        mIncreaseVa.setOnClickListener(this);
        mDecreaseVa.setOnClickListener(this);
        mErase.setOnClickListener(this);
        mPlay.setOnClickListener(this);
        mStop.setOnClickListener(this);
        mLoop.setOnClickListener(this);
        mSetAmp.setOnClickListener(this);
        mCalib.setOnClickListener(this);
        mCalib2.setOnClickListener(this);
        //2tone listener
        mTone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if ( isChecked )
                {
                    is2tone = true;
                    displayInt(0, mFreqInput2);
                    Intent i = new Intent(getApplicationContext(), USBChooserActivity.class);
                    startActivity(i);
                }
                else {
                    is2tone = false;
                    resetBoard2();
                    if (sPort2 != null && sPort2.getPortNumber() != sPort.getPortNumber()) {
                        try {
                            sPort2.close();
                        } catch (IOException e) {
                            // Ignore.
                        }
                    }
                    sPort2 = null;
                    Singleton.getInstance().setPort(null);
                }

            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#66BB6A"));
        }
    }

    //onClick is a listener to every graphic component you can link an action to the start button for example
    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.start:
                sendFreq(mFreqInput, 33);
                if (sPort2 != null && is2tone)
                    sendFreq2(mFreqInput2, 33);
                break;
            case R.id.reset:
                resetBoard();
                if (sPort2 != null && is2tone)
                    resetBoard2();
                break;
            case R.id.setva:
                sendVa(mVainput,35, sPort);
                if (sPort2 != null && is2tone)
                    sendVa(mVainput,35, sPort);
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
            case R.id.calib2:
                openFolder2();
                break;
            case R.id.increase:
                if (!(mNb = BigDecimal.valueOf(SDRFUtils.ParseFreq(mFreqInput))).equals(BigDecimal.valueOf(-101D)))
                    mNb = mNb.add(SDRFUtils.division(mVnp, mNbDiv, mFreqmult));
                else
                    return;
                display(mNb, mFreqInput);
                sendFreq(mFreqInput, 33);
                break;
            case R.id.decrease:
                if (!(mNb = BigDecimal.valueOf(SDRFUtils.ParseFreq(mFreqInput))).equals(BigDecimal.valueOf(-101D)) && mNb.signum() > 0)
                    mNb = mNb.subtract(SDRFUtils.division(mVnp, mNbDiv, mFreqmult));
                else
                    return;
                display(mNb, mFreqInput);
                sendFreq(mFreqInput, 33);
                break;
            case R.id.increase2:
                if (!(mNb = BigDecimal.valueOf(SDRFUtils.ParseFreq(mFreqInput2))).equals(BigDecimal.valueOf(-101D)))
                    mNb = mNb.add(SDRFUtils.division(mVnp, mNbDiv, mFreqmult));
                else
                    return;
                display(mNb, mFreqInput2);
                sendFreq2(mFreqInput2, 33);
                break;
            case R.id.decrease2:
                if (!(mNb = BigDecimal.valueOf(SDRFUtils.ParseFreq(mFreqInput2))).equals(BigDecimal.valueOf(-101D)) && mNb.signum() > 0)
                    mNb = mNb.subtract(SDRFUtils.division(mVnp, mNbDiv, mFreqmult));
                else
                    return;
                display(mNb, mFreqInput2);
                sendFreq2(mFreqInput2, 33);
                break;
            case R.id.setamp:
                sendAmp(mAmpinput, 32, sPort);
                if (sPort2 != null && is2tone)
                    sendAmp2(mAmpinput2, 32, sPort2);
                break;
            case R.id.increaseamp:
                if (!(mAmp = BigDecimal.valueOf(SDRFUtils.ParseFreq(mAmpinput))).equals(BigDecimal.valueOf(-101D)))
                    mAmp = mAmp.add(mDbm);
                else
                    return;
                display(mAmp, mAmpinput);
                sendAmp(mAmpinput, 32, sPort);
                break;
            case R.id.decreaseamp:
                if ((mAmp = BigDecimal.valueOf(SDRFUtils.ParseFreq(mAmpinput))).compareTo(BigDecimal.valueOf(-100D)) > 0)
                    mAmp = mAmp.subtract(mDbm);
                else
                    return;
                display(mAmp, mAmpinput);
                sendAmp(mAmpinput, 32, sPort);
                break;
            case R.id.increaseamp2:
                if (!(mAmp = BigDecimal.valueOf(SDRFUtils.ParseFreq(mAmpinput2))).equals(BigDecimal.valueOf(-101D)))
                    mAmp = mAmp.add(mDbm);
                else
                    return;
                display(mAmp, mAmpinput2);
                if (sPort2 != null && is2tone)
                    sendAmp2(mAmpinput2, 32, sPort2);
                break;
            case R.id.decreaseamp2:
                if ((mAmp = BigDecimal.valueOf(SDRFUtils.ParseFreq(mAmpinput2))).compareTo(BigDecimal.valueOf(-100D)) > 0)
                    mAmp = mAmp.subtract(mDbm);
                else
                    return;
                display(mAmp, mAmpinput2);
                if (sPort2 != null && is2tone)
                    sendAmp2(mAmpinput2, 32, sPort2);
                break;
            case R.id.increaseva:
                if (!(mVa = BigInteger.valueOf(SDRFUtils.ParseFreq(mVainput).longValue())).equals(BigInteger.valueOf(-101L)))
                    mVa = mVa.add(BigInteger.valueOf(100L));
                else
                    return;
                displayInt(mVa.intValue(), mVainput);
                sendVa(mVainput, 35, sPort);
                if (sPort2 != null && is2tone)
                    sendVa(mVainput, 35, sPort2);
                break;
            case R.id.decreaseva:
                if ((mVa = BigInteger.valueOf(SDRFUtils.ParseFreq(mVainput).longValue())).compareTo(BigInteger.valueOf(-100L)) > 0 && mVa.signum() > 0)
                    mVa = mVa.subtract(BigInteger.valueOf(100L));
                else
                    return;
                displayInt(mVa.intValue(), mVainput);
                sendVa(mVainput, 35, sPort);
                if (sPort2 != null && is2tone)
                    sendVa(mVainput, 35, sPort2);
                break;
        }
    }

    public void rfstate()
    {
        if (isOn)
            SDRFUtils.sendCommand(BigInteger.valueOf(1), 31, 1, sPort);
    }

    public void rfstate2()
    {
        if (sPort2 != null && is2tone && isOn)
            SDRFUtils.sendCommand(BigInteger.valueOf(1), 31, 1, sPort2);
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.Amplitude:
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
        //Here where i sat the path to the Tables folder, juste delete the + "/Tables" to change it
        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath() + "/Tables");

        startActivityForResult(i, FILE_CODE);
    }

    public void openFolder2()
    {
        Intent i = new Intent(this, FilePickerActivity.class);

        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath() + "/Tables");

        startActivityForResult(i, 2);
    }

    //This is the function called when a correction file is loaded
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
            if (requestCode == 2 && resultCode == -1)
            {
                List<Uri> files = Utils.getSelectedFilesFromResult(data);
                for (Uri uri : files)
                {
                    File file = Utils.getFileForUri(uri);
                    InputStreamReader is1 = new InputStreamReader(new FileInputStream(file));
                    InputStreamReader is2 = new InputStreamReader(getAssets()
                            .open("vga_ctrl_voltage_att.csv"));

                    if ((mTable2 = SDRFUtils.csvRead(is1)) == null)
                        Toast.makeText(getApplicationContext(), "Wrong Csv", Toast.LENGTH_SHORT).show();
                    if (mTension == null)
                        mTension = SDRFUtils.csvRead(is2);

                    mPath2.setText(file.getPath().substring(file.getPath().lastIndexOf("/") + 1));
                    Toast.makeText(getApplicationContext(), file.getPath(), Toast.LENGTH_SHORT).show();
                }
            }
        }
        catch (Exception ex) {
            Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //To stop the sweep timer
    private void stopTimer(){
        handler.removeCallbacks(runnable);
    }

    //To start the sweep timer
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

    //spinner class corresponding to the Frequency and the dbm spinner
    public void addItemsOnSpinner() {

        mSpinner = findViewById(R.id.Unit);
        mSpinnerStep = findViewById(R.id.Unit2);
        mSpinnerDbm = findViewById(R.id.Dbm);
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
        mSpinner = findViewById(R.id.Unit);
        mSpinner.setOnItemSelectedListener(new SpinnerActivity());
        mSpinnerStep = findViewById(R.id.Unit2);
        mSpinnerStep.setOnItemSelectedListener(new SpinnerActivity());
        mSpinnerDbm = findViewById(R.id.Dbm);
        mSpinnerDbm.setOnItemSelectedListener(new SpinnerActivity());
    }

    //be careful to always close the opened port before closing
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
        if (sPort2 != null && is2tone) {
            try {
                sPort2.close();
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

    //called everytime the application is suspended like when you open the browse folder you need to reconnect to the usb port
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resumed, port=" + sPort);

            sPort = SDRFUtils.usbConnect(sPort, mTitleTextView, this.getApplicationContext());

        if ((sPort2 = Singleton.getInstance().getPort()) != null) {
            sPort2 = SDRFUtils.usbConnect(sPort2, mUsbDevice2, this.getApplicationContext());
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

    //the function that set the amp you need to provide the edittext field where the amp is setted, the nb is the number of byte to send and a port
    public void sendAmp(EditText input, Integer nb, UsbSerialPort port)
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
                //function are located in the SDRFUtils class so you can use them like that
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
            SDRFUtils.sendCommand(tension, nb, 2, port);
        }
    }

    //same as sendamp but for the 2tone
    public void sendAmp2(EditText input, Integer nb, UsbSerialPort port)
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
                if (mTable2 != null && mTension != null)
                    display((mCorrectedamp2 = SDRFUtils.ParseAmp(mTable2, mFreq2)), mCorrection2);
                mAmp = mAmp.subtract(mCorrectedamp2);
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

                Log.i("Correct amp", mCorrectedamp2.toString());
            }
            SDRFUtils.sendCommand(tension, nb, 2, port);
        }
    }

    //use to send frequency to the board
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
                Log.i("mFreq", String.format("%d", mFreq));
            }
            //rf off
            SDRFUtils.sendCommand(BigInteger.valueOf(0), 31, 1, sPort);
            //send the amp
            sendAmp(mAmpinput, 32, sPort);
            //send the freq
            SDRFUtils.sendCommand(mFreq, nb, 8, sPort);
            //check if rfon is checked and send rf on if it is the case
            rfstate();
        }
    }

    //same as sendfreq but for the 2tone
    public void sendFreq2(EditText input, Integer nb)
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
                mFreq2 = mNb.multiply(BigDecimal.valueOf(mFreqmult)).toBigInteger();
            } catch (Exception e) {
                return;
            }
            if (mFreq2.longValue() > 100000000000L)
                Toast.makeText(getApplicationContext(),
                        "Value is too high",
                        Toast.LENGTH_SHORT).show();
            else {
                Log.i("mFreq", String.format("%d", mFreq2));
            }
            if (is2tone && sPort2 != null) {
                SDRFUtils.sendCommand(BigInteger.valueOf(0), 31, 1, sPort2);
                sendAmp2(mAmpinput, 32, sPort2);
                SDRFUtils.sendCommand(mFreq2, nb, 8, sPort2);
                rfstate2();
            }
        }
    }

    public void sendVa(EditText input, Integer nb, UsbSerialPort port)
    {
        if ((mVa = BigInteger.valueOf(SDRFUtils.ParseFreq(input).longValue())).intValue() <= -101D)
        {
            Toast.makeText(getApplicationContext(),
                    "value is to low",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        else {
            if (mVa.intValue() > 7400)
                Toast.makeText(getApplicationContext(),
                        "Value is too high",
                        Toast.LENGTH_SHORT).show();
            else if (mVa.intValue() < 0)
                Toast.makeText(getApplicationContext(),
                        "Value is too Low",
                        Toast.LENGTH_SHORT).show();
            else {
                Log.i("mVa", String.format("%d", mVa));
            }
            SDRFUtils.sendCommand(mVa, nb, 2, port);
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

    //reset for the 2tone
    public void resetBoard2(){
        if (sPort2 != null) {
            byte[] msg = SDRFUtils.intToByteArray(BigInteger.valueOf(0), 5, 2);
            try {
                sPort2.write(msg, 10);
            } catch (IOException e) {
                Log.e(TAG, "Write Error");
            }
        }
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
        if (mSerialIoManager2 != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager2.stop();
            mSerialIoManager2 = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
        if (sPort2 != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager2 = new SerialInputOutputManager(sPort2, mListener2);
            mExecutor2.submit(mSerialIoManager2);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.item, menu);
        return true;
    }

    //choose action for the action bar on the top of the activity
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

    //a hidden field that you can show by enlarging the windows in the view, it show every message comming from the board
    private void updateReceivedData(byte[] data) {
        final String message = "Read " + data.length + " bytes: \n"
                + HexDump.dumpHexString(data) + "\n\n";
        mDumpTextView.append(message);
        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
        mOuput.setText(HexDump.dumpHexString(data));
        mMessage = HexDump.dumpHexString(data);
    }

    //called when you quit the activity or the application
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
        if (sPort2 != null) {
            try {
                sPort2.close();
                Singleton.getInstance().setPort(null);
            } catch (IOException e) {
                // Ignore.
            }
        }
        stopTimer();
        try {
            unregisterReceiver(detachReceiver);
        } catch (Exception e) {
            Log.d(TAG, "unregisterReceiver: " + e.getMessage());
        }
//        finish();
    }
}
