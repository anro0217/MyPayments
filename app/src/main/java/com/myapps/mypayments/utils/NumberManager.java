package com.myapps.mypayments.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberManager {

    public static String formatNumber(double number) {
        Locale currentLocale = Locale.getDefault();
        NumberFormat numberFormat = NumberFormat.getNumberInstance(currentLocale);
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(number);
    }
}
