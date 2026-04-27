package com.example.rawaaproject.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import android.os.Handler;
import android.os.Looper;

import com.example.rawaaproject.LinkToDb.DALAppWriteConnection;
import com.example.rawaaproject.LinkToDb.DALAppWriteConnection.FileInfo;
import com.example.rawaaproject.models.User;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private static final String TABLE_USERS = "users";
    /**
     * حقل password في مستند مجموعة users بـ Appwrite (مطلوب بالـ schema) — ليس كلمة مرور الحساب؛
     * المصادقة عبر Appwrite Account. القيمة ثابتة كما في المستندات الموجودة.
     */
    private static final String APPWRITE_USERS_PASSWORD_PLACEHOLDER = "******";
    private final DALAppWriteConnection dal;
    private final Context context;
    private final Executor ioExecutor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public UserRepository(Context context) {
        this.context = context;
        this.dal = new DALAppWriteConnection(context);
    }
    
    public User getUserById(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        try {
            DALAppWriteConnection.OperationResult<User> direct =
                    dal.getDataById(TABLE_USERS, userId, null, User.class);
            if (direct.success && direct.data != null) {
                return direct.data;
            }
            DALAppWriteConnection.OperationResult<ArrayList<User>> list =
                    dal.getData(TABLE_USERS, null, User.class);
            if (list.success && list.data != null) {
                for (User u : list.data) {
                    if (userId.equals(u.id) || userId.equals(u.userId)) {
                        return u;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getUserById", e);
        }
        return null;
    }
    
    public void loginUser(String email, String password, AuthCallback callback) {
        ioExecutor.execute(() -> {
            DALAppWriteConnection.OperationResult<DALAppWriteConnection.UserData> apiResult =
                    dal.loginUser(email, password);
            DALAppWriteConnection.OperationResult<DALAppWriteConnection.UserData> out =
                    new DALAppWriteConnection.OperationResult<>(
                            apiResult.success,
                            apiResult.message,
                            apiResult.data);
            out.errorCode = apiResult.errorCode;
            if (apiResult.success && apiResult.data != null) {
                User profile = getUserById(apiResult.data.userId);
                if (profile == null) {
                    profile = new User(email, "", "", "student");
                    profile.id = apiResult.data.userId;
                    profile.fullName = apiResult.data.firstName != null && !apiResult.data.firstName.isEmpty()
                            ? apiResult.data.firstName
                            : (apiResult.data.email != null ? apiResult.data.email : "");
                }
                profile.email = email;
                out.customData = profile;
            }
            callback.onResult(out);
        });
    }

    public void registerUser(User user, Uri photoUri, AuthCallback callback) {
        if (user == null || !user.isValid()) {
            callback.onResult(new DALAppWriteConnection.OperationResult<>(
                    false, "بيانات التسجيل غير مكتملة أو غير صالحة"));
            return;
        }
        ioExecutor.execute(() -> {
            String[] parts = splitFullName(user.fullName);
            DALAppWriteConnection.OperationResult<DALAppWriteConnection.UserData> apiResult =
                    dal.createDefaultUser(
                            user.email,
                            user.password,
                            parts[0],
                            parts[1],
                            user.phoneNumber != null ? user.phoneNumber : "");

            DALAppWriteConnection.OperationResult<DALAppWriteConnection.UserData> out =
                    new DALAppWriteConnection.OperationResult<>(
                            apiResult.success,
                            apiResult.message,
                            apiResult.data);
            out.errorCode = apiResult.errorCode;

            if (apiResult.success && apiResult.data != null) {
                User sessionUser = new User();
                sessionUser.id = apiResult.data.userId;
                sessionUser.email = user.email;
                sessionUser.fullName = user.fullName;
                sessionUser.role = user.role;
                sessionUser.phoneNumber = user.phoneNumber;
                sessionUser.profileImageUrl = user.profileImageUrl;
                sessionUser.birthDate = user.birthDate;
                sessionUser.specialization = user.specialization;
                sessionUser.experience = user.experience;
                sessionUser.hourlyRate = user.hourlyRate;
                sessionUser.description = user.description;
                sessionUser.grade = user.grade;
                sessionUser.school = user.school;
                sessionUser.subjectsNeeded = user.subjectsNeeded;
                sessionUser.isActive = true;
                out.customData = sessionUser;
                persistUserRow(sessionUser);
            }
            callback.onResult(out);
        });
    }

    private static String[] splitFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new String[] { "", "" };
        }
        String t = fullName.trim();
        int i = t.indexOf(' ');
        if (i < 0) {
            return new String[] { t, "" };
        }
        return new String[] { t.substring(0, i), t.substring(i + 1).trim() };
    }
    
    public void updateUser(User user, Uri photoUri, AuthCallback callback) {
        if (user == null || user.id == null || user.id.isEmpty()) {
            deliver(callback, new DALAppWriteConnection.OperationResult<>(
                    false, "معرف المستخدم غير صالح"));
            return;
        }
        ioExecutor.execute(() -> {
            try {
                User working = copyForPersistence(user);
                working.isActive = true;

                if (photoUri != null) {
                    byte[] raw = readUriBytes(photoUri);
                    if (raw != null && raw.length > 0) {
                        String fileName = "profile_" + user.id + "_" + System.currentTimeMillis() + ".jpg";
                        DALAppWriteConnection.OperationResult<FileInfo> up =
                                dal.uploadFile(raw, fileName, "image/jpeg", null);
                        if (up.success && up.data != null && up.data.fileUrl != null
                                && !up.data.fileUrl.isEmpty()) {
                            working.profileImageUrl = up.data.fileUrl;
                        }
                    }
                }

                working.updateTimestamp();

                Map<String, Object> dataMap = buildUserAppwriteData(working);
                boolean docExists = dal.documentExists(TABLE_USERS, working.id, null);
                if (docExists) {
                    DALAppWriteConnection.OperationResult<Void> patch =
                            dal.patchDocumentData(TABLE_USERS, null, working.id, dataMap);
                    if (patch.success) {
                        applyWorkingToUser(user, working);
                        deliverSuccess(callback, user);
                        return;
                    }
                    String pm = patch.message != null ? patch.message : "فشل التحديث";
                    Log.e(TAG, "updateUser PATCH: " + pm);
                    deliver(callback, new DALAppWriteConnection.OperationResult<>(false, pm));
                    return;
                }

                DALAppWriteConnection.OperationResult<Void> created =
                        dal.createDocumentWithData(TABLE_USERS, null, working.id, dataMap);
                if (created.success) {
                    applyWorkingToUser(user, working);
                    deliverSuccess(callback, user);
                    return;
                }

                String cm = created.message != null ? created.message.toLowerCase(Locale.ROOT) : "";
                if (cm.contains("409") || cm.contains("already exists") || cm.contains("duplicate")
                        || cm.contains("document_already_exists")) {
                    DALAppWriteConnection.OperationResult<Void> patch =
                            dal.patchDocumentData(TABLE_USERS, null, working.id, dataMap);
                    if (patch.success) {
                        applyWorkingToUser(user, working);
                        deliverSuccess(callback, user);
                        return;
                    }
                    String pm2 = patch.message != null ? patch.message : "فشل التحديث";
                    Log.e(TAG, "updateUser PATCH after conflict: " + pm2);
                    deliver(callback, new DALAppWriteConnection.OperationResult<>(false, pm2));
                    return;
                }

                String cmFinal = created.message != null ? created.message : "فشل حفظ المستخدم";
                Log.e(TAG, "updateUser CREATE: " + cmFinal);
                deliver(callback, new DALAppWriteConnection.OperationResult<>(false, cmFinal));
            } catch (Exception e) {
                Log.e(TAG, "updateUser", e);
                deliver(callback, new DALAppWriteConnection.OperationResult<>(
                        false, e.getMessage() != null ? e.getMessage() : "خطأ غير متوقع"));
            }
        });
    }

    private void deliver(AuthCallback callback,
                         DALAppWriteConnection.OperationResult<DALAppWriteConnection.UserData> result) {
        mainHandler.post(() -> callback.onResult(result));
    }

    private void deliverSuccess(AuthCallback callback, User user) {
        DALAppWriteConnection.OperationResult<DALAppWriteConnection.UserData> ok =
                new DALAppWriteConnection.OperationResult<>(true, "تم التحديث بنجاح", null);
        ok.customData = user;
        deliver(callback, ok);
    }

    private static void applyWorkingToUser(User user, User working) {
        user.fullName = working.fullName;
        user.phoneNumber = working.phoneNumber;
        user.email = working.email;
        user.role = working.role;
        user.profileImageUrl = working.profileImageUrl;
        user.birthDate = working.birthDate;
        user.specialization = working.specialization;
        user.experience = working.experience;
        user.hourlyRate = working.hourlyRate;
        user.description = working.description;
        user.grade = working.grade;
        user.school = working.school;
        user.subjectsNeeded = working.subjectsNeeded;
        user.isActive = true;
        user.updateTimestamp();
    }

    private void persistUserRow(User u) {
        User row = copyForPersistence(u);
        row.updateTimestamp();
        Map<String, Object> dataMap = buildUserAppwriteData(row);
        DALAppWriteConnection.OperationResult<Void> res =
                dal.createDocumentWithData(TABLE_USERS, null, row.id, dataMap);
        if (!res.success) {
            Log.e(TAG, "persistUserRow: " + (res.message != null ? res.message : "(no message)"));
        }
    }

    /**
     * حقول متوقعة في مجموعة users بـ Appwrite (متوافقة مع الـ schema الحالي).
     */
    private static Map<String, Object> buildUserAppwriteData(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        // مطلوب في schema مجموعة users (يجب أن يطابق $id / documentId)
        if (u.id != null && !u.id.isEmpty()) {
            m.put("userId", u.id);
        }
        m.put("isActive", u.isActive);
        m.put("isVerified", false);
        m.put("password", APPWRITE_USERS_PASSWORD_PLACEHOLDER);
        long now = System.currentTimeMillis();
        long updatedAt = u.updatedAt > 0 ? u.updatedAt : now;
        m.put("updatedAt", updatedAt);
        if (u.email != null) {
            m.put("email", u.email);
        }
        if (u.fullName != null) {
            m.put("fullName", u.fullName);
        }
        if (u.role != null) {
            m.put("role", u.role);
        }
        if (u.phoneNumber != null) {
            m.put("phoneNumber", u.phoneNumber);
        }
        if (u.profileImageUrl != null && !u.profileImageUrl.isEmpty()) {
            m.put("profileImageUrl", u.profileImageUrl);
        }
        // تاريخ الميلاد يُحفظ محلياً في الجلسة فقط — مجموعة users في Appwrite لا تتضمن سمة birthDate
        return m;
    }

    private static User copyForPersistence(User u) {
        User c = new User();
        c.id = u.id;
        c.email = u.email;
        c.fullName = u.fullName;
        c.role = u.role;
        c.phoneNumber = u.phoneNumber;
        c.profileImageUrl = u.profileImageUrl;
        c.birthDate = u.birthDate;
        c.specialization = u.specialization;
        c.experience = u.experience;
        c.hourlyRate = u.hourlyRate;
        c.description = u.description;
        c.grade = u.grade;
        c.school = u.school;
        c.subjectsNeeded = u.subjectsNeeded;
        c.createdAt = u.createdAt > 0 ? u.createdAt : System.currentTimeMillis();
        c.updatedAt = u.updatedAt;
        c.isActive = true;
        return c;
    }

    private byte[] readUriBytes(Uri uri) {
        if (uri == null) {
            return null;
        }
        try {
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                String path = uri.getPath();
                if (path == null) {
                    return null;
                }
                try (FileInputStream fis = new FileInputStream(path);
                     ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
                    byte[] chunk = new byte[8192];
                    int n;
                    while ((n = fis.read(chunk)) != -1) {
                        buf.write(chunk, 0, n);
                    }
                    return buf.toByteArray();
                }
            }
            try (InputStream in = context.getContentResolver().openInputStream(uri);
                 ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
                if (in == null) {
                    return null;
                }
                byte[] chunk = new byte[8192];
                int n;
                while ((n = in.read(chunk)) != -1) {
                    buf.write(chunk, 0, n);
                }
                return buf.toByteArray();
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    public interface UserSearchCallback {
        void onUserFound(User user);
        void onUserNotFound();
        void onError(String error);
    }
    
    public void findUserByEmail(String email, UserSearchCallback callback) {
        // Implementation for finding user by email
        callback.onUserNotFound();
    }

    public interface TeachersListCallback {
        void onResult(boolean success, String message, ArrayList<User> teachers);
    }

    /** قائمة المعلمين؛ عند تمرير مادة يُصفّى حسب التخصص (نص أو قائمة مفصولة بفواصل). */
    public void listTeachers(@Nullable String subjectFilter, TeachersListCallback callback) {
        ioExecutor.execute(() -> {
            DALAppWriteConnection.OperationResult<ArrayList<User>> r =
                    dal.getData(TABLE_USERS, null, User.class);
            ArrayList<User> teachers = new ArrayList<>();
            if (r.success && r.data != null) {
                String needle = subjectFilter != null ? subjectFilter.trim() : "";
                for (User u : r.data) {
                    if (u == null || !"teacher".equals(u.role)) {
                        continue;
                    }
                    if (needle.isEmpty()) {
                        teachers.add(u);
                        continue;
                    }
                    String spec = u.specialization != null ? u.specialization : "";
                    if (specializationMatchesSubject(spec, needle)) {
                        teachers.add(u);
                    }
                }
            }
            boolean ok = r.success;
            String msg = r.message != null ? r.message : "";
            mainHandler.post(() -> callback.onResult(ok, msg, teachers));
        });
    }

    private static boolean specializationMatchesSubject(String specialization, String needle) {
        if (specialization == null || needle.isEmpty()) {
            return false;
        }
        String s = specialization.toLowerCase(Locale.ROOT);
        String n = needle.toLowerCase(Locale.ROOT);
        if (s.contains(n)) {
            return true;
        }
        String[] parts = s.split("[,،\\n;؛]+");
        for (String p : parts) {
            if (p.trim().toLowerCase(Locale.ROOT).contains(n)) {
                return true;
            }
        }
        return false;
    }
}
