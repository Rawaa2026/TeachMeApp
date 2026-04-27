package com.example.rawaaproject.util;

import com.example.rawaaproject.data.LessonRepository;
import com.example.rawaaproject.models.Lesson;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/** تجميع تكلفة الدروس من حقل lessonPrice (رقم نصي). */
public final class LessonCostHelper {

    private LessonCostHelper() {
    }

    public static double parseLessonPrice(String lessonPrice) {
        if (lessonPrice == null) {
            return 0;
        }
        String s = lessonPrice.trim().replace(',', '.').replace("٫", ".");
        if (s.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * مجموع تكلفة دروس اليوم (حسب التاريخ المحلي لـ startAt) والمجموع الكلي.
     * يُتجاهل الدرس الملغى.
     */
    public static double[] sumTodayAndTotal(List<Lesson> lessons, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        double todaySum = 0;
        double totalSum = 0;
        if (lessons == null) {
            return new double[]{0, 0};
        }
        for (Lesson l : lessons) {
            if (l == null || LessonRepository.isLessonCancelled(l)) {
                continue;
            }
            double v = parseLessonPrice(l.lessonPrice);
            totalSum += v;
            if (l.startAt == null || l.startAt.isEmpty()) {
                continue;
            }
            try {
                LocalDate d = Instant.parse(l.startAt).atZone(zone).toLocalDate();
                if (d.equals(today)) {
                    todaySum += v;
                }
            } catch (Exception ignored) {
            }
        }
        return new double[]{todaySum, totalSum};
    }

    public static String formatAmount(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return "0";
        }
        if (Math.abs(v - Math.rint(v)) < 1e-6) {
            return String.format(Locale.getDefault(), "%d", (long) Math.rint(v));
        }
        return String.format(Locale.getDefault(), "%.2f", v);
    }
}
