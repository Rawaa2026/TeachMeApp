package com.example.rawaaproject.util;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rawaaproject.R;
import com.example.rawaaproject.data.SessionManager;
import com.example.rawaaproject.models.User;

/**
 * يطبّق ثيم المعلم (أزرق) أو الطالب (أخضر) قبل {@code super.onCreate} عند وجود جلسة.
 */
public final class RoleThemeHelper {

    private RoleThemeHelper() {
    }

    public static void applyForLoggedInUser(AppCompatActivity activity) {
        SessionManager sm = new SessionManager(activity.getApplicationContext());
        if (!sm.isLoggedIn()) {
            return;
        }
        User u = sm.getCurrentUser();
        if (u == null) {
            return;
        }
        if (u.isTeacher()) {
            activity.setTheme(R.style.Theme_RawaaProject_Teacher);
        } else if (u.isStudent()) {
            activity.setTheme(R.style.Theme_RawaaProject_Student);
        }
    }
}
