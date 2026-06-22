package com.example.rawaaproject.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.rawaaproject.ProfileEditActivity;
import com.example.rawaaproject.R;
import com.example.rawaaproject.util.ProfilePhotoStore;
import com.example.rawaaproject.data.LessonRepository;
import com.example.rawaaproject.data.SessionManager;
import com.example.rawaaproject.data.UserRepository;
import com.example.rawaaproject.models.Lesson;
import com.example.rawaaproject.models.User;
import com.example.rawaaproject.util.LessonCostHelper;

import java.io.File;
import java.time.ZoneId;
import java.util.ArrayList;

/**
 * صفحة حسابي المبسطة - عرض البيانات والانتقال للتعديل
 */
public class ProfileFragment extends Fragment {

    private SessionManager sessionManager;
    private UserRepository userRepository;
    private User currentUser;

    private ImageView profilePhoto;
    private TextView profilePhotoAddLabel;
    private TextView profileName;
    private TextView profileEmail;
    private TextView profilePhone;
    private TextView profileRole;
    private Button profileEditBtn;
    private Button profileLogoutBtn;
    private Button profilePickGalleryBtn;
    private Button profilePickCameraBtn;
    private View profilePhotoClick;
    private View profileMainCard;

    private View teacherExtraBlock;
    private View studentExtraBlock;
    private TextView profileTeacherSubjects;
    private TextView profileTeacherExperience;
    private TextView profileTeacherHourly;
    private TextView profileTeacherBio;
    private TextView profileStudentBirth;
    private TextView profileStudentGrade;
    private TextView profileStudentSchool;
    private TextView profileStudentBio;

    private ProgressBar profileLessonsCostProgress;
    private TextView profileLessonsCostToday;
    private TextView profileLessonsCostTotal;

    private ActivityResultLauncher<String> pickImage;
    private ActivityResultLauncher<Uri> takePicture;
    private ActivityResultLauncher<String> requestCameraPermission;
    private Uri cameraImageUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        userRepository = new UserRepository(requireContext());

