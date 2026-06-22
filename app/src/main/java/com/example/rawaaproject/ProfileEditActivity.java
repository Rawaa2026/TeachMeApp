package com.example.rawaaproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.rawaaproject.data.SessionManager;
import com.example.rawaaproject.data.UserRepository;
import com.example.rawaaproject.models.User;
import com.example.rawaaproject.util.BirthDateSpinners;
import com.example.rawaaproject.util.ProfilePhotoStore;
import com.example.rawaaproject.util.ProfileValidation;
import com.example.rawaaproject.util.RoleThemeHelper;
import com.example.rawaaproject.util.SafeAreaHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * نشاط تعديل الملف الشخصي - يدعم البيانات الأساسية فقط
 */
public class ProfileEditActivity extends AppCompatActivity {

    private ImageView photoView;
    private TextView addPhotoLabel;
    private View photoClickArea;
    private TextView headerTitle;

    private EditText fullNameInput;
    private EditText phoneInput;
    private TextView emailView;

    private MaterialButton saveBtn;
    private Button galleryBtn;
    private Button cameraBtn;

    private LinearLayout teacherFields;
    private LinearLayout studentFields;
    private Spinner birthDaySpinner;
    private Spinner birthMonthSpinner;
    private Spinner birthYearSpinner;
    private EditText descriptionInput;

    private MaterialButton specializationPickBtn;
    private TextView specializationSummary;
    private Spinner experienceYearsSpinner;
    private EditText hourlyRateInput;

    private Spinner gradeSpinner;
    private EditText schoolInput;

    private String[] subjectLabels;
    private boolean[] teacherSubjectChecked;

