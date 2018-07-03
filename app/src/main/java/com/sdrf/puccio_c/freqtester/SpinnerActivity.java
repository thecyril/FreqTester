package com.sdrf.puccio_c.freqtester;

import android.icu.math.BigDecimal;
import android.view.View;
import android.widget.AdapterView;

/**
 * Created by puccio_c on 4/12/18.
 */

public class SpinnerActivity extends SerialConsoleActivity implements AdapterView.OnItemSelectedListener {

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
//        parent.getItemAtPosition(pos);
        switch(parent.getId()) {
            case R.id.Unit: {
                switch (pos) {
                    case 0:
                        mFreqmult = 1000000000D;
                        break;
                    case 1:
                        mFreqmult = 1000000D;
                        break;
                    case 2:
                        mFreqmult = 1000D;
                        break;
                    case 3:
                        mFreqmult = 1D;
                        break;
                }
                break;
            }
            case R.id.Unit2: {
                switch (pos) {
                    case 0:
                        mNbDiv = 1;
                        break;
                    case 1:
                        mNbDiv = 1000;
                        break;
                    case 2:
                        mNbDiv = 1000000;
                        break;
                    case 3:
                        mNbDiv = 1000000000;
                        break;
                }
                break;
            }
            case R.id.Dbm: {
                switch (pos) {
                    case 0:
                        mDbm = BigDecimal.valueOf(1);
                        break;
                    case 1:
                        mDbm = BigDecimal.valueOf(0.1);
                        break;
                    case 2:
                        mDbm = BigDecimal.valueOf(10);
                        break;
                }
                break;
            }
        }
            //            Toast.makeText(parent.getContext(),
//                    "OnItemSelectedListener : " + String.valueOf(super.mFreqmult),
//                     Toast.LENGTH_SHORT).show();
//        Spinner spinner = (Spinner) findViewById(R.id.Unit);
//        spinner.setOnItemSelectedListener(this);

    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }
}
