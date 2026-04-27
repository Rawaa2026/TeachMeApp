package com.example.rawaaproject.util;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * نسخ صورة الملف الشخصي إلى تخزين داخلي ثابت حتى تبقى بعد إعادة تشغيل التطبيق.
 * روابط content:// من المعرض لا تُعتمد للتخزين طويل الأمد.
 */
public final class ProfilePhotoStore {

    private static final String SUBDIR = "profile_photos";

    private ProfilePhotoStore() {}

    public static File getPhotoFile(Context context, String userId) {
        if (context == null || userId == null || userId.isEmpty()) {
            return null;
        }
        File dir = new File(context.getFilesDir(), SUBDIR);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        String safe = userId.replaceAll("[^a-zA-Z0-9_-]", "_");
        return new File(dir, safe + ".jpg");
    }

    /**
     * نسخ محتوى {@code sourceUri} إلى ملف الملف الشخصي المحلي للمستخدم.
     */
    public static boolean copyFromUri(Context context, String userId, Uri sourceUri) {
        if (context == null || sourceUri == null) {
            return false;
        }
        File dest = getPhotoFile(context, userId);
        if (dest == null) {
            return false;
        }
        try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
             FileOutputStream out = new FileOutputStream(dest)) {
            if (in == null) {
                return false;
            }
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
            return dest.length() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static File asLocalFile(String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isEmpty()) {
            return null;
        }
        if (profileImageUrl.startsWith("file://")) {
            String path = Uri.parse(profileImageUrl).getPath();
            if (path == null) {
                return null;
            }
            File f = new File(path);
            return f.exists() ? f : null;
        }
        if (profileImageUrl.startsWith("/")) {
            File f = new File(profileImageUrl);
            return f.exists() ? f : null;
        }
        return null;
    }

    /** مسار ملف داخلي موجود — يبقى صالحاً بعد إعادة التشغيل */
    public static boolean isPersistentLocalPath(String profileImageUrl) {
        return asLocalFile(profileImageUrl) != null;
    }

    /** نموذج تحميل مناسب لـ Glide: {@link File} أو سلسلة http(s) أو السلسلة كما هي */
    public static Object glideModel(String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isEmpty()) {
            return null;
        }
        if (profileImageUrl.startsWith("http://") || profileImageUrl.startsWith("https://")) {
            return profileImageUrl;
        }
        File f = asLocalFile(profileImageUrl);
        if (f != null) {
            return f;
        }
        return profileImageUrl;
    }
}
