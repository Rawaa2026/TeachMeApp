package com.example.rawaaproject.util;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.DimenRes;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public final class SafeAreaHelper {

    private static final int HEADER_INSETS = WindowInsetsCompat.Type.statusBars()
            | WindowInsetsCompat.Type.displayCutout();

    private SafeAreaHelper() {
    }

    public static void enableEdgeToEdge(Activity activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
    }

    public static void applyHeaderStatusBarInset(View headerRoot) {
        if (headerRoot == null) {
            return;
        }
        final int paddingStart = headerRoot.getPaddingStart();
        final int paddingEnd = headerRoot.getPaddingEnd();
        final int paddingBottom = headerRoot.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(headerRoot, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(HEADER_INSETS);
            int topInset = insets.top;
            if (topInset == 0) {
                topInset = getFallbackStatusBarHeight(view);
            }
            view.setPaddingRelative(paddingStart, topInset, paddingEnd, paddingBottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(headerRoot);
    }

    public static void applyNavigationBarPadding(View view) {
        if (view == null) {
            return;
        }
        final int paddingStart = view.getPaddingStart();
        final int paddingTop = view.getPaddingTop();
        final int paddingEnd = view.getPaddingEnd();
        final int paddingBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPaddingRelative(
                    paddingStart,
                    paddingTop,
                    paddingEnd,
                    paddingBottom + navigationBars.bottom
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    public static void applyNavigationBarMargin(View view, @DimenRes int baseMarginRes) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        final int baseMargin = view.getResources().getDimensionPixelSize(baseMarginRes);
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = baseMargin + navigationBars.bottom;
            v.setLayoutParams(params);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    private static int getFallbackStatusBarHeight(View view) {
        int resourceId = view.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return view.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
}
