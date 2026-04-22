package com.example.servicearc;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ProviderPagerAdapter extends FragmentStateAdapter {

    public ProviderPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new ProviderHomeFragment();
            case 1: return new ProviderBookingsFragment();
            case 2: return new ProviderChatFragment();
            case 3: return new ProviderProfileFragment();
            default: return new ProviderHomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
