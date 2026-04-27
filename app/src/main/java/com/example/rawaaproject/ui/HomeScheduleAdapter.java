package com.example.rawaaproject.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rawaaproject.R;
import com.example.rawaaproject.data.LessonRepository;
import com.example.rawaaproject.models.Lesson;
import com.example.rawaaproject.models.User;
import com.example.rawaaproject.util.LessonScheduleCardStyle;
import com.example.rawaaproject.util.LessonUiHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * عرض جدول الرئيسية كبطاقات مقروءة (بدلاً من جدول مضغوط).
 */
public class HomeScheduleAdapter extends RecyclerView.Adapter<HomeScheduleAdapter.VH> {

    public interface CancelListener {
        void onCancel(Lesson lesson, boolean isStudentRole);
    }

    private final List<Lesson> lessons;
    private final boolean isStudent;
    private final Map<String, String> teacherNames;
    private final Map<String, String> teacherPhones;
    private final String teacherSelfPhone;
    private final User currentUser;
    private final CancelListener cancelListener;

    public HomeScheduleAdapter(ArrayList<Lesson> lessons,
                               boolean isStudent,
                               Map<String, String> teacherNames,
                               Map<String, String> teacherPhones,
                               String teacherSelfPhone,
                               User currentUser,
                               CancelListener cancelListener) {
        this.lessons = lessons != null ? new ArrayList<>(lessons) : new ArrayList<>();
        this.isStudent = isStudent;
        this.teacherNames = teacherNames != null ? teacherNames : Collections.emptyMap();
        this.teacherPhones = teacherPhones != null ? teacherPhones : Collections.emptyMap();
        this.teacherSelfPhone = teacherSelfPhone;
        this.currentUser = currentUser;
        this.cancelListener = cancelListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_schedule_lesson, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Lesson l = lessons.get(position);
        LessonScheduleCardStyle.apply(h.card, l);
        boolean today = LessonUiHelper.isStartAtToday(l.startAt);
        h.todayBadge.setVisibility(today ? View.VISIBLE : View.GONE);

        String subj = l.subject != null ? l.subject : "—";
        String topic = l.lessonTitle != null ? l.lessonTitle : "—";
        h.subject.setText(subj);
        h.topic.setText(topic);
        h.time.setText(LessonUiHelper.formatStartAt(l.startAt));

        String priceTxt = l.lessonPrice != null && !l.lessonPrice.trim().isEmpty()
                ? l.lessonPrice.trim() : "—";
        h.price.setText(priceTxt);

        String phoneTxt;
        if (isStudent) {
            phoneTxt = "—";
            if (l.teacherId != null && teacherPhones.containsKey(l.teacherId)) {
                phoneTxt = teacherPhones.get(l.teacherId);
            }
        } else {
            phoneTxt = teacherSelfPhone != null ? teacherSelfPhone : "—";
        }
        h.phone.setText(phoneTxt);

        if (isStudent) {
            h.teacherRow.setVisibility(View.VISIBLE);
            String tn = l.teacherId != null ? teacherNames.get(l.teacherId) : null;
            h.teacher.setText(tn != null ? tn : "—");
        } else {
            h.teacherRow.setVisibility(View.GONE);
        }

        boolean past = LessonUiHelper.isStartInPast(l.startAt);
        LessonUiHelper.setPastStrikeThrough(past, h.subject, h.topic, h.time, h.price, h.phone);
        if (h.teacherRow.getVisibility() == View.VISIBLE) {
            LessonUiHelper.setPastStrikeThrough(past, h.teacher);
        }
        h.pastBadge.setVisibility(past ? View.VISIBLE : View.GONE);

        boolean cancelled = LessonRepository.isLessonCancelled(l);
        boolean can = !past && !cancelled && LessonRepository.canCancelBeforeLessonStart(l.startAt)
                && currentUser != null && currentUser.id != null;
        if (can) {
            h.cancel.setVisibility(View.VISIBLE);
            h.cancel.setEnabled(true);
            h.cancel.setOnClickListener(v -> cancelListener.onCancel(l, isStudent));
        } else {
            h.cancel.setVisibility(View.GONE);
            h.cancel.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView subject;
        final TextView topic;
        final TextView time;
        final TextView price;
        final TextView phone;
        final TextView teacher;
        final View teacherRow;
        final TextView todayBadge;
        final TextView pastBadge;
        final MaterialButton cancel;

        VH(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            subject = itemView.findViewById(R.id.home_schedule_subject);
            topic = itemView.findViewById(R.id.home_schedule_topic);
            time = itemView.findViewById(R.id.home_schedule_time);
            price = itemView.findViewById(R.id.home_schedule_price);
            phone = itemView.findViewById(R.id.home_schedule_phone);
            teacher = itemView.findViewById(R.id.home_schedule_teacher);
            teacherRow = itemView.findViewById(R.id.home_schedule_row_teacher);
            todayBadge = itemView.findViewById(R.id.home_schedule_today_badge);
            pastBadge = itemView.findViewById(R.id.home_schedule_past_badge);
            cancel = itemView.findViewById(R.id.home_schedule_cancel);
        }
    }
}
