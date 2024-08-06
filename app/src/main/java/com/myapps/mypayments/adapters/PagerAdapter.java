package com.myapps.mypayments.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.myapps.mypayments.fragments.MonthlyBalanceFragment;
import com.myapps.mypayments.fragments.TotalBalanceFragment;
import com.myapps.mypayments.models.SharedViewModel;

public class PagerAdapter extends FragmentStateAdapter {
    private static final int NUM_PAGES = 2;
    public PagerAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return a NEW fragment instance in createFragment(int)
        int pageIndex = position % NUM_PAGES;
        if (pageIndex == 0) {
            return new TotalBalanceFragment();
        } else {
            return new MonthlyBalanceFragment();
        }
    }

    @Override
    public int getItemCount() {
        return Integer.MAX_VALUE;
    }
}
