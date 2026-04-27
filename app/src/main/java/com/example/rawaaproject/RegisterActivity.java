package com.example.rawaaproject;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.card.MaterialCardView;
import com.example.rawaaproject.data.AuthRepository;
import com.example.rawaaproject.data.SessionManager;
import com.example.rawaaproject.util.BirthDateSpinners;
import com.example.rawaaproject.util.ProfileValidation;

/**
 * شاشة التسجيل: اختيار الدور (مدرس/طالب) ثم الاسم، الصورة، الوصف، وتاريخ الميلاد للطالب.
 * مربوطة بـ AuthRepository وقاعدة البيانات.
 */
public class RegisterActivity extends AppCompatActivity {

    public static final int RESULT_REGISTER_OK = 100;

    private boolean isStudent = false;

    private MaterialCardView cardTeacher;
    private MaterialCardView cardStudent;
    private EditText fullName;
    private EditText phoneInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private View continueBtn;
    private View goLogin;
    private LinearLayout studentBirthBlock;
    private Spinner birthDaySpinner;
    private Spinner birthMonthSpinner;
    private Spinner birthYearSpinner;

    private AuthRepository authRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        TextView headerTitle = findViewById(R.id.header_title);
        if (headerTitle != null) {
            headerTitle.setText(R.string.register);
        }

        authRepository = new AuthRepository(this);
        sessionManager = new SessionManager(this);

        cardTeacher = findViewById(R.id.card_teacher);
        cardStudent = findViewById(R.id.card_student);
        fullName = findViewById(R.id.register_full_name);
        phoneInput = findViewById(R.id.register_phone);
        emailInput = findViewById(R.id.register_email);
        passwordInput = findViewById(R.id.register_password);
        confirmPasswordInput = findViewById(R.id.register_confirm_password);
        continueBtn = findViewById(R.id.register_continue_btn);
        goLogin = findViewById(R.id.register_go_login);
        studentBirthBlock = findViewById(R.id.register_student_birth_block);
        birthDaySpinner = findViewById(R.id.register_birth_day);
        birthMonthSpinner = findViewById(R.id.register_birth_month);
        birthYearSpinner = findViewById(R.id.register_birth_year);

        setupStudentBirthPickers();

        cardTeacher.setOnClickListener(v -> setRole(false));
        cardStudent.setOnClickListener(v -> setRole(true));

        continueBtn.setOnClickListener(v -> submitRegister());
        goLogin.setOnClickListener(v -> finish());

        setRole(false);
    }

    private void setupStudentBirthPickers() {
        BirthDateSpinners.setupYear(this, birthYearSpinner, R.array.student_birth_years, R.string.birth_year_prompt);
        BirthDateSpinners.setupMonth(this, birthMonthSpinner, R.string.birth_month_prompt);
        BirthDateSpinners.setupDays(this, birthDaySpinner, R.string.birth_day_prompt, 31);
        BirthDateSpinners.wireDayUpdates(this, birthYearSpinner, birthMonthSpinner, birthDaySpinner,
                R.string.birth_day_prompt);
    }

    private void setRole(boolean student) {
        isStudent = student;
        int strokePx = getResources().getDimensionPixelSize(R.dimen.stroke_selected);
        int teacherColor = ContextCompat.getColor(this, R.color.brand_teacher);
        int studentColor = ContextCompat.getColor(this, R.color.brand_student);
        if (student) {
            cardTeacher.setStrokeWidth(0);
            cardTeacher.setStrokeColor(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
            cardStudent.setStrokeWidth(strokePx);
            cardStudent.setStrokeColor(ColorStateList.valueOf(studentColor));
        } else {
            cardTeacher.setStrokeWidth(strokePx);
            cardTeacher.setStrokeColor(ColorStateList.valueOf(teacherColor));
            cardStudent.setStrokeWidth(0);
            cardStudent.setStrokeColor(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        }
        int accent = student ? studentColor : teacherColor;
        if (continueBtn instanceof Button) {
            ((Button) continueBtn).setBackgroundTintList(ColorStateList.valueOf(accent));
        }
        studentBirthBlock.setVisibility(student ? View.VISIBLE : View.GONE);
        if (!student) {
            birthDaySpinner.setSelection(0);
            birthMonthSpinner.setSelection(0);
            birthYearSpinner.setSelection(0);
        }
    }

    private void submitRegister() {
        String name = fullName.getText() != null ? fullName.getText().toString().trim() : "";
        String phone = phoneInput.getText() != null ? phoneInput.getText().toString().trim() : "";
        String email = emailInput != null && emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput != null ? passwordInput.getText().toString() : "";
        String confirmPassword = confirmPasswordInput != null ? confirmPasswordInput.getText().toString() : "";
        
        // التحقق من الحقول الأساسية
        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.full_name_hint), Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, getString(R.string.phone_hint), Toast.LENGTH_SHORT).show();
            return;
        }
        if (email.isEmpty()) {
            Toast.makeText(this, getString(R.string.email), Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, getString(R.string.password_min_length), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, getString(R.string.password_mismatch), Toast.LENGTH_SHORT).show();
            return;
        }

        // جمع البيانات
        String role = isStudent ? "student" : "teacher";

        String birthDate = null;
        if (isStudent) {
            birthDate = BirthDateSpinners.toIsoDateOrNull(birthYearSpinner, birthMonthSpinner, birthDaySpinner);
            if (birthDate == null) {
                Toast.makeText(this, R.string.birth_date_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!ProfileValidation.isAgeAtLeastYears(birthDate, 7)) {
                Toast.makeText(this, getString(R.string.student_age_min_years, 7), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        continueBtn.setEnabled(false);
        
        // استخدام الدالة الجديدة مع البيانات الأساسية فقط
        authRepository.register(role, name, email, password, phone, birthDate, null, null, result -> {
            continueBtn.setEnabled(true);
            if (result.success && result.data != null) {
                // حفظ بيانات المستخدم الكاملة
                if (result.customData instanceof com.example.rawaaproject.models.User) {
                    sessionManager.saveUserSession((com.example.rawaaproject.models.User) result.customData);
                } else {
                    sessionManager.saveLogin(result.data.userId, result.data.email, role);
                }
                setResult(RESULT_REGISTER_OK);
                finish();
            } else {
                Toast.makeText(this, result.message != null ? result.message : getString(R.string.register_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
