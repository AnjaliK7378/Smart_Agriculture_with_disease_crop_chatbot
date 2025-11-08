package com.example.myapplication;

import android.content.Context;
import android.widget.Spinner;

public class LanguageUtils {

    public static String getSelectedLanguageCode(Context context, Spinner spinnerLanguage) {
        String selected = spinnerLanguage.getSelectedItem().toString();
        if (selected.contains("(")) {
            return selected.substring(selected.indexOf('(') + 1, selected.indexOf(')'));
        }
        return "en";
    }
}