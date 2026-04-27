package com.example.rawaaproject.data;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.example.rawaaproject.LinkToDb.DALAppWriteConnection;
import com.example.rawaaproject.models.User;

/**
 * وحدة المصادقة المحسنة - تستخدم UserRepository والنموذج الجديد للمستخدم
 */
public class AuthRepository {

    private final UserRepository userRepository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AuthRepository(Context context) {
        this.userRepository = new UserRepository(context);
    }

    /**
     * تسجيل الدخول بالبريد وكلمة المرور
     */
    public void login(String email, String password, AuthCallback callback) {
        userRepository.loginUser(email, password, result -> {
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    /**
     * تسجيل مستخدم جديد مع البيانات الأساسية فقط
     */
    public void register(String role, String fullName, String email, String password,
                         String phoneNumber, String birthDate, String specialization,
                         Uri photoUri, AuthCallback callback) {
        
        // إنشاء كائن المستخدم المبسط
        User user = new User(email, password, fullName, role);
        user.phoneNumber = phoneNumber;
        if (birthDate != null && !birthDate.isEmpty()) {
            user.birthDate = birthDate;
        }

        userRepository.registerUser(user, photoUri, result -> {
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    /**
     * تسجيل مستخدم جديد مع كائن User كامل
     */
    public void registerUser(User user, Uri photoUri, AuthCallback callback) {
        userRepository.registerUser(user, photoUri, result -> {
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    /**
     * البحث عن مستخدم بالبريد
     */
    public void findUserByEmail(String email, UserRepository.UserSearchCallback callback) {
        userRepository.findUserByEmail(email, callback);
    }

    /**
     * جلب المستخدم بالمعرف
     */
    public User getUserById(String userId) {
        return userRepository.getUserById(userId);
    }
}
