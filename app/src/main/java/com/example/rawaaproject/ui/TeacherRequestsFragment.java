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
import com.example.rawaaproject.models.LessonEnrollment;
import com.example.rawaaproject.models.User;
import com.example.rawaaproject.util.LessonUiHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherRequestsFragment extends Fragment {

    private LessonRepository repo;
    private UserRepository userRepo;
    private SessionManager session;
    private ProgressBar progress;
    private TextView empty;
    private RecyclerView recycler;
    private final List<LessonRepository.PendingItem> items = new ArrayList<>();
    private final Map<String, String> studentNames = new HashMap<>();
    private ReqAdapter adapter;

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
        userRepo = new UserRepository(requireContext());
        session = new SessionManager(requireContext());
        progress = view.findViewById(R.id.list_progress);
        empty = view.findViewById(R.id.list_empty);
        recycler = view.findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReqAdapter();
        recycler.setAdapter(adapter);
        empty.setText(R.string.requests_empty);
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private boolean isSafe() {
        return isAdded() && getView() != null;
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
        repo.loadPendingForTeacher(u.id, result -> {
            if (!isSafe()) {
                return;
            }
            progress.setVisibility(View.GONE);
            items.clear();
            if (result.data != null) {
                items.addAll(result.data);
            }
            studentNames.clear();
            if (items.isEmpty()) {
                adapter.notifyDataSetChanged();
                empty.setVisibility(View.VISIBLE);
                return;
            }
            final ArrayList<LessonRepository.PendingItem> pendingSnapshot = new ArrayList<>(items);
            new Thread(() -> {
                for (LessonRepository.PendingItem p : pendingSnapshot) {
                    String sid = p.enrollment.studentId;
                    if (sid == null || studentNames.containsKey(sid)) {
                        continue;
                    }
                    User st = userRepo.getUserById(sid);
                    studentNames.put(sid, st != null ? st.getDisplayName() : sid);
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
    }

    private void approve(LessonEnrollment e) {
        User u = session.getCurrentUser();
        if (u == null) {
            return;
        }
        repo.setEnrollmentStatus(e.id, LessonEnrollment.STATUS_APPROVED, u.id, r -> {
            if (!isAdded() || getContext() == null) {
                return;
            }
            Toast.makeText(getContext(),
                    r.success ? getString(R.string.request_approved_toast)
                            : (r.message != null ? r.message : getString(R.string.publish_lesson_failed)),
                    Toast.LENGTH_SHORT).show();
            load();
        });
    }

    private void reject(LessonEnrollment e) {
        User u = session.getCurrentUser();
        if (u == null) {
            return;
        }
        repo.setEnrollmentStatus(e.id, LessonEnrollment.STATUS_REJECTED, u.id, r -> {
            if (!isAdded() || getContext() == null) {
                return;
            }
            Toast.makeText(getContext(),
                    r.success ? getString(R.string.request_rejected_toast)
                            : (r.message != null ? r.message : getString(R.string.publish_lesson_failed)),
                    Toast.LENGTH_SHORT).show();
            load();
        });
    }

    private class ReqAdapter extends RecyclerView.Adapter<ReqAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_request, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            LessonRepository.PendingItem p = items.get(position);
            h.lessonTitle.setText(p.lesson.lessonTitle);
            h.meta.setText(p.lesson.subject + " · " + LessonUiHelper.formatStartAt(p.lesson.startAt));
            String sn = studentNames.get(p.enrollment.studentId);
            h.student.setText(getString(R.string.student_label) + ": " + (sn != null ? sn : "…"));
            h.approve.setOnClickListener(v -> approve(p.enrollment));
            h.reject.setOnClickListener(v -> reject(p.enrollment));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView lessonTitle;
            final TextView meta;
            final TextView student;
            final MaterialButton approve;
            final MaterialButton reject;

            Holder(@NonNull View itemView) {
                super(itemView);
                lessonTitle = itemView.findViewById(R.id.pend_lesson_title);
                meta = itemView.findViewById(R.id.pend_meta);
                student = itemView.findViewById(R.id.pend_student);
                approve = itemView.findViewById(R.id.pend_approve);
                reject = itemView.findViewById(R.id.pend_reject);
            }
        }
    }
}
