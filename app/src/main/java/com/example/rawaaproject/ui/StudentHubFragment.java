package com.example.rawaaproject.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.rawaaproject.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * مركز الطالب: تصفح الدروس، المعلمون، الجدول — ضمن تبويبات.
 */
public class StudentHubFragment extends Fragment {

    @Nullable
    private TabLayoutMediator tabMediator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_role_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TabLayout tabs = view.findViewById(R.id.hub_tabs);
        ViewPager2 pager = view.findViewById(R.id.hub_pager);
        pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return new StudentLessonsFragment();
                    case 1:
                        return new StudentTeachersFragment();
                    case 2:
                    default:
                        return new ScheduleFragment();
                }
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        });
        tabMediator = new TabLayoutMediator(tabs, pager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.nav_browse_lessons);
            } else if (position == 1) {
                tab.setText(R.string.nav_teachers_by_subject);
            } else {
                tab.setText(R.string.nav_schedule);
            }
        });
        tabMediator.attach();
    }

    @Override
    public void onDestroyView() {
        if (tabMediator != null) {
            tabMediator.detach();
            tabMediator = null;
        }
        super.onDestroyView();
    }
}
