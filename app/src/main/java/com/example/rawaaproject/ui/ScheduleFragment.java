package com.example.rawaaproject.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rawaaproject.R;
import com.example.rawaaproject.data.LessonRepository;
import com.example.rawaaproject.data.SessionManager;
import com.example.rawaaproject.data.UserRepository;
import com.example.rawaaproject.models.Lesson;
import com.example.rawaaproject.models.User;
import com.example.rawaaproject.util.LessonScheduleCardStyle;
import com.example.rawaaproject.util.LessonUiHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleFragment extends Fragment {

    private LessonRepository repo;
    private SessionManager session;
    private ProgressBar progress;
    private TextView empty;
    private RecyclerView recycler;
    private final List<Lesson> items = new ArrayList<>();
    private final Map<String, String> teacherNames = new HashMap<>();
    private final Map<String, String> teacherPhones = new HashMap<>();
    private ScheduleAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recycler_state, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new LessonRepository(requireContext());
        session = new SessionManager(requireContext());
        progress = view.findViewById(R.id.list_progress);
        empty = view.findViewById(R.id.list_empty);
        recycler = view.findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ScheduleAdapter();
        recycler.setAdapter(adapter);
        empty.setText(R.string.schedule_empty);
    }

    private boolean isSafe() {
        return isAdded() && getView() != null;
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        User u = session.getCurrentUser();
        if (u == null || u.id == null) {
            if (progress != null) {
                progress.setVisibility(View.GONE);
            }
            if (isSafe()) {
                items.clear();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                if (empty != null) {
                    empty.setVisibility(View.VISIBLE);
                }
            }
            return;
        }
        progress.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        if (u.isTeacher()) {
            repo.loadTeacherSchedule(u.id, result -> {
                if (!isSafe()) {
                    return;
                }
                progress.setVisibility(View.GONE);
                ArrayList<Lesson> list = result.data != null ? result.data : new ArrayList<>();
                if (!result.success && list.isEmpty()) {
                    android.util.Log.w("ScheduleFragment", result.message != null ? result.message : "schedule");
                }
                bindList(list, false);
            });
        } else {
            repo.loadStudentSchedule(u.id, result -> {
                if (!isSafe()) {
                    return;
                }
                progress.setVisibility(View.GONE);
                ArrayList<Lesson> list = result.data != null ? result.data : new ArrayList<>();
                if (!result.success && list.isEmpty()) {
                    android.util.Log.w("ScheduleFragment", result.message != null ? result.message : "schedule");
                }
                bindList(list, true);
            });
        }
    }

    private void bindList(ArrayList<Lesson> lessons, boolean showTeacher) {
        if (!isSafe()) {
            return;
        }
        items.clear();
        items.addAll(lessons != null ? lessons : Collections.emptyList());
        teacherNames.clear();
        teacherPhones.clear();
        if (showTeacher && !items.isEmpty()) {
            final ArrayList<Lesson> lessonsSnapshot = new ArrayList<>(items);
            new Thread(() -> {
                UserRepository ur = new UserRepository(requireContext());
                for (Lesson l : lessonsSnapshot) {
                    if (l.teacherId == null || teacherNames.containsKey(l.teacherId)) {
                        continue;
                    }
                    User t = ur.getUserById(l.teacherId);
                    String name = t != null ? t.getDisplayName() : l.teacherId;
                    teacherNames.put(l.teacherId, name);
                    String ph = t != null && t.phoneNumber != null && !t.phoneNumber.isEmpty()
                            ? t.phoneNumber : "";
                    teacherPhones.put(l.teacherId, ph);
                }
                View root = getView();
                if (root != null) {
                    root.post(() -> {
                        if (!isAdded()) {
                            return;
                        }
                        adapter.notifyDataSetChanged();
                        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                }
            }).start();
        } else {
            adapter.notifyDataSetChanged();
            empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void cancelLesson(Lesson lesson) {
        User u = session.getCurrentUser();
        if (u == null || u.id == null || lesson.id == null) {
            return;
        }
        repo.cancelLessonByTeacher(lesson.id, u.id, r -> {
            if (!isAdded() || getContext() == null) {
                return;
            }
            Toast.makeText(getContext(),
                    r.success ? getString(R.string.lesson_cancel_done)
                            : (r.message != null ? r.message : getString(R.string.publish_lesson_failed)),
                    Toast.LENGTH_LONG).show();
            load();
        });
    }

    private void cancelEnrollment(Lesson lesson) {
        User u = session.getCurrentUser();
        if (u == null || u.id == null || lesson.id == null) {
            return;
        }
        repo.cancelStudentEnrollment(lesson.id, u.id, r -> {
            if (!isAdded() || getContext() == null) {
                return;
            }
            Toast.makeText(getContext(),
                    r.success ? getString(R.string.lesson_cancel_done)
                            : (r.message != null ? r.message : getString(R.string.publish_lesson_failed)),
                    Toast.LENGTH_LONG).show();
            load();
        });
    }

    private class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lesson_row, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Lesson l = items.get(position);
            MaterialCardView rowCard = (MaterialCardView) h.itemView;
            LessonScheduleCardStyle.apply(rowCard, l);
            h.todayBadge.setVisibility(LessonUiHelper.isStartAtToday(l.startAt) ? View.VISIBLE : View.GONE);

            User u = session.getCurrentUser();
            boolean studentView = u != null && u.isStudent();

            LessonUiHelper.setPastStrikeThrough(false, h.subject, h.title, h.time, h.extra, h.phone, h.price);
            h.subject.setText(l.subject);
            h.title.setText(l.lessonTitle);
            h.time.setText(LessonUiHelper.formatStartAt(l.startAt));

            if (l.lessonPrice != null && !l.lessonPrice.trim().isEmpty()) {
                h.price.setVisibility(View.VISIBLE);
                h.price.setText(getString(R.string.lesson_price_label, l.lessonPrice.trim()));
            } else {
                h.price.setVisibility(View.GONE);
            }

            if (studentView && l.teacherId != null) {
                String tph = teacherPhones.get(l.teacherId);
                if (tph != null && !tph.isEmpty()) {
                    h.phone.setVisibility(View.VISIBLE);
                    h.phone.setText(getString(R.string.lesson_phone_label, tph));
                } else {
                    h.phone.setVisibility(View.GONE);
                }
                String nm = teacherNames.get(l.teacherId);
                h.extra.setVisibility(View.VISIBLE);
                h.extra.setText(getString(R.string.lesson_teacher_label, nm != null ? nm : "…"));
            } else {
                h.phone.setVisibility(View.GONE);
                h.extra.setVisibility(View.GONE);
            }

            boolean past = LessonUiHelper.isStartInPast(l.startAt);
            LessonUiHelper.setPastStrikeThrough(past, h.subject, h.title, h.time, h.phone, h.price);
            if (h.extra.getVisibility() == View.VISIBLE) {
                LessonUiHelper.setPastStrikeThrough(past, h.extra);
            }

            h.action.setVisibility(View.GONE);
            h.actionsRow.setVisibility(View.VISIBLE);
            h.cancel.setVisibility(View.GONE);
            h.cancel.setOnClickListener(null);

            boolean canTime = LessonRepository.canCancelBeforeLessonStart(l.startAt);
            if (!past && canTime && u != null && u.id != null) {
                if (studentView) {
                    h.cancel.setVisibility(View.VISIBLE);
                    h.cancel.setText(R.string.lesson_cancel_enrollment);
                    h.cancel.setOnClickListener(v -> cancelEnrollment(l));
                } else if (u.id.equals(l.teacherId)) {
                    h.cancel.setVisibility(View.VISIBLE);
                    h.cancel.setText(R.string.lesson_cancel_lesson);
                    h.cancel.setOnClickListener(v -> cancelLesson(l));
                }
            }
            if (h.cancel.getVisibility() != View.VISIBLE) {
                h.actionsRow.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView todayBadge;
            final TextView subject;
            final TextView title;
            final TextView time;
            final TextView phone;
            final TextView price;
            final TextView extra;
            final View actionsRow;
            final MaterialButton cancel;
            final MaterialButton action;

            Holder(@NonNull View itemView) {
                super(itemView);
                todayBadge = itemView.findViewById(R.id.row_today_badge);
                subject = itemView.findViewById(R.id.row_subject);
                title = itemView.findViewById(R.id.row_title);
                time = itemView.findViewById(R.id.row_time);
                phone = itemView.findViewById(R.id.row_phone);
                price = itemView.findViewById(R.id.row_price);
                extra = itemView.findViewById(R.id.row_extra);
                actionsRow = itemView.findViewById(R.id.row_actions_row);
                cancel = itemView.findViewById(R.id.row_cancel);
                action = itemView.findViewById(R.id.row_action);
            }
        }
    }
}
