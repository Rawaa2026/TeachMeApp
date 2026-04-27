package com.example.rawaaproject.util;

import android.graphics.Paint;
import android.widget.TextView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class LessonUiHelper {
    private LessonUiHelper() {
    }

    /** هل يقع وقت بدء الدرس في «اليوم» حسب التوقيت المحلي؟ */
    public static boolean isStartAtToday(String startAtIso) {
        if (startAtIso == null || startAtIso.isEmpty()) {
            return false;
        }
        try {
            ZoneId z = ZoneId.systemDefault();
            LocalDate lessonDay = Instant.parse(startAtIso).atZone(z).toLocalDate();
            return lessonDay.equals(LocalDate.now(z));
        } catch (Exception e) {
            return false;
        }
    }

    /** وقت بدء الدرس قبل الآن (مضى). */
    public static boolean isStartInPast(String startAtIso) {
        if (startAtIso == null || startAtIso.isEmpty()) {
            return false;
        }
        try {
            return Instant.parse(startAtIso).isBefore(Instant.now());
        } catch (Exception e) {
            return false;
        }
    }

    /** مشطوب للدروس المنتهية؛ إزالة التشطيب عند التحديث. */
    public static void setPastStrikeThrough(boolean past, TextView... textViews) {
        for (TextView tv : textViews) {
            if (tv == null) {
                continue;
            }
            int flags = tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG);
            if (past) {
                flags |= Paint.STRIKE_THRU_TEXT_FLAG;
            }
            tv.setPaintFlags(flags);
            tv.setAlpha(past ? 0.65f : 1f);
        }
    }

    public static String formatStartAt(String iso) {
        if (iso == null || iso.isEmpty()) {
            return "";
        }
        try {
            Instant i = Instant.parse(iso);
            ZonedDateTime z = i.atZone(ZoneId.systemDefault());
            DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.getDefault());
            return f.format(z);
        } catch (Exception e) {
            return iso;
        }
    }
}
