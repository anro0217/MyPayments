package com.myapps.mypayments.utils;

import android.util.Log;
import android.widget.Toast;

import com.myapps.mypayments.R;
import com.myapps.mypayments.activities.MainActivity;

public class FinanceManager {
    private static FinanceManager instance;
    private double totalBalance;
    private double monthlyBalance;
    private double debtBalance;
    private double savesBalance;
    private final FirestoreManager firestoreManager;

    public FinanceManager() {
        firestoreManager = FirestoreManager.getInstance();
        firestoreManager.getUserDocument().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                totalBalance = documentSnapshot.getDouble("totalBalance") != null ? documentSnapshot.getDouble("totalBalance") : 0.0;
                monthlyBalance = documentSnapshot.getDouble("monthlyBalance") != null ? documentSnapshot.getDouble("monthlyBalance") : 0.0;
                debtBalance = documentSnapshot.getDouble("debtBalance") != null ? documentSnapshot.getDouble("debtBalance") : 0.0;
                savesBalance = documentSnapshot.getDouble("savesBalance") != null ? documentSnapshot.getDouble("savesBalance") : 0.0;
            }
        });
    }

    public static synchronized FinanceManager getInstance() {
        if (instance == null) {
            instance = new FinanceManager();
        }
        return instance;
    }

    private void initializeBalances(Runnable onBalancesUpdated) {
        firestoreManager.getBalances(new FirestoreManager.OnBalancesRetrievedListener() {
            @Override
            public void onBalancesRetrieved(double totalBalance, double monthlyBalance, double debtBalance, double savesBalance) {
                FinanceManager.this.totalBalance = totalBalance;
                FinanceManager.this.monthlyBalance = monthlyBalance;
                FinanceManager.this.debtBalance = debtBalance;
                FinanceManager.this.savesBalance = savesBalance;
                if (onBalancesUpdated != null) {
                    onBalancesUpdated.run();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("FinanceManager", "Error retrieving balances", e);
            }
        });
    }

    public void performTransactionByNoteEdit(int categoryIndex, double amount) {
        performTransactionByNoteEdit(categoryIndex, amount, false);
    }

    public void performTransactionByNoteEdit(int categoryIndex, double amount, boolean isDelete) {
        initializeBalances(() -> {
            switch (categoryIndex) {
                case 1: // Bevételek
                    addIncome(amount);
                    break;
                case 3: // Tartozások
                    addDebt(amount);
                    break;
                case 4: // Ismétlődő havi kiadások
                    /*TODO: metódus, ami minden hónap megh. napján levonja egyszer a listán
                       szereplőket */
                    break;
                case 5: // Aktuális havi kiadások
                    addExpense(amount, false);
                    break;
                case 6: // Nagyobb összegű kiadások
                    if (totalBalance - amount < monthlyBalance) {
                        addExpense(amount, false);

                    } else {
                        addExpense(amount, true);
                    }
                    break;
                case 7: // Havi fizetések
                    setMonthlyBalance(amount, isDelete);
                    break;
                default:
                    break;
            }
        });
    }

    public void performTransactionByOperation(MainActivity.OperationType operationType, double amount) {
        initializeBalances(() -> {
            switch (operationType) {
                case OTHER_INCOME:
                    addIncome(amount);
                    break;
                case EXPENSE:
                    addExpense(amount, false);
                    break;
                case LARGER_EXPENSE:
                    addExpense(amount, true);
                    break;
                case RECURRING_MONTHLY_EXPENSE:
                    /*TODO: havonta ismétlődő kiadások implementálása*/
                    break;
                case DEBT:
                    addDebt(amount);
                    break;
                case NEW_MONTHLY_BALANCE:
                    setMonthlyBalance(amount, false);
                    break;
                default:
                    break;
            }
        });
    }

    public void addIncome(double amount) {
        totalBalance += amount;
        updateBalances();
    }

    public void addExpense(double amount, boolean isLarger) {
        if(!isLarger){
            monthlyBalance -= amount;
        }
        totalBalance -= amount;
        updateBalances();
    }

    public void addDebt(double amount) {
        totalBalance -= amount;
        monthlyBalance -= amount;
        debtBalance += amount;
        updateBalances();
    }

    public void addSavings(double amount) {
        savesBalance += amount;
        updateBalances();
    }

    public void setMonthlyBalance(double amount, boolean isDelete) {
        if (!isDelete){
            addSavings(monthlyBalance);
            monthlyBalance = amount;
        } else {
            monthlyBalance += amount + savesBalance;
            savesBalance = 0.0;
        }
        totalBalance += amount;
        updateBalances();
    }

    private void updateBalances() {
        FirestoreManager.getInstance().updateBalances(totalBalance, monthlyBalance, debtBalance, savesBalance, new FirestoreManager.OnFirestoreOperationCompleteListener() {
            @Override
            public void onSuccess() {
                Log.d("FinanceManager", "Balances updated successfully including savings balance");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("FinanceManager", "Failed to update balances", e);
            }
        });
    }

}
