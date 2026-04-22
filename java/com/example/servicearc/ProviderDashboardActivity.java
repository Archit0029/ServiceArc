package com.example.servicearc;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProviderDashboardActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_dashboard);

        viewPager = findViewById(R.id.viewPagerProvider);
        bottomNav = findViewById(R.id.bottomNav);

        ProviderPagerAdapter adapter = new ProviderPagerAdapter(this);
        viewPager.setAdapter(adapter);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_provider_home) {
                if (viewPager.getCurrentItem() != 0) viewPager.setCurrentItem(0, true);
            } else if (id == R.id.nav_provider_bookings) {
                if (viewPager.getCurrentItem() != 1) viewPager.setCurrentItem(1, true);
            } else if (id == R.id.nav_provider_chat) {
                if (viewPager.getCurrentItem() != 2) viewPager.setCurrentItem(2, true);
            } else if (id == R.id.nav_provider_profile) {
                if (viewPager.getCurrentItem() != 3) viewPager.setCurrentItem(3, true);
            }
            return true;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int itemId = -1;
                switch (position) {
                    case 0: itemId = R.id.nav_provider_home; break;
                    case 1: itemId = R.id.nav_provider_bookings; break;
                    case 2: itemId = R.id.nav_provider_chat; break;
                    case 3: itemId = R.id.nav_provider_profile; break;
                }
                if (itemId != -1 && bottomNav.getSelectedItemId() != itemId) {
                    bottomNav.setSelectedItemId(itemId);
                }
            }
        });
    }

    /**
     * Helper method to programmatically change tabs from fragments
     */
    public void navigateToTab(int menuId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(menuId);
        }
    }
}