    private SessionManager sessionManager;
    private UserRepository userRepository;
    private Uri photoUri;
    private Uri cameraImageUri;
    private User currentUser;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null || currentUser == null || currentUser.id == null || currentUser.id.isEmpty()) {
                    return;
                }
                if (!ProfilePhotoStore.copyFromUri(this, currentUser.id, uri)) {
                    Toast.makeText(this, R.string.photo_save_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                File local = ProfilePhotoStore.getPhotoFile(this, currentUser.id);
                if (local == null || !local.exists()) {
                    Toast.makeText(this, R.string.photo_save_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                currentUser.profileImageUrl = local.getAbsolutePath();
                sessionManager.saveUserSession(currentUser);
                photoUri = Uri.fromFile(local);
                showProfilePhoto(local);
            });

    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (!success || cameraImageUri == null || currentUser == null
                        || currentUser.id == null || currentUser.id.isEmpty()) {
                    return;
                }
                if (!ProfilePhotoStore.copyFromUri(this, currentUser.id, cameraImageUri)) {
                    Toast.makeText(this, R.string.photo_save_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                File local = ProfilePhotoStore.getPhotoFile(this, currentUser.id);
                if (local == null || !local.exists()) {
                    Toast.makeText(this, R.string.photo_save_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                currentUser.profileImageUrl = local.getAbsolutePath();
                sessionManager.saveUserSession(currentUser);
                photoUri = Uri.fromFile(local);
                showProfilePhoto(local);
            });

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        RoleThemeHelper.applyForLoggedInUser(this);
        super.onCreate(savedInstanceState);
        SafeAreaHelper.enableEdgeToEdge(this);
        setContentView(R.layout.activity_profile_edit);

        SafeAreaHelper.applyHeaderStatusBarInset(findViewById(R.id.header_root));
        SafeAreaHelper.applyNavigationBarPadding(findViewById(android.R.id.content));

        sessionManager = new SessionManager(this);
        userRepository = new UserRepository(this);

        initViews();
        setupHeader();
        loadUserData();
        setupClickListeners();
    }

    private void initViews() {
        headerTitle = findViewById(R.id.header_title);
        photoView = findViewById(R.id.profile_edit_photo);
        addPhotoLabel = findViewById(R.id.profile_edit_add_photo_label);
        photoClickArea = findViewById(R.id.profile_edit_photo_click);
        galleryBtn = findViewById(R.id.profile_edit_pick_gallery_btn);
        cameraBtn = findViewById(R.id.profile_edit_pick_camera_btn);

        fullNameInput = findViewById(R.id.profile_edit_full_name);
        phoneInput = findViewById(R.id.profile_edit_phone);
        emailView = findViewById(R.id.profile_edit_email);

        saveBtn = findViewById(R.id.profile_edit_save_btn);

        teacherFields = findViewById(R.id.profile_edit_teacher_fields);
        studentFields = findViewById(R.id.profile_edit_student_fields);
        birthDaySpinner = findViewById(R.id.profile_edit_birth_day);
        birthMonthSpinner = findViewById(R.id.profile_edit_birth_month);
        birthYearSpinner = findViewById(R.id.profile_edit_birth_year);
        descriptionInput = findViewById(R.id.profile_edit_description);

        specializationPickBtn = findViewById(R.id.profile_edit_specialization_btn);
        specializationSummary = findViewById(R.id.profile_edit_specialization_summary);
        experienceYearsSpinner = findViewById(R.id.profile_edit_experience_years);
        hourlyRateInput = findViewById(R.id.profile_edit_hourly_rate);

        gradeSpinner = findViewById(R.id.profile_edit_grade_spinner);
        schoolInput = findViewById(R.id.profile_edit_school);

        subjectLabels = getResources().getStringArray(R.array.israel_school_subjects);
        teacherSubjectChecked = new boolean[subjectLabels.length];
    }

    private void setupHeader() {
        if (headerTitle != null) {
            headerTitle.setText(R.string.edit_profile);
        }
    }

    private void loadUserData() {
        currentUser = sessionManager.getUserSession();
        if (currentUser == null) {
            Toast.makeText(this, "لم يتم العثور على بيانات المستخدم", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fullNameInput.setText(currentUser.fullName != null ? currentUser.fullName : "");
        phoneInput.setText(currentUser.phoneNumber != null ? currentUser.phoneNumber : "");
        emailView.setText(currentUser.email != null ? currentUser.email : "");

        if (currentUser.isTeacher()) {
            teacherFields.setVisibility(View.VISIBLE);
            studentFields.setVisibility(View.GONE);
            descriptionInput.setHint(getString(R.string.description_hint_teacher));
            Arrays.fill(teacherSubjectChecked, false);
            applyStoredSubjectsToChecks(currentUser.specialization, teacherSubjectChecked);
            setupExperienceYearsSpinner();
            selectExperienceFromUser();
            hourlyRateInput.setText(currentUser.hourlyRate != null ? currentUser.hourlyRate : "");
            refreshTeacherSubjectsSummary();
        } else {
            teacherFields.setVisibility(View.GONE);
            studentFields.setVisibility(View.VISIBLE);
            descriptionInput.setHint(getString(R.string.description_hint_student));
            setupStudentBirthPickers();
            applyBirthSelectionFromUser();
            setupGradeSpinner();
            selectGradeFromUser();
            schoolInput.setText(currentUser.school != null ? currentUser.school : "");
        }

        descriptionInput.setText(currentUser.description != null ? currentUser.description : "");

        ProfilePhotoStore.preferLocalPhotoUrl(this, currentUser);
        bindProfilePhoto();
    }

    private void bindProfilePhoto() {
        if (currentUser == null) {
            return;
        }
        Object model = ProfilePhotoStore.displayModel(this, currentUser.id, currentUser.profileImageUrl);
        if (model != null) {
            try {
                if (model instanceof File) {
                    showProfilePhoto((File) model);
                } else {
                    Glide.with(this).load(model).centerCrop().into(photoView);
                    photoView.setVisibility(View.VISIBLE);
                    addPhotoLabel.setVisibility(View.GONE);
                }
                return;
            } catch (Exception ignored) {
            }
        }
        photoView.setVisibility(View.GONE);
        addPhotoLabel.setVisibility(View.VISIBLE);
    }

    private void showProfilePhoto(File local) {
        Glide.with(this)
                .load(local)
                .centerCrop()
                .signature(new ObjectKey(local.lastModified()))
                .into(photoView);
        photoView.setVisibility(View.VISIBLE);
        addPhotoLabel.setVisibility(View.GONE);
    }

    private void setupStudentBirthPickers() {
        BirthDateSpinners.setupYear(this, birthYearSpinner, R.array.student_birth_years, R.string.birth_year_prompt);
        BirthDateSpinners.setupMonth(this, birthMonthSpinner, R.string.birth_month_prompt);
        BirthDateSpinners.setupDays(this, birthDaySpinner, R.string.birth_day_prompt, 31);
        BirthDateSpinners.wireDayUpdates(this, birthYearSpinner, birthMonthSpinner, birthDaySpinner,
                R.string.birth_day_prompt);
    }

    private void applyBirthSelectionFromUser() {
        BirthDateSpinners.selectFromStored(this, birthYearSpinner, birthMonthSpinner, birthDaySpinner,
                currentUser.birthDate, R.string.birth_day_prompt);
    }

    private void applyStoredSubjectsToChecks(String stored, boolean[] checks) {
        Arrays.fill(checks, false);
        if (stored == null || stored.trim().isEmpty()) {
            return;
        }
        for (String part : stored.split("[\\n،,|]+")) {
            String t = part.trim();
            if (t.isEmpty()) {
                continue;
            }
            for (int i = 0; i < subjectLabels.length; i++) {
                if (subjectLabels[i].equals(t)) {
                    checks[i] = true;
                    break;
                }
            }
        }
    }

    private void setupExperienceYearsSpinner() {
        if (experienceYearsSpinner.getAdapter() != null) {
            return;
        }
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.experience_prompt));
        for (int i = 1; i <= 40; i++) {
            items.add(String.valueOf(i));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        experienceYearsSpinner.setAdapter(adapter);
    }

    private void selectExperienceFromUser() {
        String ex = currentUser.experience != null ? currentUser.experience.trim() : "";
        if (ex.isEmpty()) {
            experienceYearsSpinner.setSelection(0);
            return;
        }
        android.widget.Adapter ad = experienceYearsSpinner.getAdapter();
        if (ad == null) {
            return;
        }
        for (int i = 0; i < ad.getCount(); i++) {
            Object it = ad.getItem(i);
            if (it != null && ex.equals(it.toString())) {
                experienceYearsSpinner.setSelection(i);
                return;
            }
        }
        experienceYearsSpinner.setSelection(0);
    }

    private void setupGradeSpinner() {
        if (gradeSpinner.getAdapter() != null) {
            return;
        }
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.grade_prompt));
        String[] grades = getResources().getStringArray(R.array.student_grades_1_12);
        for (String g : grades) {
            items.add(g);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gradeSpinner.setAdapter(adapter);
    }

    private void selectGradeFromUser() {
        String g = currentUser.grade != null ? currentUser.grade.trim() : "";
        if (g.isEmpty()) {
            gradeSpinner.setSelection(0);
            return;
        }
        android.widget.Adapter ad = gradeSpinner.getAdapter();
        if (ad == null) {
            return;
        }
        for (int i = 0; i < ad.getCount(); i++) {
            Object it = ad.getItem(i);
            if (it != null && g.equals(it.toString())) {
                gradeSpinner.setSelection(i);
                return;
            }
        }
        gradeSpinner.setSelection(0);
    }

    private static int countSelected(boolean[] checks) {
        int n = 0;
        for (boolean b : checks) {
            if (b) {
                n++;
            }
        }
        return n;
    }

    private String joinSelectedSubjects(boolean[] checks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < checks.length; i++) {
            if (checks[i]) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(subjectLabels[i]);
            }
        }
        return sb.toString();
    }

    private void refreshTeacherSubjectsSummary() {
        int n = countSelected(teacherSubjectChecked);
        if (n == 0) {
            specializationSummary.setVisibility(View.GONE);
            return;
        }
        specializationSummary.setVisibility(View.VISIBLE);
        String joined = joinSelectedSubjects(teacherSubjectChecked);
        String preview = joined.length() > 120 ? joined.substring(0, 120) + "…" : joined;
        specializationSummary.setText(getString(R.string.selected_subjects_summary, n) + "\n" + preview);
    }

    private void showTeacherSubjectsPickDialog() {
        boolean[] state = Arrays.copyOf(teacherSubjectChecked, subjectLabels.length);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.specialization)
                .setMultiChoiceItems(subjectLabels, state, (dialog, which, isChecked) -> state[which] = isChecked)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    System.arraycopy(state, 0, teacherSubjectChecked, 0, state.length);
                    refreshTeacherSubjectsSummary();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setupClickListeners() {
        saveBtn.setOnClickListener(v -> saveProfile());
        galleryBtn.setOnClickListener(v -> pickImage.launch("image/*"));
        cameraBtn.setOnClickListener(v -> openCamera());
        photoClickArea.setOnClickListener(v -> pickImage.launch("image/*"));
        specializationPickBtn.setOnClickListener(v -> showTeacherSubjectsPickDialog());
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
            return;
        }
        launchCamera();
    }

    private void launchCamera() {
        try {
            File photoFile = new File(getCacheDir(), "profile_capture_" + System.currentTimeMillis() + ".jpg");
            cameraImageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile);
            takePicture.launch(cameraImageUri);
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح الكاميرا", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfile() {
        String fullName = fullNameInput.getText() != null ? fullNameInput.getText().toString().trim() : "";
        String phone = phoneInput.getText() != null ? phoneInput.getText().toString().trim() : "";
        String email = emailView.getText() != null ? emailView.getText().toString().trim() : "";

        if (fullName.isEmpty()) {
            Toast.makeText(this, "الاسم الكامل مطلوب", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, "رقم الهاتف مطلوب", Toast.LENGTH_SHORT).show();
            return;
        }
        if (email.isEmpty()) {
            Toast.makeText(this, "البريد الإلكتروني مطلوب", Toast.LENGTH_SHORT).show();
            return;
        }

        User sessionUser = sessionManager.getUserSession();
        if (sessionUser != null) {
            sessionUser.fullName = fullName;
            sessionUser.phoneNumber = phone;
            sessionUser.updateTimestamp();

            String desc = descriptionInput.getText() != null ? descriptionInput.getText().toString().trim() : "";
            sessionUser.description = desc.isEmpty() ? null : desc;

            if (sessionUser.isTeacher()) {
                sessionUser.birthDate = null;
                if (countSelected(teacherSubjectChecked) == 0) {
                    Toast.makeText(this, R.string.specialization_required, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (experienceYearsSpinner.getSelectedItemPosition() <= 0) {
                    Toast.makeText(this, R.string.experience_required, Toast.LENGTH_SHORT).show();
                    return;
                }
                sessionUser.specialization = joinSelectedSubjects(teacherSubjectChecked);
                sessionUser.experience = experienceYearsSpinner.getSelectedItem().toString();
                String rate = hourlyRateInput.getText() != null ? hourlyRateInput.getText().toString().trim() : "";
                if (rate.isEmpty()) {
                    Toast.makeText(this, R.string.hourly_rate_required, Toast.LENGTH_SHORT).show();
                    return;
                }
                sessionUser.hourlyRate = rate;
                sessionUser.grade = null;
                sessionUser.school = null;
                sessionUser.subjectsNeeded = null;
            } else {
                String iso = BirthDateSpinners.toIsoDateOrNull(birthYearSpinner, birthMonthSpinner, birthDaySpinner);
                if (iso == null) {
                    Toast.makeText(this, R.string.birth_date_required, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!ProfileValidation.isAgeAtLeastYears(iso, 7)) {
                    Toast.makeText(this, getString(R.string.student_age_min_years, 7), Toast.LENGTH_SHORT).show();
                    return;
                }
                sessionUser.birthDate = iso;
                if (gradeSpinner.getSelectedItemPosition() <= 0) {
                    Toast.makeText(this, R.string.grade_required, Toast.LENGTH_SHORT).show();
                    return;
                }
                sessionUser.grade = gradeSpinner.getSelectedItem().toString();
                String school = schoolInput.getText() != null ? schoolInput.getText().toString().trim() : "";
                if (school.isEmpty()) {
                    Toast.makeText(this, R.string.school_required, Toast.LENGTH_SHORT).show();
                    return;
                }
                sessionUser.school = school;
                sessionUser.subjectsNeeded = null;
                sessionUser.specialization = null;
                sessionUser.experience = null;
                sessionUser.hourlyRate = null;
            }

            userRepository.updateUser(sessionUser, photoUri, result -> {
                if (result.success) {
                    ProfilePhotoStore.preferLocalPhotoUrl(ProfileEditActivity.this, sessionUser);
                    sessionManager.saveUserSession(sessionUser);
                    Toast.makeText(this, "تم تحديث الملف الشخصي", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this,
                            result.message != null ? result.message : getString(R.string.profile_save_failed),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
