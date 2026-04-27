package com.example.rawaaproject;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rawaaproject.data.LessonRepository;
import com.example.rawaaproject.data.SessionManager;
import com.example.rawaaproject.util.RoleThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;

public class PublishLessonActivity extends AppCompatActivity {

    private Spinner subjectSpinner;
    private TextInputEditText titleInput;
    private TextView datetimeSummary;
    private MaterialButton pickDatetime;
    private MaterialButton submit;
    private MaterialButton cancel;
    private TextInputEditText priceInput;

    @Nullable
    private ZonedDateTime chosenStart;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        RoleThemeHelper.applyForLoggedInUser(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_lesson);

        MaterialToolbar toolbar = findViewById(R.id.publish_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        subjectSpinner = findViewById(R.id.publish_subject_spinner);
        titleInput = findViewById(R.id.publish_lesson_title_input);
        datetimeSummary = findViewById(R.id.publish_datetime_summary);
        pickDatetime = findViewById(R.id.publish_pick_datetime);
        submit = findViewById(R.id.publish_submit);
        cancel = findViewById(R.id.publish_cancel);
        priceInput = findViewById(R.id.publish_lesson_price_input);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.teacher_subjects, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjectSpinner.setAdapter(adapter);

        pickDatetime.setOnClickListener(v -> showDateThenTimePicker());

        submit.setOnClickListener(v -> submitLesson());
        cancel.setOnClickListener(v -> finish());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void showDateThenTimePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dateDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            TimePickerDialog timeDialog = new TimePickerDialog(this, (tv, hourOfDay, minute) -> {
                LocalDate date = LocalDate.of(year, month + 1, dayOfMonth);
                LocalTime time = LocalTime.of(hourOfDay, minute);
                LocalDateTime ldt = LocalDateTime.of(date, time);
                chosenStart = ldt.atZone(ZoneId.systemDefault());
                datetimeSummary.setText(formatChosen(chosenStart));
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
            timeDialog.show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dateDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dateDialog.show();
    }

    private String formatChosen(ZonedDateTime z) {
        return z.toLocalDate().toString() + " " + z.toLocalTime().withSecond(0).withNano(0);
    }

    private void submitLesson() {
        SessionManager sm = new SessionManager(this);
        if (!sm.isLoggedIn() || sm.getCurrentUser() == null || !sm.getCurrentUser().isTeacher()) {
            Toast.makeText(this, R.string.publish_lesson_not_teacher, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        CharSequence subj = subjectSpinner.getSelectedItem() != null
                ? subjectSpinner.getSelectedItem().toString() : "";
        String title = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.lesson_topic_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (chosenStart == null) {
            Toast.makeText(this, R.string.lesson_datetime_required, Toast.LENGTH_SHORT).show();
            return;
        }
        String iso = chosenStart.toInstant().toString();
        if (!LessonRepository.isStartAtInFuture(iso)) {
            Toast.makeText(this, R.string.lesson_time_must_be_future, Toast.LENGTH_SHORT).show();
            return;
        }
        submit.setEnabled(false);
        String teacherId = sm.getCurrentUser().id;
        String price = priceInput.getText() != null ? priceInput.getText().toString().trim() : "";
        new LessonRepository(this).publishLesson(teacherId, subj.toString().trim(), title, iso, price, result -> {
            submit.setEnabled(true);
            if (result.success) {
                Toast.makeText(this, R.string.publish_lesson_ok, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this,
                        result.message != null ? result.message : getString(R.string.publish_lesson_failed),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
