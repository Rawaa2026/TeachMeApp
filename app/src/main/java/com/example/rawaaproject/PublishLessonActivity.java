package com.example.rawaaproject;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rawaaproject.data.LessonRepository;
import com.example.rawaaproject.data.SessionManager;
import com.example.rawaaproject.util.BirthDateSpinners;
import com.example.rawaaproject.util.LessonScheduleSpinners;
import com.example.rawaaproject.util.RoleThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Locale;

public class PublishLessonActivity extends AppCompatActivity {

    private Spinner subjectSpinner;
    private TextInputEditText titleInput;
    private TextView datetimeSummary;
    private Spinner yearSpinner;
    private Spinner monthSpinner;
    private Spinner daySpinner;
    private Spinner hourSpinner;
    private Spinner minuteSpinner;
    private MaterialButton submit;
    private MaterialButton cancel;
    private TextInputEditText priceInput;

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
        yearSpinner = findViewById(R.id.publish_year_spinner);
        monthSpinner = findViewById(R.id.publish_month_spinner);
        daySpinner = findViewById(R.id.publish_day_spinner);
        hourSpinner = findViewById(R.id.publish_hour_spinner);
        minuteSpinner = findViewById(R.id.publish_minute_spinner);
        submit = findViewById(R.id.publish_submit);
        cancel = findViewById(R.id.publish_cancel);
        priceInput = findViewById(R.id.publish_lesson_price_input);

        ArrayAdapter<CharSequence> subjectAdapter = ArrayAdapter.createFromResource(this,
                R.array.teacher_subjects, android.R.layout.simple_spinner_dropdown_item);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subjectSpinner.setAdapter(subjectAdapter);

        setupDateTimeSpinners();

        submit.setOnClickListener(v -> submitLesson());
        cancel.setOnClickListener(v -> finish());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void setupDateTimeSpinners() {
        BirthDateSpinners.setupYear(this, yearSpinner, R.array.lesson_schedule_years, R.string.birth_year_prompt);
        BirthDateSpinners.setupMonth(this, monthSpinner, R.string.birth_month_prompt);
        BirthDateSpinners.setupDays(this, daySpinner, R.string.birth_day_prompt, 31);

        LessonScheduleSpinners.setupHour(this, hourSpinner, R.string.lesson_hour);
        LessonScheduleSpinners.setupMinute(this, minuteSpinner, R.string.lesson_minute);

        enableSpinnerTouch(subjectSpinner);
        enableSpinnerTouch(yearSpinner);
        enableSpinnerTouch(monthSpinner);
        enableSpinnerTouch(daySpinner);
        enableSpinnerTouch(hourSpinner);
        enableSpinnerTouch(minuteSpinner);

        Calendar now = Calendar.getInstance();
        String today = String.format(
                Locale.US,
                "%04d-%02d-%02d",
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH));
        BirthDateSpinners.selectFromStored(this, yearSpinner, monthSpinner, daySpinner, today, R.string.birth_day_prompt);
        LessonScheduleSpinners.selectHour(hourSpinner, now.get(Calendar.HOUR_OF_DAY));
        LessonScheduleSpinners.selectMinute(minuteSpinner, now.get(Calendar.MINUTE));

        AdapterView.OnItemSelectedListener dateListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                BirthDateSpinners.refreshDaySpinner(
                        PublishLessonActivity.this,
                        yearSpinner,
                        monthSpinner,
                        daySpinner,
                        R.string.birth_day_prompt);
                updateSummary();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        yearSpinner.setOnItemSelectedListener(dateListener);
        monthSpinner.setOnItemSelectedListener(dateListener);

        AdapterView.OnItemSelectedListener summaryListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSummary();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        daySpinner.setOnItemSelectedListener(summaryListener);
        hourSpinner.setOnItemSelectedListener(summaryListener);
        minuteSpinner.setOnItemSelectedListener(summaryListener);

        updateSummary();
    }

    private void enableSpinnerTouch(Spinner spinner) {
        LessonScheduleSpinners.enableDropdownTouch(spinner);
    }

    @Nullable
    private ZonedDateTime readChosenStart() {
        String dateIso = BirthDateSpinners.toIsoDateOrNull(yearSpinner, monthSpinner, daySpinner);
        Integer hour = LessonScheduleSpinners.selectedHour(hourSpinner);
        Integer minute = LessonScheduleSpinners.selectedMinute(minuteSpinner);
        if (dateIso == null || hour == null || minute == null) {
            return null;
        }
        String[] parts = dateIso.split("-");
        LocalDateTime ldt = LocalDateTime.of(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                hour,
                minute);
        return ldt.atZone(ZoneId.systemDefault());
    }

    private void updateSummary() {
        ZonedDateTime chosen = readChosenStart();
        if (chosen == null) {
            datetimeSummary.setText(R.string.lesson_datetime_not_set);
            return;
        }
        datetimeSummary.setText(formatChosen(chosen));
    }

    private String formatChosen(ZonedDateTime z) {
        return String.format(
                Locale.getDefault(),
                "%04d-%02d-%02d  %02d:%02d",
                z.getYear(),
                z.getMonthValue(),
                z.getDayOfMonth(),
                z.getHour(),
                z.getMinute());
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

        ZonedDateTime chosenStart = readChosenStart();
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
