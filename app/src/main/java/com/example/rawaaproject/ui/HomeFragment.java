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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * الصفحة الرئيسية مع جدول الدروس المعتمدة (معلم أو طالب) — عرض كبطاقات مقروءة.
 */
public class HomeFragment extends Fragment {

    private TextView scheduleTitle;
    private ProgressBar progress;
    private TextView empty;
    private RecyclerView scheduleRecycler;
    private View scheduleCard;
    private LessonRepository lessonRepo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        lessonRepo = new LessonRepository(requireContext());
        scheduleTitle = view.findViewById(R.id.home_schedule_title);
        progress = view.findViewById(R.id.home_schedule_progress);
        empty = view.findViewById(R.id.home_schedule_empty);
        scheduleRecycler = view.findViewById(R.id.home_schedule_recycler);
        scheduleCard = view.findViewById(R.id.home_schedule_card);
        scheduleRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadScheduleTable();
    }

    private boolean isSafe() {
        return isAdded() && getView() != null && scheduleRecycler != null;
    }

    private void loadScheduleTable() {
        SessionManager sm = new SessionManager(requireContext());
        User u = sm.getCurrentUser();
        if (u == null || u.id == null) {
            setScheduleVisible(false);
            return;
        }
        setScheduleVisible(true);
        progress.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        scheduleCard.setVisibility(View.VISIBLE);
        scheduleRecycler.setAdapter(null);

        if (u.isTeacher()) {
            lessonRepo.loadTeacherSchedule(u.id, result -> {
                if (!isSafe()) {
                    return;
                }
                progress.setVisibility(View.GONE);
                ArrayList<Lesson> list = result.data != null ? result.data : new ArrayList<>();
                if (list.isEmpty()) {
                    showEmpty();
                    return;
                }
                String selfPhone = u.phoneNumber != null && !u.phoneNumber.isEmpty() ? u.phoneNumber : null;
                bindScheduleCards(new ArrayList<>(list), false, null, null, selfPhone, u);
            });
        } else {
            lessonRepo.loadStudentSchedule(u.id, result -> {
                if (!isSafe()) {
                    return;
                }
                progress.setVisibility(View.GONE);
                ArrayList<Lesson> list = result.data != null ? result.data : new ArrayList<>();
                if (list.isEmpty()) {
                    showEmpty();
                    return;
                }
                final ArrayList<Lesson> snapshot = new ArrayList<>(list);
                new Thread(() -> {
                    UserRepository ur = new UserRepository(requireContext());
                    Map<String, String> teacherNames = new HashMap<>();
                    Map<String, String> teacherPhones = new HashMap<>();
                    for (Lesson l : snapshot) {
                        if (l.teacherId == null || teacherNames.containsKey(l.teacherId)) {
                            continue;
                        }
                        User t = ur.getUserById(l.teacherId);
                        teacherNames.put(l.teacherId, t != null ? t.getDisplayName() : "—");
                        String ph = t != null && t.phoneNumber != null && !t.phoneNumber.isEmpty()
                                ? t.phoneNumber : "";
                        teacherPhones.put(l.teacherId, ph.isEmpty() ? "—" : ph);
                    }
                    View root = getView();
                    if (root != null) {
                        root.post(() -> {
                            if (!isAdded()) {
                                return;
                            }
                            bindScheduleCards(snapshot, true, teacherNames, teacherPhones, null, u);
                        });
                    }
                }).start();
            });
        }
    }

    private void setScheduleVisible(boolean show) {
        if (scheduleTitle == null) {
            return;
        }
        int v = show ? View.VISIBLE : View.GONE;
        scheduleTitle.setVisibility(v);
        progress.setVisibility(View.GONE);
        empty.setVisibility(View.GONE);
        scheduleCard.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show && scheduleRecycler != null) {
            scheduleRecycler.setAdapter(null);
        }
    }

    private void showEmpty() {
        empty.setVisibility(View.VISIBLE);
        scheduleCard.setVisibility(View.GONE);
        if (scheduleRecycler != null) {
            scheduleRecycler.setAdapter(null);
        }
    }

    private void bindScheduleCards(ArrayList<Lesson> lessons, boolean isStudent,
                                   Map<String, String> teacherNames, Map<String, String> teacherPhones,
                                   String teacherSelfPhone, User currentUser) {
        if (!isSafe()) {
            return;
        }
        empty.setVisibility(View.GONE);
        scheduleCard.setVisibility(View.VISIBLE);

        HomeScheduleAdapter adapter = new HomeScheduleAdapter(
                lessons,
                isStudent,
                teacherNames,
                teacherPhones,
                teacherSelfPhone,
                currentUser,
                (lesson, isStudentRole) -> {
                    if (currentUser == null || currentUser.id == null || lesson.id == null) {
                        return;
                    }
                    if (isStudentRole) {
                        lessonRepo.cancelStudentEnrollment(lesson.id, currentUser.id, r -> {
                            if (!isAdded() || getContext() == null) {
                                return;
                            }
                            Toast.makeText(getContext(),
                                    r.success ? getString(R.string.lesson_cancel_done)
                                            : (r.message != null ? r.message
                                            : getString(R.string.publish_lesson_failed)),
                                    Toast.LENGTH_LONG).show();
                            loadScheduleTable();
                        });
                    } else {
                        lessonRepo.cancelLessonByTeacher(lesson.id, currentUser.id, r -> {
                            if (!isAdded() || getContext() == null) {
                                return;
                            }
                            Toast.makeText(getContext(),
                                    r.success ? getString(R.string.lesson_cancel_done)
                                            : (r.message != null ? r.message
                                            : getString(R.string.publish_lesson_failed)),
                                    Toast.LENGTH_LONG).show();
                            loadScheduleTable();
                        });
                    }
                });
        scheduleRecycler.setAdapter(adapter);
    }
}
