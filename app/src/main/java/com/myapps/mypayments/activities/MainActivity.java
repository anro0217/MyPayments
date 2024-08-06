package com.myapps.mypayments.activities;



import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.net.Uri;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.firebase.firestore.DocumentSnapshot;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.myapps.mypayments.R;
import com.myapps.mypayments.databinding.ActivityMainBinding;
import com.myapps.mypayments.fragments.HomeFragment;
import com.myapps.mypayments.models.RecurringExpenseWorker;
import com.myapps.mypayments.utils.FinanceManager;
import com.myapps.mypayments.utils.FirestoreManager;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private FirestoreManager firestoreManager;
    private FinanceManager financeManager;
    private HomeFragment homeFragment;

    public enum OperationType {
        OTHER_INCOME,
        EXPENSE,
        LARGER_EXPENSE,
        COMMON_EXPENSE,
        RECURRING_MONTHLY_EXPENSE,
        DEBT,
        NEW_MONTHLY_BALANCE
    }

    public enum CategoryType {
        SIMPLE_NOTE, //nem törölhető, a sorrend indexelés miatt (ordial)
        OTHER_INCOMES,
        MONTHLY_SAVES,
        DEBTS,
        RECURRING_MONTHLY_EXPENSES,
        CURRENT_MONTHLY_EXPENSES,
        LARGER_EXPENSES,
        MONTHLY_PAYMENTS
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("Settings", Activity.MODE_PRIVATE);
        String language = prefs.getString("My_Lang", "");
        setLocale(language);

        // Itt ellenőrizzük, hogy van-e bejelentkezett felhasználó
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Nincs bejelentkezett felhasználó, indítsuk el a LoginActivity-t
            Intent intent = new Intent(this, LoginActivity.class);
            // FLAG_ACTIVITY_NEW_TASK és FLAG_ACTIVITY_CLEAR_TASK zászlók használata megakadályozza, hogy
            // a felhasználó visszatérhessen a MainActivity-hez a 'back' gomb használatával.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {

            ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            firestoreManager = FirestoreManager.getInstance();
            financeManager = new FinanceManager();

            setSupportActionBar(binding.appBarMain.toolbar);

            binding.appBarMain.fab.setOnClickListener(view -> {
                int selectedMenuItemId = getSelectedMenuItemId();
                if (selectedMenuItemId == R.id.nav_home) {
                    homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment).getChildFragmentManager().getFragments().get(0);
                    if (homeFragment != null && homeFragment.isVisible()) {
                        int position = homeFragment.getViewPager().getCurrentItem() % 2;
                        if (position == 0) {
                            showTotalBalanceDialog(view);
                        } else if (position == 1) {
                            showMonthlyBalanceDialog(view);
                        }
                    }
                } else if (selectedMenuItemId == R.id.nav_notes) {
                    Intent intent = new Intent(MainActivity.this, NewNoteActivity.class);
                    startActivity(intent);
                } else if (selectedMenuItemId == R.id.nav_others) {
                    Calendar today = Calendar.getInstance();
                    DatePickerDialog dialog = new DatePickerDialog(
                            this, null,
                            today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)
                    );
                    dialog.show();
                    //TODO: valutaváltó funkciója a FAB-ra
                }

            });

            DrawerLayout drawer = binding.drawerLayout;
            NavigationView navigationView = binding.navView;
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_home, R.id.nav_notes, R.id.nav_others)
                    .setOpenableLayout(drawer)
                    .build();
            NavHostFragment navHostFragment =
                    (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);

            updateNavHeader();
        }
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    private void showTotalBalanceDialog(View view) {
        PopupMenu popup = new PopupMenu(MainActivity.this, view);
        popup.getMenuInflater().inflate(R.menu.fab_total_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.other_income) {
                showOperationDialog(OperationType.OTHER_INCOME, CategoryType.OTHER_INCOMES);
                return true;
            } else if (id == R.id.larger_expense) {
                showOperationDialog(OperationType.LARGER_EXPENSE, CategoryType.LARGER_EXPENSES, CategoryType.CURRENT_MONTHLY_EXPENSES);
                return true;
            } else if (id == R.id.recurring_monthly_expense) {
                showOperationDialog(OperationType.RECURRING_MONTHLY_EXPENSE, CategoryType.RECURRING_MONTHLY_EXPENSES);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showMonthlyBalanceDialog(View view) {
        PopupMenu popup = new PopupMenu(MainActivity.this, view);
        popup.getMenuInflater().inflate(R.menu.fab_monthly_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.expense) {
                showOperationDialog(OperationType.EXPENSE, CategoryType.CURRENT_MONTHLY_EXPENSES);
                return true;
            } else if (id == R.id.common_expense) {
                showOperationDialog(OperationType.COMMON_EXPENSE, CategoryType.CURRENT_MONTHLY_EXPENSES, CategoryType.DEBTS);
                return true;
            } else if (id == R.id.dept) {
                showOperationDialog(OperationType.DEBT, CategoryType.DEBTS);
                return true;
            } else if (id == R.id.new_monthly_balance) {
                showOperationDialog(OperationType.NEW_MONTHLY_BALANCE, CategoryType.MONTHLY_SAVES, CategoryType.MONTHLY_PAYMENTS);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showOperationDialog(OperationType operationType, CategoryType categoryType1) {
        showOperationDialog(operationType, categoryType1, CategoryType.SIMPLE_NOTE);
    }
    private void showOperationDialog(OperationType operationType, CategoryType categoryType1, CategoryType categoryType2) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getTitleForOperationType(operationType));

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_dialog_layout, null);
        builder.setView(dialogView);

        EditText amountInput = dialogView.findViewById(R.id.amount_input);
        EditText descriptionInput = dialogView.findViewById(R.id.description_input);
        EditText ownShareInput = dialogView.findViewById(R.id.own_share_input);
        EditText othersShareInput = dialogView.findViewById(R.id.others_share_input);
        Switch sharedSwitch = dialogView.findViewById(R.id.shared_switch);
        LinearLayout sharedAmountInput = dialogView.findViewById(R.id.shared_input_container);

        // Megfelelő mezők megjelenítése a művelettípustól függően
        configureDialogFields(operationType, sharedSwitch, sharedAmountInput, descriptionInput);

        builder.setPositiveButton(R.string.approve, (dialog, which) -> {
            boolean allFieldsFilled = !amountInput.getText().toString().isEmpty() &&
                    (!descriptionInput.getText().toString().isEmpty() && (operationType != OperationType.COMMON_EXPENSE &&
                            operationType != OperationType.NEW_MONTHLY_BALANCE || operationType == OperationType.COMMON_EXPENSE &&
                            !sharedSwitch.isChecked() || operationType == OperationType.COMMON_EXPENSE &&
                            sharedSwitch.isChecked() && (!ownShareInput.getText().toString().isEmpty() ||
                            !othersShareInput.getText().toString().isEmpty())) || operationType == OperationType.NEW_MONTHLY_BALANCE);

            if (allFieldsFilled) {
                double amount = Double.parseDouble(amountInput.getText().toString());
                String description = descriptionInput.getText().toString();
                double otherAmount;

                if (sharedSwitch.isChecked()){
                    otherAmount = !othersShareInput.getText().toString().isEmpty() ?
                            Double.parseDouble(othersShareInput.getText().toString()) :
                            amount - Double.parseDouble(ownShareInput.getText().toString());
                } else {
                    otherAmount = amount / 2;
                }

                String title1 = getCategoryNameForNote(categoryType1.ordinal());

                if (operationType == OperationType.COMMON_EXPENSE) {

                    //EXPENSE
                    firestoreManager.findNoteByCategory(categoryType1.ordinal(), new FirestoreManager.FirestoreNotesCallback() {
                        @Override
                        public void onNoteFound(DocumentSnapshot note) {
                            if (note != null) {
                                String existingContent = note.getString("content");
                                String updatedContent = existingContent + "\n" + (amount - otherAmount) + " " + description;
                                firestoreManager.updateNote(note.getId(), note.getString("title"), updatedContent, categoryType1.ordinal(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                    @Override
                                    public void onSuccess() {
                                        financeManager.performTransactionByOperation(OperationType.EXPENSE, amount - otherAmount);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {

                                    }
                                });
                            } else {
                                firestoreManager.createNote(title1, (amount - otherAmount) + " " + description, categoryType1.ordinal(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                    @Override
                                    public void onSuccess() {
                                        financeManager.performTransactionByOperation(OperationType.EXPENSE, amount - otherAmount);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {

                                    }
                                });
                            }
                        }
                        @Override
                        public void onError(Exception e) {

                        }
                    });

                    //DEBT
                    firestoreManager.findNoteByCategory(categoryType2.ordinal(), new FirestoreManager.FirestoreNotesCallback() {
                        @Override
                        public void onNoteFound(DocumentSnapshot note) {
                            if (note != null) {
                                String existingContent = note.getString("content");
                                String updatedContent = existingContent + "\n" + otherAmount + " " + description;
                                firestoreManager.updateNote(note.getId(), note.getString("title"), updatedContent, categoryType2.ordinal(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                    @Override
                                    public void onSuccess() {
                                        financeManager.performTransactionByOperation(OperationType.DEBT, otherAmount);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {

                                    }
                                });
                            } else {
                                String title2 = getCategoryNameForNote(categoryType2.ordinal());
                                firestoreManager.createNote(title2, otherAmount + " " + description, categoryType2.ordinal(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                    @Override
                                    public void onSuccess() {
                                        financeManager.performTransactionByOperation(OperationType.DEBT, otherAmount);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {

                                    }
                                });
                            }
                        }
                        @Override
                        public void onError(Exception e) {

                        }
                    });
                } else if (operationType == OperationType.NEW_MONTHLY_BALANCE) {

                    // Havi megtakarítások jegyzete
                    firestoreManager.findNoteByCategory(categoryType1.ordinal(), new FirestoreManager.FirestoreNotesCallback() {
                        @Override
                        public void onNoteFound(DocumentSnapshot note) {
                            if (note != null) {
                                firestoreManager.getBalances(new FirestoreManager.OnBalancesRetrievedListener() {
                                    @Override
                                    public void onBalancesRetrieved(double totalBalance, double monthlyBalance, double debtBalance, double savesBalance) {
                                        String existingContent = note.getString("content");
                                        String newLine = monthlyBalance + " " + getPreviousMonthName(Locale.getDefault());
                                        String updatedContent = existingContent + "\n" + newLine;
                                        firestoreManager.updateNote(note.getId(), note.getString("title"), updatedContent, categoryType1.ordinal(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                            @Override
                                            public void onSuccess() {}

                                            @Override
                                            public void onFailure(Exception e) {}
                                        });
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        // Kezeld a lekérdezés során felmerülő hibát
                                        e.printStackTrace();
                                    }
                                });
                            } else {
                                firestoreManager.getBalances(new FirestoreManager.OnBalancesRetrievedListener() {
                                    @Override
                                    public void onBalancesRetrieved(double totalBalance, double monthlyBalance, double debtBalance, double savesBalance) {
                                        firestoreManager.createNote(title1, monthlyBalance + " " + getPreviousMonthName(Locale.getDefault()),
                                                categoryType1.ordinal(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                            @Override
                                            public void onSuccess() {}

                                            @Override
                                            public void onFailure(Exception e) {}
                                        });
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        // Kezeld a lekérdezés során felmerülő hibát
                                        e.printStackTrace();
                                    }
                                });
                            }
                        }
                        @Override
                        public void onError(Exception e) {

                        }
                    });

                    // Havi fizetések jegyzete
                    firestoreManager.findNoteByCategory(categoryType2.ordinal(), new FirestoreManager.FirestoreNotesCallback() {
                        @Override
                        public void onNoteFound(DocumentSnapshot note) {
                            if (note != null) {
                                firestoreManager.getBalances(new FirestoreManager.OnBalancesRetrievedListener() {
                                    @Override
                                    public void onBalancesRetrieved(double totalBalance, double monthlyBalance, double debtBalance, double savesBalance) {
                                        String existingContent = note.getString("content");
                                        String newLine = amount + " " + getCurrentMonthName(Locale.getDefault());
                                        String updatedContent = existingContent + "\n" + newLine;
                                        firestoreManager.updateNote(note.getId(), note.getString("title"), updatedContent, categoryType2.ordinal(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                            @Override
                                            public void onSuccess() {
                                                financeManager.performTransactionByOperation(operationType, amount);
                                            }

                                            @Override
                                            public void onFailure(Exception e) {
                                                // Hiba kezelése
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        // Kezeld a lekérdezés során felmerülő hibát
                                        e.printStackTrace();
                                    }
                                });
                            } else {
                                firestoreManager.getBalances(new FirestoreManager.OnBalancesRetrievedListener() {
                                    @Override
                                    public void onBalancesRetrieved(double totalBalance, double monthlyBalance, double debtBalance, double savesBalance) {
                                        String title2 = getCategoryNameForNote(categoryType2.ordinal());
                                        firestoreManager.createNote(title2, amount + " " + getCurrentMonthName(Locale.getDefault()),
                                                categoryType2.ordinal(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                                    @Override
                                                    public void onSuccess() {
                                                        financeManager.performTransactionByOperation(operationType, amount);
                                                    }

                                                    @Override
                                                    public void onFailure(Exception e) {
                                                        // Hiba kezelése
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        // Kezeld a lekérdezés során felmerülő hibát
                                        e.printStackTrace();
                                    }
                                });
                            }
                        }
                        @Override
                        public void onError(Exception e) {

                        }
                    });
                // Nagyobb összegű kiadások
                } else if (operationType == OperationType.LARGER_EXPENSE) {
                    firestoreManager.getBalances(new FirestoreManager.OnBalancesRetrievedListener() {
                        @Override
                        public void onBalancesRetrieved(double totalBalance, double monthlyBalance, double debtBalance, double savesBalance) {
                            // Ha nem tudjuk kifizetni csak a teljes egyenlegből, akkor havi kiadásnak tekintjük
                            if (totalBalance - amount < monthlyBalance) {
                                firestoreManager.findNoteByCategory(categoryType2.ordinal(), new FirestoreManager.FirestoreNotesCallback() {
                                    @Override
                                    public void onNoteFound(DocumentSnapshot note) {
                                        if (note != null) {
                                            String existingContent = note.getString("content");
                                            String updatedContent = existingContent + "\n" + amount + " " + description;
                                            firestoreManager.updateNote(note.getId(), note.getString("title"), updatedContent, categoryType2.ordinal(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                                @Override
                                                public void onSuccess() {
                                                    financeManager.performTransactionByOperation(OperationType.EXPENSE, amount);
                                                }

                                                @Override
                                                public void onFailure(Exception e) {
                                                    // Hiba kezelése
                                                }
                                            });
                                        } else {
                                            firestoreManager.createNote(title1, amount + " " + description, categoryType2.ordinal(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                                @Override
                                                public void onSuccess() {
                                                    financeManager.performTransactionByOperation(OperationType.EXPENSE, amount);
                                                }

                                                @Override
                                                public void onFailure(Exception e) {
                                                    // Hiba kezelése
                                                }
                                            });
                                        }
                                    }
                                    @Override
                                    public void onError(Exception e) {

                                    }
                                });
                            } else {
                                firestoreManager.findNoteByCategory(categoryType1.ordinal(), new FirestoreManager.FirestoreNotesCallback() {
                                    @Override
                                    public void onNoteFound(DocumentSnapshot note) {
                                        if (note != null) {
                                            String existingContent = note.getString("content");
                                            String updatedContent = existingContent + "\n" + amount + " " + description;
                                            firestoreManager.updateNote(note.getId(), note.getString("title"), updatedContent, categoryType1.ordinal(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                                @Override
                                                public void onSuccess() {
                                                    financeManager.performTransactionByOperation(operationType, amount);
                                                }

                                                @Override
                                                public void onFailure(Exception e) {
                                                    // Hiba kezelése
                                                }
                                            });
                                        } else {
                                            firestoreManager.createNote(title1, amount + " " + description, categoryType1.ordinal(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                                @Override
                                                public void onSuccess() {
                                                    financeManager.performTransactionByOperation(operationType, amount);
                                                }

                                                @Override
                                                public void onFailure(Exception e) {
                                                    // Hiba kezelése
                                                }
                                            });
                                        }
                                    }
                                    @Override
                                    public void onError(Exception e) {

                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else if (operationType == OperationType.RECURRING_MONTHLY_EXPENSE) {
//                    DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
//                        // Itt tárolhatod el az ismétlődő kiadás időpontját
//                        Calendar repeatDate = Calendar.getInstance();
//                        repeatDate.set(Calendar.YEAR, year);
//                        repeatDate.set(Calendar.MONTH, monthOfYear);
//                        repeatDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
//
//                        // Elindítjuk az ismétlődő kiadások ütemezését az első futtatásra
//                        scheduleFirstRun(repeatDate.get(Calendar.DAY_OF_MONTH), amount, description);
//                    };
//
//                    // Dátumválasztó megnyitása
//                    Calendar today = Calendar.getInstance();
//                    DatePickerDialog datePickerDialog = new DatePickerDialog(
//                            MainActivity.this, dateSetListener, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
//                    datePickerDialog.show();
                } else {
                    firestoreManager.findNoteByCategory(categoryType1.ordinal(), new FirestoreManager.FirestoreNotesCallback() {
                        @Override
                        public void onNoteFound(DocumentSnapshot note) {
                            if (note != null) {
                                String existingContent = note.getString("content");
                                String updatedContent = existingContent + "\n" + amount + " " + description;
                                firestoreManager.updateNote(note.getId(), note.getString("title"), updatedContent, categoryType1.ordinal(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                    @Override
                                    public void onSuccess() {
                                        financeManager.performTransactionByOperation(operationType, amount);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        // Hiba kezelése
                                    }
                                });
                            } else {
                                firestoreManager.createNote(title1, amount + " " + description, categoryType1.ordinal(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                    @Override
                                    public void onSuccess() {
                                        financeManager.performTransactionByOperation(operationType, amount);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        // Hiba kezelése
                                    }
                                });
                            }
                        }
                        @Override
                        public void onError(Exception e) {

                        }
                    });
                }
            } else {
                Toast.makeText(MainActivity.this, R.string.empty_fields, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void scheduleFirstRun(int dayOfMonth, double amount, String description) {
        Calendar firstRun = Calendar.getInstance();
        if (firstRun.get(Calendar.DAY_OF_MONTH) > dayOfMonth) {
            firstRun.add(Calendar.MONTH, 1);
        }
        firstRun.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        long initialDelay = firstRun.getTimeInMillis() - System.currentTimeMillis();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(RecurringExpenseWorker.class)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder()
                        .putDouble("amount", amount)
                        .putString("description", description)
                        .putInt("dayOfMonth", dayOfMonth)
                        .build())
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(request);
    }

    private String getTitleForOperationType(OperationType operationType) {
        Resources res = getResources();
        switch (operationType) {
            case OTHER_INCOME:
                return res.getString(R.string.other_income);
            case EXPENSE:
                return res.getString(R.string.expense);
            case LARGER_EXPENSE:
                return res.getString(R.string.larger_expense);
            case COMMON_EXPENSE:
                return res.getString(R.string.common_expense);
            case RECURRING_MONTHLY_EXPENSE:
                return res.getString(R.string.recurring_monthly_expense);
            case DEBT:
                return res.getString(R.string.debt);
            case NEW_MONTHLY_BALANCE:
                return res.getString(R.string.new_monthly_balance);
            default:
                return res.getString(R.string.unknown_title);
        }
    }

    private String getCategoryNameForNote(int categoryIndex) {
        String[] categoryNames = getResources().getStringArray(R.array.note_categories);

        if (categoryIndex >= 0 && categoryIndex < categoryNames.length) {
            return categoryNames[categoryIndex];
        } else {
            return "";
        }
    }

    private void configureDialogFields(OperationType operationType, Switch sharedSwitch, LinearLayout sharedAmountInput, EditText descriptionInput) {
        boolean isCommonExpense = operationType == OperationType.COMMON_EXPENSE;
        boolean isNewMonthlyExpense = operationType == OperationType.NEW_MONTHLY_BALANCE;
        sharedSwitch.setVisibility(isCommonExpense ? View.VISIBLE : View.GONE);
        sharedAmountInput.setVisibility(View.GONE);
        descriptionInput.setVisibility(isNewMonthlyExpense ? View.GONE : View.VISIBLE);

        if (isCommonExpense) {
            sharedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sharedAmountInput.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });
        }
    }

    public String getCurrentMonthName(Locale locale) {
        Calendar calendar = Calendar.getInstance();
        int monthIndex = calendar.get(Calendar.MONTH);
        return new DateFormatSymbols(locale).getMonths()[monthIndex];
    }

    public String getPreviousMonthName(Locale locale) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        int monthIndex = calendar.get(Calendar.MONTH);
        return new DateFormatSymbols(locale).getMonths()[monthIndex];
    }

    private void updateNavHeader() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        ImageView profileImageView = headerView.findViewById(R.id.imageView);
        TextView nameTextView = headerView.findViewById(R.id.nameTextView);
        TextView emailTextView = headerView.findViewById(R.id.textView);

        View.OnClickListener openSettingsListener = v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        };

        profileImageView.setOnClickListener(openSettingsListener);
        nameTextView.setOnClickListener(openSettingsListener);
        emailTextView.setOnClickListener(openSettingsListener);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Uri photoUrl = user.getPhotoUrl();
            String email = user.getEmail();

            if (photoUrl != null) {
                Glide.with(this).load(photoUrl.toString()).into(profileImageView);
            }
            emailTextView.setText(email != null ? email : getString(R.string.nav_header_subtitle));

            FirestoreManager.getInstance().getDb().collection("users").document(user.getUid())
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            nameTextView.setText(username != null ? username : getString(R.string.nav_header_title));
                        }
                    });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private int getSelectedMenuItemId() {
        // Ellenőrizd, hogy melyik menüpont van kiválasztva a Navigation Drawer-ben
        NavigationView navigationView = findViewById(R.id.nav_view);
        Menu menu = navigationView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            if (menuItem.isChecked()) {
                return menuItem.getItemId();
            }
        }
        return -1; // Ha nincs kiválasztott menüpont
    }
}
