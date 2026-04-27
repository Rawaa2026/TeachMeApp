package com.example.rawaaproject.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rawaaproject.PublishLessonActivity;
import com.example.rawaaproject.R;
import com.example.rawaaproject.data.LessonRepository;
import com.example.rawaaproject.data.SessionManager;
import com.example.rawaaproject.models.Lesson;
import com.example.rawaaproject.models.User;
import com.example.rawaaproject.util.LessonScheduleCardStyle;
import com.example.rawaaproject.util.LessonUiHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TeacherLessonsFragment extends Fragment {

    private final ActivityResultLauncher<Intent> publishLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (isAdded()) {
                    reload();
                }
            });

    private LessonRepository repo;
    private SessionManager session;
    private ProgressBar progress;
    private TextView empty;
    private RecyclerView recycler;
    private final List<Lesson> items = new ArrayList<>();
    private RowsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_lessons, container, false);
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
        adapter = new RowsAdapter();
        recycler.setAdapter(adapter);
        empty.setText(R.string.lesson_empty);

        FloatingActionButton fab = view.findViewById(R.id.fab_publish_lesson);
        fab.setOnClickListener(v -> publishLauncher.launch(new Intent(requireContext(), PublishLessonActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
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
        repo.listMyLessonsAsTeacher(u.id, result -> {
            if (!isSafe()) {
                return;
            }
            progress.setVisibility(View.GONE);
            items.clear();
            if (result.success && result.data != null) {
                items.addAll(result.data);
            } else if (!result.success) {
                Toast.makeText(requireContext(),
                        result.message != null ? result.message : getString(R.string.loading),
                        Toast.LENGTH_SHORT).show();
            }
            adapter.notifyDataSetChanged();
            empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        });
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

            User me = session.getCurrentUser();
            boolean cancelled = LessonRepository.isLessonCancelled(l);
            String subj = l.subject != null ? l.subject : "";
            if (cancelled) {
                h.subject.setText(getString(R.string.lesson_cancelled_badge) + " · " + subj);
            } else {
                h.subject.setText(subj);
            }
            h.title.setText(l.lessonTitle);
            h.time.setText(LessonUiHelper.formatStartAt(l.startAt));
            h.extra.setVisibility(View.GONE);

            String myPhone = me != null && me.phoneNumber != null && !me.phoneNumber.isEmpty()
                    ? me.phoneNumber : null;
            if (myPhone != null) {
                h.phone.setVisibility(View.VISIBLE);
                h.phone.setText(getString(R.string.lesson_phone_label, myPhone));
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
            LessonUiHelper.setPastStrikeThrough(past || cancelled, h.subject, h.title, h.time, h.phone, h.price);

            h.action.setVisibility(View.GONE);
            h.actionsRow.setVisibility(View.VISIBLE);
            boolean canCancel = !cancelled && !past && LessonRepository.canCancelBeforeLessonStart(l.startAt);
            if (canCancel) {
                h.cancel.setVisibility(View.VISIBLE);
                h.cancel.setEnabled(true);
                h.cancel.setText(R.string.lesson_cancel_lesson);
                h.cancel.setOnClickListener(v -> cancelLesson(l));
            } else {
                h.cancel.setVisibility(View.GONE);
                h.cancel.setOnClickListener(null);
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
