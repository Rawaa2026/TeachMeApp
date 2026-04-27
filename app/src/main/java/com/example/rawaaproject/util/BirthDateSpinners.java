package com.example.rawaaproject.util;

import android.content.Context;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * تهيئة Spinners لتاريخ ميلاد الطالب (يوم + شهر + سنة) وحفظه كـ YYYY-MM-DD محلياً فقط.
 */
public final class BirthDateSpinners {

    private BirthDateSpinners() {
    }

    public static int maxDayFor(int year, int month1to12) {
        if (month1to12 < 1 || month1to12 > 12) {
            return 31;
        }
        Calendar c = Calendar.getInstance();
        c.setLenient(false);
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month1to12 - 1);
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static void setupYear(Context ctx, Spinner sp, int yearsArrayRes, int promptRes) {
        String[] years = ctx.getResources().getStringArray(yearsArrayRes);
        List<String> items = new ArrayList<>();
        items.add(ctx.getString(promptRes));
        for (String y : years) {
            items.add(y);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                ctx, android.R.layout.simple_spinner_dropdown_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
    }

    public static void setupMonth(Context ctx, Spinner sp, int promptRes) {
        List<String> items = new ArrayList<>();
        items.add(ctx.getString(promptRes));
        for (int m = 1; m <= 12; m++) {
            items.add(String.valueOf(m));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                ctx, android.R.layout.simple_spinner_dropdown_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
    }

    public static void setupDays(Context ctx, Spinner sp, int promptRes, int maxDayInclusive) {
        int max = Math.max(1, Math.min(31, maxDayInclusive));
        List<String> items = new ArrayList<>();
        items.add(ctx.getString(promptRes));
        String previousDay = null;
        if (sp.getAdapter() != null && sp.getSelectedItemPosition() > 0) {
            Object cur = sp.getSelectedItem();
            if (cur != null) {
                previousDay = cur.toString();
            }
        }
        for (int d = 1; d <= max; d++) {
            items.add(String.valueOf(d));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                ctx, android.R.layout.simple_spinner_dropdown_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
        if (previousDay != null) {
            for (int i = 1; i < items.size(); i++) {
                if (previousDay.equals(items.get(i))) {
                    sp.setSelection(i);
                    return;
                }
            }
        }
        sp.setSelection(0);
    }

    public static void refreshDaySpinner(Context ctx, Spinner yearSp, Spinner monthSp, Spinner daySp, int dayPromptRes) {
        int max = 31;
        if (yearSp.getSelectedItemPosition() > 0 && monthSp.getSelectedItemPosition() > 0) {
            try {
                int y = Integer.parseInt(yearSp.getSelectedItem().toString());
                int m = Integer.parseInt(monthSp.getSelectedItem().toString());
                max = maxDayFor(y, m);
            } catch (NumberFormatException ignored) {
            }
        }
        setupDays(ctx, daySp, dayPromptRes, max);
    }

    public static void wireDayUpdates(Context ctx, Spinner yearSp, Spinner monthSp, Spinner daySp, int dayPromptRes) {
        AdapterView.OnItemSelectedListener l = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                refreshDaySpinner(ctx, yearSp, monthSp, daySp, dayPromptRes);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        yearSp.setOnItemSelectedListener(l);
        monthSp.setOnItemSelectedListener(l);
    }

    /**
     * @return YYYY-MM-DD أو null إن لم يُختر التاريخ كاملاً أو كان اليوم غير صالح للشهر
     */
    public static String toIsoDateOrNull(Spinner yearSp, Spinner monthSp, Spinner daySp) {
        if (yearSp.getSelectedItemPosition() <= 0
                || monthSp.getSelectedItemPosition() <= 0
                || daySp.getSelectedItemPosition() <= 0) {
            return null;
        }
        try {
            int y = Integer.parseInt(yearSp.getSelectedItem().toString());
            int m = Integer.parseInt(monthSp.getSelectedItem().toString());
            int d = Integer.parseInt(daySp.getSelectedItem().toString());
            if (d > maxDayFor(y, m)) {
                return null;
            }
            return String.format(Locale.US, "%04d-%02d-%02d", y, m, d);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void selectByValue(Spinner sp, String value) {
        if (sp.getAdapter() == null || value == null) {
            return;
        }
        for (int i = 0; i < sp.getAdapter().getCount(); i++) {
            Object it = sp.getAdapter().getItem(i);
            if (it != null && value.equals(it.toString())) {
                sp.setSelection(i);
                return;
            }
        }
    }

    /**
     * يدعم YYYY-MM-DD أو سنة فقط YYYY (يُفترض 01-01 للعرض).
     */
    public static void selectFromStored(Context ctx, Spinner yearSp, Spinner monthSp, Spinner daySp,
                                        String birthDate, int dayPromptRes) {
        if (birthDate == null || birthDate.trim().isEmpty()) {
            yearSp.setSelection(0);
            monthSp.setSelection(0);
            daySp.setSelection(0);
            return;
        }
        String s = birthDate.trim();
        int y = -1;
        int m = 1;
        int d = 1;
        Matcher iso = Pattern.compile("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$").matcher(s);
        if (iso.matches()) {
            y = Integer.parseInt(iso.group(1));
            m = Integer.parseInt(iso.group(2));
            d = Integer.parseInt(iso.group(3));
        } else if (s.matches("^\\d{4}$")) {
            y = Integer.parseInt(s);
            m = 1;
            d = 1;
        }
        if (y < 0) {
            yearSp.setSelection(0);
            monthSp.setSelection(0);
            daySp.setSelection(0);
            return;
        }
        selectByValue(yearSp, String.valueOf(y));
        selectByValue(monthSp, String.valueOf(m));
        refreshDaySpinner(ctx, yearSp, monthSp, daySp, dayPromptRes);
        int max = maxDayFor(y, m);
        if (d > max) {
            d = max;
        }
        selectByValue(daySp, String.valueOf(d));
    }
}