        pickImage = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                applyPickedPhoto(uri);
            }
        });
        takePicture = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraImageUri != null) {
                applyPickedPhoto(cameraImageUri);
            }
        });
        requestCameraPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        launchCamera();
                    } else {
                        Toast.makeText(requireContext(), R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupUserProfile();
        setupClickListeners();
    }

    private void initViews(View view) {
        profilePhoto = view.findViewById(R.id.profile_photo);
        profilePhotoAddLabel = view.findViewById(R.id.profile_photo_add_label);
        profileName = view.findViewById(R.id.profile_name);
        profileEmail = view.findViewById(R.id.profile_email);
        profilePhone = view.findViewById(R.id.profile_phone);
        profileRole = view.findViewById(R.id.profile_role);
        profileEditBtn = view.findViewById(R.id.profile_edit_btn);
        profileLogoutBtn = view.findViewById(R.id.profile_logout_btn);
        profilePickGalleryBtn = view.findViewById(R.id.profile_pick_gallery_btn);
        profilePickCameraBtn = view.findViewById(R.id.profile_pick_camera_btn);
        profilePhotoClick = view.findViewById(R.id.profile_photo_click);
        profileMainCard = view.findViewById(R.id.profile_main_card);

        teacherExtraBlock = view.findViewById(R.id.profile_teacher_extra);
        studentExtraBlock = view.findViewById(R.id.profile_student_extra);
        profileTeacherSubjects = view.findViewById(R.id.profile_teacher_subjects);
        profileTeacherExperience = view.findViewById(R.id.profile_teacher_experience);
        profileTeacherHourly = view.findViewById(R.id.profile_teacher_hourly);
        profileTeacherBio = view.findViewById(R.id.profile_teacher_bio);
        profileStudentBirth = view.findViewById(R.id.profile_student_birth);
        profileStudentGrade = view.findViewById(R.id.profile_student_grade);
        profileStudentSchool = view.findViewById(R.id.profile_student_school);
        profileStudentBio = view.findViewById(R.id.profile_student_bio);
        profileLessonsCostProgress = view.findViewById(R.id.profile_lessons_cost_progress);
        profileLessonsCostToday = view.findViewById(R.id.profile_lessons_cost_today);
        profileLessonsCostTotal = view.findViewById(R.id.profile_lessons_cost_total);
    }

    private void setupUserProfile() {
        currentUser = sessionManager.getUserSession();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "لم يتم العثور على بيانات المستخدم", Toast.LENGTH_SHORT).show();
            return;
        }

        final User localSnapshot = currentUser;
        ProfilePhotoStore.preferLocalPhotoUrl(requireContext(), localSnapshot);
        sessionManager.saveUserSession(localSnapshot);
        displayUserData();

        if (localSnapshot.id != null && !localSnapshot.id.isEmpty()) {
            new Thread(() -> {
                User updatedUser = userRepository.getUserById(localSnapshot.id);
                requireActivity().runOnUiThread(() -> {
                    if (updatedUser != null) {
                        copySessionProfileExtras(localSnapshot, updatedUser);
                        ProfilePhotoStore.preferLocalPhotoUrl(requireContext(), updatedUser);
                        currentUser = updatedUser;
                        sessionManager.saveUserSession(currentUser);
                    }
                    displayUserData();
                });
            }).start();
        }
    }

    /**
     * حقول المواد والخبرة والنبذة وغيرها غير مخزّنة في مستند Appwrite — ندمجها من الجلسة
     * حتى لا تُستبدل بقيم فارغة عند جلب المستند.
     */
    private static void copySessionProfileExtras(User from, User onto) {
        if (from == null || onto == null) {
            return;
        }
        onto.specialization = from.specialization;
        onto.experience = from.experience;
        onto.description = from.description;
        onto.hourlyRate = from.hourlyRate;
        onto.grade = from.grade;
        onto.school = from.school;
        if (from.isStudent()) {
            onto.subjectsNeeded = null;
        } else {
            onto.subjectsNeeded = from.subjectsNeeded;
        }
        onto.birthDate = from.birthDate;
    }

    private void displayUserData() {
        if (currentUser == null) return;

        // البيانات الأساسية
        profileName.setText(currentUser.fullName != null && !currentUser.fullName.isEmpty() ? 
            currentUser.fullName : "غير محدد");
        profileEmail.setText(currentUser.email != null && !currentUser.email.isEmpty() ? 
            currentUser.email : "غير محدد");
        profilePhone.setText(currentUser.phoneNumber != null && !currentUser.phoneNumber.isEmpty() ? 
            currentUser.phoneNumber : "غير محدد");
        profileRole.setText(currentUser.role != null ?
            (currentUser.isTeacher() ? "معلم" : "طالب") : "غير محدد");

        applyRoleCardStyle();

        String dash = getString(R.string.profile_dash);
        if (currentUser.isTeacher()) {
            teacherExtraBlock.setVisibility(View.VISIBLE);
            studentExtraBlock.setVisibility(View.GONE);
            profileTeacherSubjects.setText(formatMultilineSubjects(currentUser.specialization, dash));
            if (currentUser.experience != null && !currentUser.experience.trim().isEmpty()) {
                profileTeacherExperience.setText(
                        getString(R.string.profile_experience_years, currentUser.experience.trim()));
            } else {
                profileTeacherExperience.setText(dash);
            }
            profileTeacherHourly.setText(
                    currentUser.hourlyRate != null && !currentUser.hourlyRate.trim().isEmpty()
                            ? currentUser.hourlyRate.trim()
                            : dash);
            profileTeacherBio.setText(
                    currentUser.description != null && !currentUser.description.trim().isEmpty()
                            ? currentUser.description.trim()
                            : dash);
        } else {
            teacherExtraBlock.setVisibility(View.GONE);
            studentExtraBlock.setVisibility(View.VISIBLE);
            profileStudentBirth.setText(
                    currentUser.birthDate != null && !currentUser.birthDate.trim().isEmpty()
                            ? currentUser.birthDate.trim()
                            : dash);
            profileStudentGrade.setText(
                    currentUser.grade != null && !currentUser.grade.trim().isEmpty()
                            ? currentUser.grade.trim()
                            : dash);
            profileStudentSchool.setText(
                    currentUser.school != null && !currentUser.school.trim().isEmpty()
                            ? currentUser.school.trim()
                            : dash);
            profileStudentBio.setText(
                    currentUser.description != null && !currentUser.description.trim().isEmpty()
                            ? currentUser.description.trim()
                            : dash);
        }

        bindProfilePhoto();

        loadLessonCostSummary();
    }

    private void bindProfilePhoto() {
        if (currentUser == null) {
            return;
        }
        Object glideModel = ProfilePhotoStore.displayModel(
                requireContext(), currentUser.id, currentUser.profileImageUrl);
        if (glideModel != null) {
            try {
                com.bumptech.glide.request.RequestOptions options =
                        new com.bumptech.glide.request.RequestOptions().centerCrop();
                if (glideModel instanceof File) {
                    options = options.signature(new ObjectKey(((File) glideModel).lastModified()));
                }
                Glide.with(requireContext())
                        .load(glideModel)
                        .apply(options)
                        .into(profilePhoto);
                profilePhoto.setVisibility(View.VISIBLE);
                profilePhotoAddLabel.setVisibility(View.GONE);
                return;
            } catch (Exception ignored) {
            }
        }
        profilePhoto.setVisibility(View.VISIBLE);
        profilePhoto.setImageDrawable(null);
        profilePhotoAddLabel.setVisibility(View.VISIBLE);
    }

    private void loadLessonCostSummary() {
        if (profileLessonsCostToday == null || profileLessonsCostTotal == null) {
            return;
        }
        User u = sessionManager.getUserSession();
        if (u == null || u.id == null || u.id.isEmpty()) {
            profileLessonsCostProgress.setVisibility(View.GONE);
            profileLessonsCostToday.setText(getString(R.string.profile_lessons_cost_today, "0"));
            profileLessonsCostTotal.setText(getString(R.string.profile_lessons_cost_total, "0"));
            return;
        }
        profileLessonsCostProgress.setVisibility(View.VISIBLE);
        LessonRepository repo = new LessonRepository(requireContext());
        ZoneId zone = ZoneId.systemDefault();
        if (u.isTeacher()) {
            repo.listMyLessonsAsTeacher(u.id, res -> {
                if (!isAdded() || getContext() == null) {
                    return;
                }
                profileLessonsCostProgress.setVisibility(View.GONE);
                ArrayList<Lesson> list = res.success && res.data != null ? res.data : new ArrayList<>();
                applyCostTexts(list, zone);
            });
        } else {
            repo.loadStudentSchedule(u.id, res -> {
                if (!isAdded() || getContext() == null) {
                    return;
                }
                profileLessonsCostProgress.setVisibility(View.GONE);
                ArrayList<Lesson> list = res.success && res.data != null ? res.data : new ArrayList<>();
                applyCostTexts(list, zone);
            });
        }
    }

    private void applyCostTexts(ArrayList<Lesson> list, ZoneId zone) {
        double[] sums = LessonCostHelper.sumTodayAndTotal(list, zone);
        String t = LessonCostHelper.formatAmount(sums[0]);
        String tot = LessonCostHelper.formatAmount(sums[1]);
        profileLessonsCostToday.setText(getString(R.string.profile_lessons_cost_today, t));
        profileLessonsCostTotal.setText(getString(R.string.profile_lessons_cost_total, tot));
    }

    private void applyRoleCardStyle() {
        if (currentUser == null) return;

        int accent = ContextCompat.getColor(requireContext(),
                currentUser.isTeacher() ? R.color.profile_teacher_accent : R.color.profile_student_accent);
        profileRole.setTextColor(accent);

        if (profileMainCard != null) {
            profileMainCard.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.background));
        }
    }

    private void applyPickedPhoto(Uri uri) {
        if (currentUser == null || currentUser.id == null || currentUser.id.isEmpty()) {
            Toast.makeText(requireContext(), R.string.photo_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ProfilePhotoStore.copyFromUri(requireContext(), currentUser.id, uri)) {
            Toast.makeText(requireContext(), R.string.photo_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        File local = ProfilePhotoStore.getPhotoFile(requireContext(), currentUser.id);
        if (local == null || !local.exists()) {
            Toast.makeText(requireContext(), R.string.photo_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        final String localPath = local.getAbsolutePath();
        currentUser.profileImageUrl = localPath;
        currentUser.updateTimestamp();
        sessionManager.saveUserSession(currentUser);
        Glide.with(requireContext())
                .load(local)
                .centerCrop()
                .signature(new ObjectKey(local.lastModified()))
                .into(profilePhoto);
        profilePhoto.setVisibility(View.VISIBLE);
        profilePhotoAddLabel.setVisibility(View.GONE);
        Uri uploadUri = Uri.fromFile(local);
        userRepository.updateUser(currentUser, uploadUri, result -> {
            if (getActivity() == null || !isAdded()) {
                return;
            }
            if (result.success && result.customData instanceof User) {
                User u = (User) result.customData;
                copySessionProfileExtras(currentUser, u);
                u.profileImageUrl = localPath;
                currentUser = u;
                sessionManager.saveUserSession(currentUser);
            }
            if (!result.success && result.message != null) {
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
            return;
        }
        launchCamera();
    }

    private void launchCamera() {
        try {
            File photoFile = new File(requireContext().getCacheDir(),
                    "profile_capture_" + System.currentTimeMillis() + ".jpg");
            cameraImageUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);
            takePicture.launch(cameraImageUri);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "تعذر فتح الكاميرا", Toast.LENGTH_SHORT).show();
        }
    }

    private static String formatMultilineSubjects(String stored, String emptyDash) {
        if (stored == null || stored.trim().isEmpty()) {
            return emptyDash;
        }
        String t = stored.trim().replace('\r', '\n');
        String[] lines = t.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("• ").append(line);
        }
        return sb.length() > 0 ? sb.toString() : emptyDash;
    }

    private void setupClickListeners() {
        profileEditBtn.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ProfileEditActivity.class));
        });

        profilePickGalleryBtn.setOnClickListener(v -> pickImage.launch("image/*"));
        profilePickCameraBtn.setOnClickListener(v -> openCamera());
        profilePhotoClick.setOnClickListener(v -> pickImage.launch("image/*"));
        
        profileLogoutBtn.setOnClickListener(v -> {
            sessionManager.clearSession();
            // إعادة تشغيل النشاط الرئيسي للعودة لشاشة تسجيل الدخول
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // إعادة تحميل البيانات عند العودة من شاشة التعديل
        setupUserProfile();
    }
}
