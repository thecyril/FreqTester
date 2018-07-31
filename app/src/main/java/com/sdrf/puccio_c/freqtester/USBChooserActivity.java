package com.sdrf.puccio_c.freqtester;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TwoLineListItem;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.util.ArrayList;
import java.util.List;

public class USBChooserActivity extends AppCompatActivity {

    private final String TAG = USBChooserActivity.class.getSimpleName();

    private ListView mListView;
    private UsbDevice mDevice;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;

    private List<UsbSerialPort> mEntries = new ArrayList<>();
    private ArrayAdapter<UsbSerialPort> mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usbchooser);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mListView = findViewById(R.id.deviceList);
        mCollapsingToolbarLayout = findViewById(R.id.toolbar_layout);

        mCollapsingToolbarLayout.setTitle("Second device:");

        ProbeTable customTable = new ProbeTable();
        customTable.addProduct(0x04d8, 0x000a, CdcAcmSerialDriver.class);
        UsbSerialProber prober = new UsbSerialProber(customTable);
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        final List<UsbSerialDriver> drivers = prober.findAllDrivers(manager);

        for (final UsbSerialDriver driver : drivers) {
            final List<UsbSerialPort> ports = driver.getPorts();
            Log.d(TAG, String.format("+ %s: %s port%s",
                    driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));
            mEntries.addAll(ports);
        }

        mAdapter = new ArrayAdapter<UsbSerialPort>(this,
                android.R.layout.simple_expandable_list_item_2, mEntries) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final TwoLineListItem row;
                if (convertView == null){
                    final LayoutInflater inflater =
                            (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    row = (TwoLineListItem) inflater.inflate(android.R.layout.simple_list_item_2, null);
                } else {
                    row = (TwoLineListItem) convertView;
                }

                final UsbSerialPort port = mEntries.get(position);
                final UsbSerialDriver driver = port.getDriver();
                mDevice = driver.getDevice();

                final String title = String.format("Board: %s",
                        mDevice.getProductName());
                row.getText1().setText(title);

                final String subtitle = driver.getClass().getSimpleName();
                row.getText2().setText(subtitle);

                return row;
                }
            };

        mAdapter.notifyDataSetChanged();

        mListView.setAdapter(mAdapter);

        ViewCompat.setNestedScrollingEnabled(mListView, true);

        mListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Pressed item " + position);
                if (position >= mEntries.size()) {
                    Log.w(TAG, "Illegal position.");
                    return;
                }

                Singleton.getInstance().setPort(mEntries.get(position));
                finish();
            }
        });
    }
}
