package com.example.rawaaproject.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;

/** تحققات إضافية لملف الطالب (العمر، إلخ). */
public final class ProfileValidation {

    private ProfileValidation() {
    }

    /**
     * تاريخ ميلاد بصيغة YYYY-MM-DD: هل عمر الشخص الآن على الأقل minYears سنة؟
     */
    public static boolean isAgeAtLeastYears(String yyyyMmDd, int minYears) {
        if (yyyyMmDd == null || yyyyMmDd.trim().isEmpty() || minYears < 0) {
            return false;
        }
        try {
            LocalDate birth = LocalDate.parse(yyyyMmDd.trim());
            int years = Period.between(birth, LocalDate.now()).getYears();
            return years >= minYears;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
