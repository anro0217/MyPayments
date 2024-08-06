package com.myapps.mypayments.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.myapps.mypayments.R;
import com.myapps.mypayments.models.SharedViewModel;
import com.myapps.mypayments.utils.FirestoreManager;
import com.myapps.mypayments.utils.NumberManager;

public class MonthlyBalanceFragment extends Fragment {

    private TextView monthlyBalanceText;
    private TextView debtBalanceText;
    private SharedViewModel viewModel;
    private Button toggleVisibilityButton;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        viewModel.setContext(context); // Beállítjuk a kontextust, amikor a Fragment csatolódik
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.monthly_balance_fragment, container, false);
        monthlyBalanceText = view.findViewById(R.id.monthlyBalanceText);
        debtBalanceText = view.findViewById(R.id.debtBalanceText);

        toggleVisibilityButton = view.findViewById(R.id.toggleVisibilityButtonMonthly);
        toggleVisibilityButton.setOnClickListener(v -> viewModel.toggleBalancesVisibility());

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        viewModel.getMonthlyBalance().observe(getViewLifecycleOwner(), balance -> {
            monthlyBalanceText.setText(NumberManager.formatNumber(balance));
            view.setVisibility(View.VISIBLE);
        });
        viewModel.getDebtBalance().observe(getViewLifecycleOwner(), balance -> {
            debtBalanceText.setText(NumberManager.formatNumber(balance));
        });

        viewModel.getBalancesVisibility().observe(getViewLifecycleOwner(), isVisible -> {
            monthlyBalanceText.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            debtBalanceText.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        });

        return view;
    }
}