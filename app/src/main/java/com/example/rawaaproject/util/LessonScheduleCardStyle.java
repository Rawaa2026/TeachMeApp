package com.example.rawaaproject.util;

import androidx.core.content.ContextCompat;

import com.example.rawaaproject.R;
import com.example.rawaaproject.models.Lesson;
import com.google.android.material.card.MaterialCardView;

/**
 * لون بطاقة الدرس: تمييز دروس «اليوم» بلون وحدّ إطار أوضح.
 */
public final class LessonScheduleCardStyle {

    private LessonScheduleCardStyle() {
    }

    public static void apply(MaterialCardView card, Lesson lesson) {
        if (card == null) {
            return;
        }
        float d = card.getResources().getDisplayMetrics().density;
        boolean today = LessonUiHelper.isStartAtToday(lesson != null ? lesson.startAt : null);
        if (today) {
            card.setCardBackgroundColor(ContextCompat.getColor(card.getContext(), R.color.home_schedule_today_card_fill));
            card.setStrokeWidth(Math.max(1, Math.round(2f * d)));
            card.setStrokeColor(ContextCompat.getColor(card.getContext(), R.color.home_schedule_today_stroke));
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(card.getContext(), R.color.home_schedule_card_fill));
            card.setStrokeWidth(Math.max(1, Math.round(1f * d)));
            card.setStrokeColor(ContextCompat.getColor(card.getContext(), R.color.home_schedule_card_stroke));
        }
    }
}
