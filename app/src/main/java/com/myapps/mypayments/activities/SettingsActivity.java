package com.myapps.mypayments.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.myapps.mypayments.R;
import com.myapps.mypayments.utils.FirestoreManager;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private EditText nicknameEditText;
    private ImageView backButton;
    private Spinner languageSpinner;
    private boolean userIsInteracting = false;
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        nicknameEditText = findViewById(R.id.nicknameEditText);
        backButton = findViewById(R.id.backButton);
        languageSpinner = findViewById(R.id.languageSpinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.language_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        setSpinnerToCurrentLocale();

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (userIsInteracting) {
                    String selectedLanguage = parent.getItemAtPosition(position).toString();
                    String currentLocale = getCurrentLocale();

                    if ((selectedLanguage.equals("English") || selectedLanguage.equals("Angol") || selectedLanguage.equals("英語")) && !currentLocale.equals("en")) {
                        saveLocale("en");
                    } else if ((selectedLanguage.equals("Magyar") || selectedLanguage.equals("Hungarian") || selectedLanguage.equals("ハンガリー語")) && !currentLocale.equals("hu")) {
                        saveLocale("hu");
                    } else if ((selectedLanguage.equals("Japán") || selectedLanguage.equals("Japanese") || selectedLanguage.equals("日本語")) && !currentLocale.equals("ja")) {
                        saveLocale("ja");
                    }
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        backButton.setOnClickListener(v -> finish());

        nicknameEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveUserInformation();
                return true;
            }
            return false;
        });

        loadUserInformation();

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> signOut());
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        userIsInteracting = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setSpinnerToCurrentLocale();
        userIsInteracting = false;
    }

    private void setSpinnerToCurrentLocale() {
        String currentLocale = getSharedPreferences("Settings", MODE_PRIVATE).getString("My_Lang", "en");
        int spinnerPosition;
        switch (currentLocale) {
            case "hu":
                spinnerPosition = 1;
                break;
            case "ja":
                spinnerPosition = 2;
                break;
            default:
                spinnerPosition = 0; //Angol
                break;
        }
        languageSpinner.setSelection(spinnerPosition, false);
        userIsInteracting = false;
    }


    private void restartApp() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    private String getCurrentLocale() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        return prefs.getString("My_Lang", "en");
    }

    private void saveLocale(String lang) {
        SharedPreferences prefs = getSharedPreferences("Settings", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("My_Lang", lang);
        editor.apply();
        new Handler().postDelayed(this::restartApp, 500);
    }

    private void loadUserInformation() {
        FirestoreManager.getInstance().getUserDocument()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        nicknameEditText.setText(username);
                    }
                })
                .addOnFailureListener(e -> {});
    }

    private void saveUserInformation() {
        String newUsername = nicknameEditText.getText().toString().trim();
        if (!newUsername.isEmpty()) {
            FirestoreManager.getInstance().updateUsername(newUsername, new FirestoreManager.OnFirestoreOperationCompleteListener() {
                @Override
                public void onSuccess() {
                    // Sikeres frissítés esetén, értesítjük a felhasználót
                    Toast.makeText(SettingsActivity.this, R.string.username_updated, Toast.LENGTH_SHORT).show();
                    nicknameEditText.clearFocus();
                    // Bezárjuk a billentyűzetet
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(nicknameEditText.getWindowToken(), 0);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(SettingsActivity.this, R.string.username_update_error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, R.string.username_empty, Toast.LENGTH_SHORT).show();
        }
    }

    private void signOut() {
        // Firebase és Google kijelentkezés
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Töröljük a userId-t a FirestoreManager-ben
                        FirestoreManager.getInstance().setUserId(null);

                        // Sikeres kijelentkezés esetén átnavigálunk a bejelentkezési képernyőre
                        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // Kijelentkezés nem sikerült, kezeljük a hibát
                        Toast.makeText(SettingsActivity.this, R.string.sing_out_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
