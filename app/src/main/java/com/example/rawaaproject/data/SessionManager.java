package com.example.rawaaproject.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.example.rawaaproject.models.User;

/**
 * وحدة محسنة لإدارة جلسات المستخدم - تدعم حفظ بيانات المستخدم الكاملة
 */
public class SessionManager {

    private static final String PREFS_NAME = "rawaa_session";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_USER_JSON = "user_json";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";

    private final SharedPreferences prefs;
    private final Gson gson;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new GsonBuilder().create();
    }

    /** حفظ بيانات المستخدم الكاملة بعد تسجيل الدخول أو التسجيل */
    public void saveUserSession(User user) {
        if (user == null) return;
        
        String userJson = gson.toJson(user);
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_USER_JSON, userJson)
                .putString(KEY_USER_ID, user.id)
                .putString(KEY_USER_EMAIL, user.email)
                .putString(KEY_USER_ROLE, user.role != null ? user.role : "")
                .apply();
    }

    /** حفظ بيانات الجلسة (للتوافق مع الكود القديم) */
    public void saveLogin(String userId, String email, String role) {
        User user = new User();
        user.id = userId;
        user.email = email;
        user.role = role;
        saveUserSession(user);
    }

    /** الحصول على المستخدم الحالي */
    public User getCurrentUser() {
        if (!isLoggedIn()) return null;
        
        String userJson = prefs.getString(KEY_USER_JSON, null);
        if (userJson != null) {
            try {
                User user = gson.fromJson(userJson, User.class);
                // التحقق من صحة البيانات وتصحيحها
                return validateUser(user);
            } catch (Exception e) {
                // في حالة خطأ في تحليل JSON، نرجع بيانات أساسية
                return createBasicUser();
            }
        }
        return createBasicUser();
    }

    /**
     * التحقق من صحة بيانات المستخدم وتصحيحها
     */
    private User validateUser(User user) {
        if (user == null) return null;
        
        // التحقق من الحقول الأساسية
        if (user.id == null || user.id.isEmpty()) {
            user.id = getUserId();
        }
        
        if (user.email == null || user.email.isEmpty()) {
            user.email = getUserEmail();
        }
        
        if (user.role == null || user.role.isEmpty()) {
            user.role = getUserRole();
            if (user.role == null || user.role.isEmpty()) {
                user.role = "student"; // قيمة افتراضية
            }
        }
        
        // التحقق من التواريخ
        if (user.createdAt <= 0) {
            user.createdAt = System.currentTimeMillis();
        }
        
        if (user.updatedAt <= 0) {
            user.updatedAt = user.createdAt;
        }
        
        // التحقق من الحالة
        if (!user.isActive) {
            user.isActive = true;
        }
        
        return user;
    }

    /** الحصول على جلسة المستخدم (别名方法) */
    public User getUserSession() {
        return getCurrentUser();
    }

    /** إنشاء مستخدم أساسي من البيانات القديمة */
    private User createBasicUser() {
        User user = new User();
        user.id = getUserId();
        user.email = getUserEmail();
        user.role = getUserRole();
        return user;
    }

    /** تحديث بيانات المستخدم الحالي */
    public void updateCurrentUser(User user) {
        if (user != null && isLoggedIn()) {
            user.updateTimestamp();
            saveUserSession(user);
        }
    }

    /** مسح الجلسة (تسجيل الخروج) */
    public void clearSession() {
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .remove(KEY_USER_JSON)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_ROLE)
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "");
    }

    /** التحقق إذا كان المستخدم الحالي معلم */
    public boolean isCurrentUserTeacher() {
        return "teacher".equals(getUserRole());
    }

    /** التحقق إذا كان المستخدم الحالي طالب */
    public boolean isCurrentUserStudent() {
        return "student".equals(getUserRole());
    }

    /** الحصول على اسم العرض للمستخدم الحالي */
    public String getCurrentUserDisplayName() {
        User user = getCurrentUser();
        return user != null ? user.getDisplayName() : "";
    }
}
