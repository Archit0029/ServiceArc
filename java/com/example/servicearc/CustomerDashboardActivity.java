package com.example.servicearc;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CustomerDashboardActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomMenu;
    private CustomerPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        viewPager = findViewById(R.id.viewPager);
        bottomMenu = findViewById(R.id.bottomNav);

        pagerAdapter = new CustomerPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        bottomMenu.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) viewPager.setCurrentItem(0, false);
            else if (id == R.id.nav_explore) viewPager.setCurrentItem(1, false);
            else if (id == R.id.nav_booking) viewPager.setCurrentItem(2, false);
            else if (id == R.id.nav_chat) viewPager.setCurrentItem(3, false);
            else if (id == R.id.nav_profile) viewPager.setCurrentItem(4, false);
            return true;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0: bottomMenu.setSelectedItemId(R.id.nav_home); break;
                    case 1: bottomMenu.setSelectedItemId(R.id.nav_explore); break;
                    case 2: bottomMenu.setSelectedItemId(R.id.nav_booking); break;
                    case 3: bottomMenu.setSelectedItemId(R.id.nav_chat); break;
                    case 4: bottomMenu.setSelectedItemId(R.id.nav_profile); break;
                }
            }
        });
    }

    public void openExploreWithFilter(String category) {
        viewPager.setCurrentItem(1, false);
        // Find the ExploreFragment and apply the filter
        ExploreFragment fragment = (ExploreFragment) getSupportFragmentManager().findFragmentByTag("f1");
        if (fragment != null) {
            fragment.filterByCategory(category);
        } else {
            // If fragment is not yet created, we might need a different approach (e.g., shared ViewModel)
            // But usually 'f1' works for ViewPager2's second fragment.
        }
    }
}
