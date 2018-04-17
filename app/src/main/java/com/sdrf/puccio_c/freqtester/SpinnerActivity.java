package com.sdrf.puccio_c.freqtester;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import static com.sdrf.puccio_c.freqtester.SerialConsoleActivity.*;

/**
 * Created by puccio_c on 4/12/18.
 */

public class SpinnerActivity extends SerialConsoleActivity implements AdapterView.OnItemSelectedListener {

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
//        parent.getItemAtPosition(pos);

            switch (pos) {
                case 0:
                    mFreqmult = 1000000000D;
                    break;
                case 1:
                    mFreqmult = 1000000D;
                    break;
                case 2:
                    mFreqmult = 1D;
                    break;
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
