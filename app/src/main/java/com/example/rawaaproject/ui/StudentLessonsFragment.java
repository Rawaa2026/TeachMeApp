package com.example.rawaaproject.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.example.rawaaproject.models.LessonEnrollment;
import com.example.rawaaproject.models.User;
import com.example.rawaaproject.util.LessonScheduleCardStyle;
import com.example.rawaaproject.util.LessonUiHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentLessonsFragment extends Fragment {

    private LessonRepository repo;
    private SessionManager session;
    private UserRepository userRepo;
    private ProgressBar progress;
    private TextView empty;
    private RecyclerView recycler;
    private Spinner subjectSpinner;
    private final List<String> spinnerOptions = new ArrayList<>();
    private String subjectFilter = "";
    private final List<Lesson> items = new ArrayList<>();
    private final Map<String, LessonEnrollment> enrollmentByLesson = new HashMap<>();
    private final Map<String, String> teacherNames = new HashMap<>();
    private final Map<String, String> teacherPhones = new HashMap<>();
    private RowsAdapter adapter;
    private boolean spinnerInit;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_lessons, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new LessonRepository(requireContext());
        session = new SessionManager(requireContext());
        userRepo = new UserRepository(requireContext());
        progress = view.findViewById(R.id.list_progress);
        empty = view.findViewById(R.id.list_empty);
        recycler = view.findViewById(R.id.recycler);
        subjectSpinner = view.findViewById(R.id.lessons_subject_spinner);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RowsAdapter();
        recycler.setAdapter(adapter);
        empty.setText(R.string.lesson_empty);

        spinnerOptions.clear();
        spinnerOptions.add(getString(R.string.filter_all_subjects));
        String[] subs = getResources().getStringArray(R.array.teacher_subjects);
        for (String s : subs) {
            spinnerOptions.add(s);
        }
        ArrayAdapter<String> spinAd = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, spinnerOptions);
        spinAd.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjectSpinner.setAdapter(spinAd);
        subjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                subjectFilter = position <= 0 ? "" : spinnerOptions.get(position);
                if (spinnerInit) {
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        spinnerInit = false;
        reload();
        spinnerInit = true;
    }

    private boolean isSafe() {
        return isAdded() && getView() != null;
    }

    private void reload() {
        User u = session.getCurrentUser();
        if (u == null || u.id == null) {
            if (progress != null) {
                progress.setVisibility(View.GONE);
            }
            if (isSafe()) {
                items.clear();
                enrollmentByLesson.clear();
                teacherPhones.clear();
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
        repo.listEnrollments(er -> {
            if (!isSafe()) {
                return;
            }
            Map<String, LessonEnrollment> mine = new HashMap<>();
            if (er.success && er.data != null) {
                for (LessonEnrollment e : er.data) {
                    if (!u.id.equals(e.studentId)) {
                        continue;
                    }
                    if (LessonEnrollment.STATUS_CANCELLED.equals(e.status)) {
                        continue;
                    }
                    mine.put(e.lessonId, e);
                }
            }
            repo.listUpcomingLessons(subjectFilter.isEmpty() ? null : subjectFilter, lr -> {
                if (!isSafe()) {
                    return;
                }
                progress.setVisibility(View.GONE);
                items.clear();
                enrollmentByLesson.clear();
                teacherNames.clear();
                teacherPhones.clear();
                if (lr.success && lr.data != null) {
                    items.addAll(lr.data);
                } else if (!lr.success) {
                    Toast.makeText(requireContext(),
                            lr.message != null ? lr.message : getString(R.string.loading),
                            Toast.LENGTH_SHORT).show();
                }
                enrollmentByLesson.putAll(mine);
                if (items.isEmpty()) {
                    adapter.notifyDataSetChanged();
                    empty.setVisibility(View.VISIBLE);
                    return;
                }
                final ArrayList<Lesson> lessonsSnapshot = new ArrayList<>(items);
                new Thread(() -> {
                    for (Lesson l : lessonsSnapshot) {
                        if (l.teacherId == null || teacherNames.containsKey(l.teacherId)) {
                            continue;
                        }
                        User t = userRepo.getUserById(l.teacherId);
                        teacherNames.put(l.teacherId, t != null ? t.getDisplayName() : l.teacherId);
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
            });
        });
    }

    private void requestJoin(Lesson lesson) {
        User u = session.getCurrentUser();
        if (u == null || lesson.id == null) {
            return;
        }
        repo.requestJoin(lesson.id, u.id, r -> {
            if (!isAdded() || getContext() == null) {
                return;
            }
            Toast.makeText(getContext(),
                    r.success ? getString(R.string.request_join_sent)
                            : (r.message != null ? r.message : getString(R.string.publish_lesson_failed)),
                    Toast.LENGTH_LONG).show();
            reload();
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
            reload();
        });
    }

    private class RowsAdapter extends RecyclerView.Adapter<RowsAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lesson_row, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Lesson l = items.get(position);
            LessonScheduleCardStyle.apply((MaterialCardView) h.itemView, l);
            h.todayBadge.setVisibility(LessonUiHelper.isStartAtToday(l.startAt) ? View.VISIBLE : View.GONE);

            LessonUiHelper.setPastStrikeThrough(false, h.subject, h.title, h.time, h.extra, h.phone, h.price);
            h.subject.setText(l.subject);
            h.title.setText(l.lessonTitle);
            h.time.setText(LessonUiHelper.formatStartAt(l.startAt));
            String tname = teacherNames.get(l.teacherId);
            h.extra.setVisibility(View.VISIBLE);
            h.extra.setText(getString(R.string.lesson_teacher_label, tname != null ? tname : "…"));

            String tph = l.teacherId != null ? teacherPhones.get(l.teacherId) : null;
            if (tph != null && !tph.isEmpty()) {
                h.phone.setVisibility(View.VISIBLE);
                h.phone.setText(getString(R.string.lesson_phone_label, tph));
            } else {
                h.phone.setVisibility(View.GONE);
            }
            if (l.lessonPrice != null && !l.lessonPrice.trim().isEmpty()) {
                h.price.setVisibility(View.VISIBLE);
                h.price.setText(getString(R.string.lesson_price_label, l.lessonPrice.trim()));
            } else {
                h.price.setVisibility(View.GONE);
            }

            boolean past = LessonUiHelper.isStartInPast(l.startAt);
            LessonUiHelper.setPastStrikeThrough(past, h.subject, h.title, h.time, h.extra, h.phone, h.price);

            User me = session.getCurrentUser();
            LessonEnrollment en = enrollmentByLesson.get(l.id);
            String st = en != null ? en.status : null;
            h.actionsRow.setVisibility(View.VISIBLE);
            h.cancel.setVisibility(View.GONE);
            h.cancel.setOnClickListener(null);
            h.action.setVisibility(View.VISIBLE);

            if (me != null && me.id != null && me.id.equals(l.teacherId)) {
                h.action.setVisibility(View.GONE);
                h.cancel.setVisibility(View.GONE);
                h.actionsRow.setVisibility(View.GONE);
                return;
            }

            boolean canCancelTime = LessonRepository.canCancelBeforeLessonStart(l.startAt);
            boolean canShowCancel = !past && canCancelTime
                    && (LessonEnrollment.STATUS_PENDING.equals(st)
                    || LessonEnrollment.STATUS_APPROVED.equals(st));
            if (canShowCancel) {
                h.cancel.setVisibility(View.VISIBLE);
                h.cancel.setEnabled(true);
                h.cancel.setText(LessonEnrollment.STATUS_PENDING.equals(st)
                        ? R.string.lesson_cancel_pending_request
                        : R.string.lesson_cancel_enrollment);
                h.cancel.setOnClickListener(v -> cancelEnrollment(l));
            }

            if (past) {
                h.action.setOnClickListener(null);
                h.action.setEnabled(false);
                h.action.setText(R.string.lesson_past_ended);
                h.actionsRow.setVisibility(h.cancel.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
                return;
            }

            if (LessonEnrollment.STATUS_PENDING.equals(st)) {
                h.action.setOnClickListener(null);
                h.action.setEnabled(false);
                h.action.setText(R.string.lesson_pending_review);
            } else if (LessonEnrollment.STATUS_APPROVED.equals(st)) {
                h.action.setOnClickListener(null);
                h.action.setEnabled(false);
                h.action.setText(R.string.lesson_enrolled);
            } else if (LessonEnrollment.STATUS_REJECTED.equals(st)) {
                h.action.setEnabled(true);
                h.action.setText(R.string.lesson_request_join);
                h.action.setOnClickListener(v -> requestJoin(l));
            } else {
                h.action.setEnabled(true);
                h.action.setText(R.string.lesson_request_join);
                h.action.setOnClickListener(v -> requestJoin(l));
            }

            if (h.cancel.getVisibility() != View.VISIBLE && h.action.getVisibility() != View.VISIBLE) {
                h.actionsRow.setVisibility(View.GONE);
            } else {
                h.actionsRow.setVisibility(View.VISIBLE);
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
