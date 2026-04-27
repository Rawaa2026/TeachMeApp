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
import com.example.rawaaproject.data.UserRepository;
import com.example.rawaaproject.models.User;

import java.util.ArrayList;
import java.util.List;

public class StudentTeachersFragment extends Fragment {

    private UserRepository userRepo;
    private ProgressBar progress;
    private TextView empty;
    private RecyclerView recycler;
    private Spinner subjectSpinner;
    private final List<String> spinnerOptions = new ArrayList<>();
    private String subjectFilter = "";
    private final List<User> items = new ArrayList<>();
    private RowsAdapter adapter;
    private boolean spinnerInit;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_teachers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userRepo = new UserRepository(requireContext());
        progress = view.findViewById(R.id.list_progress);
        empty = view.findViewById(R.id.list_empty);
        recycler = view.findViewById(R.id.recycler);
        subjectSpinner = view.findViewById(R.id.teachers_subject_spinner);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RowsAdapter();
        recycler.setAdapter(adapter);
        empty.setText(R.string.teachers_empty);

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
        progress.setVisibility(View.VISIBLE);
        userRepo.listTeachers(subjectFilter.isEmpty() ? null : subjectFilter, (success, message, teachers) -> {
            if (!isSafe()) {
                return;
            }
            progress.setVisibility(View.GONE);
            if (!success && isAdded() && getContext() != null) {
                Toast.makeText(getContext(),
                        message != null && !message.isEmpty() ? message : getString(R.string.loading),
                        Toast.LENGTH_SHORT).show();
            }
            items.clear();
            if (teachers != null) {
                items.addAll(teachers);
            }
            adapter.notifyDataSetChanged();
            empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private class RowsAdapter extends RecyclerView.Adapter<RowsAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher_discover_row, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            User t = items.get(position);
            h.name.setText(t.getDisplayName());
            String spec = t.specialization != null && !t.specialization.isEmpty() ? t.specialization : "—";
            h.spec.setText(getString(R.string.specialization) + ": " + spec);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView spec;

            Holder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.teacher_name);
                spec = itemView.findViewById(R.id.teacher_spec);
            }
        }
    }
}
