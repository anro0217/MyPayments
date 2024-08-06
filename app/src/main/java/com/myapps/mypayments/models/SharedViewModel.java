package com.myapps.mypayments.models;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.myapps.mypayments.utils.FirestoreManager;

public class SharedViewModel extends ViewModel {
    private MutableLiveData<Double> totalBalance = new MutableLiveData<>();
    private MutableLiveData<Double> monthlyBalance = new MutableLiveData<>();
    private MutableLiveData<Double> debtBalance = new MutableLiveData<>();
    private MutableLiveData<Double> savesBalance = new MutableLiveData<>();
    private MutableLiveData<Boolean> balancesVisibility = new MutableLiveData<>();
    private FirestoreManager firestoreManager = FirestoreManager.getInstance();
    private SharedPreferences sharedPref;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    public SharedViewModel() {
        firestoreManager.listenToBalanceUpdates(new FirestoreManager.OnBalancesRetrievedListener() {
            @Override
            public void onBalancesRetrieved(double totalBalanceValue, double monthlyBalanceValue, double debtBalanceValue, double savesBalance) {
                totalBalance.postValue(totalBalanceValue);
                monthlyBalance.postValue(monthlyBalanceValue);
                debtBalance.postValue(debtBalanceValue);
            }

            @Override
            public void onError(Exception e) {
                // Log error or handle it
            }
        });
    }

    public void setContext(Context context) {
        if(sharedPref == null) {
            sharedPref = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
            balancesVisibility.postValue(sharedPref.getBoolean("BalancesVisible", true));

            preferenceChangeListener = (sharedPreferences, key) -> {
                if ("BalancesVisible".equals(key)) {
                    balancesVisibility.postValue(sharedPreferences.getBoolean(key, true));
                }
            };
            sharedPref.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
    }

    public void toggleBalancesVisibility() {
        if (sharedPref != null) {
            boolean isCurrentlyVisible = sharedPref.getBoolean("BalancesVisible", true);
            sharedPref.edit().putBoolean("BalancesVisible", !isCurrentlyVisible).apply();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if(sharedPref != null && preferenceChangeListener != null) {
            // Leiratkozás a SharedPreferences változásfigyelőjéről a ViewModel megsemmisítésekor
            sharedPref.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
    }

    public LiveData<Double> getTotalBalance() {
        return totalBalance;
    }

    public LiveData<Double> getMonthlyBalance() {
        return monthlyBalance;
    }

    public LiveData<Double> getDebtBalance() {
        return debtBalance;
    }
    public LiveData<Double> getSavesBalance() { return savesBalance;}

    public LiveData<Boolean> getBalancesVisibility() {
        return balancesVisibility;
    }
}
