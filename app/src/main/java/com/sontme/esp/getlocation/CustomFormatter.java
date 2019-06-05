package com.sontme.esp.getlocation;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;

public class CustomFormatter implements IValueFormatter {

    private DecimalFormat mFormat;

    public CustomFormatter() {
        mFormat = new DecimalFormat("###,###,##0");
    }

    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {

        if (value > 0) {
            return mFormat.format(value);
        } else {
            return "";
        }
    }
}
