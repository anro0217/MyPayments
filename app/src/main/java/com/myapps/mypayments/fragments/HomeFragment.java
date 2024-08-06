package com.myapps.mypayments.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.myapps.mypayments.R;
import com.myapps.mypayments.adapters.PagerAdapter;
import com.myapps.mypayments.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // ViewPager2 és PagerAdapter beállítása marad
        ViewPager2 viewPager = binding.viewPager;
        PagerAdapter pagerAdapter = new PagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(Integer.MAX_VALUE / 2, false);

        viewPager.setOffscreenPageLimit(1);

        return root;
    }
    public ViewPager2 getViewPager() {
        return binding.viewPager;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}