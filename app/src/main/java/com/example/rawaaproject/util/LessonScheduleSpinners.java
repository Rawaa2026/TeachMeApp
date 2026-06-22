package com.example.rawaaproject.util;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Spinners لتاريخ ووقت نشر الدرس (نفس أسلوب BirthDateSpinners).
 */
public final class LessonScheduleSpinners {

    private LessonScheduleSpinners() {
    }

    public static void setupHour(Context ctx, Spinner sp, int promptRes) {
        List<String> items = new ArrayList<>();
        items.add(ctx.getString(promptRes));
        for (int hour = 0; hour <= 23; hour++) {
            items.add(String.format(Locale.getDefault(), "%02d", hour));
        }
        bind(sp, items);
    }

    public static void setupMinute(Context ctx, Spinner sp, int promptRes) {
        List<String> items = new ArrayList<>();
        items.add(ctx.getString(promptRes));
        for (int minute = 0; minute <= 59; minute++) {
            items.add(String.format(Locale.getDefault(), "%02d", minute));
        }
        bind(sp, items);
    }

    public static void selectHour(Spinner sp, int hour) {
        selectValue(sp, String.format(Locale.getDefault(), "%02d", hour));
    }

    public static void selectMinute(Spinner sp, int minute) {
        selectValue(sp, String.format(Locale.getDefault(), "%02d", minute));
    }

    public static Integer selectedHour(Spinner sp) {
        return selectedNumber(sp);
    }

    public static Integer selectedMinute(Spinner sp) {
        return selectedNumber(sp);
    }

    public static void enableDropdownTouch(Spinner spinner) {
        spinner.setOnTouchListener((View v, MotionEvent event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View parent = (View) v.getParent();
                while (parent != null) {
                    if (parent instanceof ViewGroup) {
                        ((ViewGroup) parent).requestDisallowInterceptTouchEvent(true);
                    }
                    parent = parent.getParent() instanceof View ? (View) parent.getParent() : null;
                }
            }
            return false;
        });
    }

    private static void bind(Spinner sp, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                sp.getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
    }

    private static void selectValue(Spinner sp, String value) {
        if (sp.getAdapter() == null) {
            return;
        }
        for (int i = 0; i < sp.getAdapter().getCount(); i++) {
            Object item = sp.getAdapter().getItem(i);
            if (item != null && value.equals(item.toString())) {
                sp.setSelection(i);
                return;
            }
        }
    }

    private static Integer selectedNumber(Spinner sp) {
        if (sp.getSelectedItemPosition() <= 0) {
            return null;
        }
        try {
            return Integer.parseInt(sp.getSelectedItem().toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
