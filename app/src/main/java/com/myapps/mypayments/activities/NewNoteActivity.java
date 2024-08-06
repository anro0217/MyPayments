package com.myapps.mypayments.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.myapps.mypayments.R;
import com.myapps.mypayments.models.Note;
import com.myapps.mypayments.utils.FinanceManager;
import com.myapps.mypayments.utils.FirestoreManager;

import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class NewNoteActivity extends AppCompatActivity {

    private ImageView backButton, undoButton, redoButton, saveButton;
    private EditText titleEditText, contentEditText;
    private Spinner categorySpinner;
    private String noteId;
    private Intent intent;
    private boolean isContentChanged = false, isTitleChanged = false, isUndoPerformed = false;
    private boolean isNewNote;
    private String originalContent = null, modifiedContent = null;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_note);

        firestoreManager = FirestoreManager.getInstance();

        // View-k inicializálása
        titleEditText = findViewById(R.id.titleEditText);
        contentEditText = findViewById(R.id.contentEditText);
        categorySpinner = findViewById(R.id.categorySpinner);
        backButton = findViewById(R.id.backButton);
        saveButton = findViewById(R.id.saveButton);
        undoButton = findViewById(R.id.undoButton);
        redoButton = findViewById(R.id.redoButton);

        intent = getIntent();
        if (intent != null && intent.hasExtra("noteId")) {
            noteId = intent.getStringExtra("noteId");
            titleEditText.setText(intent.getStringExtra("title"));
            originalContent = intent.getStringExtra("content");
            contentEditText.setText(originalContent);
            isNewNote = false;
        } else {
            isNewNote = true;
            originalContent = "";
        }

        setupUI();
    }

    private void setupUI() {
        backButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveNote());
        setupSpinner();
        setupTextWatchers();
        updateButtonStates();
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.note_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        // A kategória beállítása itt történik
        if (!isNewNote && intent != null) {
            int categoryIndex = intent.getIntExtra("categoryIndex", -1);
            if (categoryIndex != -1) {
                categorySpinner.setSelection(categoryIndex, false);
            }
            categorySpinner.setEnabled(false);
        } else {
            categorySpinner.setEnabled(true);
        }
    }

    private void setupTextWatchers() {
        TextWatcher titleTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                isTitleChanged = true;
                updateButtonStates();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };

        contentEditText.addTextChangedListener(new TextWatcher() {
            private String previousText = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                previousText = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!previousText.trim().equals(s.toString().trim()) && !s.toString().trim().isEmpty()) {
                    updateButtonStates();
                }
            }
        });

        titleEditText.addTextChangedListener(titleTextWatcher);
    }

    private void updateButtonStates() {
        String title = titleEditText.getText().toString();
        String content = contentEditText.getText().toString();

        boolean isTitleNotEmpty = !title.trim().isEmpty();
        boolean isContentNotEmpty = !content.trim().isEmpty();

        // A változások detektálása

        String intentTitle = intent.getStringExtra("title");
        if (intentTitle != null) {
            isTitleChanged = !title.trim().equals(intentTitle.trim());
        } else {
            isTitleChanged = false;
        }
        isContentChanged = !content.trim().equals(originalContent.trim());


        // Az új jegyzet és a módosított jegyzet állapotának kezelése
        boolean shouldEnableSaveButton = isTitleNotEmpty && isContentNotEmpty && (isNewNote || isTitleChanged || isContentChanged);
        saveButton.setEnabled(shouldEnableSaveButton);
        saveButton.setAlpha(shouldEnableSaveButton ? 1.0f : 0.5f);

        // Undo és redo logika frissítése
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);
        undoButton.setAlpha(0.5f);
        redoButton.setAlpha(0.5f);
    }

    private void saveNote() {
        String title = titleEditText.getText().toString();
        String content = contentEditText.getText().toString();
        int categoryIndex = categorySpinner.getSelectedItemPosition();

        int largerCategoryIndex = MainActivity.CategoryType.LARGER_EXPENSES.ordinal();

        if (categoryIndex == largerCategoryIndex) {
            isBalanceEnough(content, largerCategoryIndex, new BalanceCheckCallback() {
                @Override
                public void onResult(boolean isEnough) {
                    if (isEnough) {
                        checkTitleAndCategoryUniqueness(title, categoryIndex, noteId, content);
                    } else {
                        runOnUiThread(() -> showBalanceWarningDialog());
                    }
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {});
                }
            });
        } else {
            checkTitleAndCategoryUniqueness(title, categoryIndex, noteId, content);
        }
    }

    interface BalanceCheckCallback {
        void onResult(boolean isEnough);
        void onError(Exception e);
    }

    private void isBalanceEnough(final String content, int largerCategoryIndex, BalanceCheckCallback callback) {
        firestoreManager.getBalances(new FirestoreManager.OnBalancesRetrievedListener() {
            @Override
            public void onBalancesRetrieved(double totalBalance, double monthlyBalance, double debtBalance, double savesBalance) {
                firestoreManager.checkCategoryUnique(largerCategoryIndex, noteId, new FirestoreManager.FirestoreCheckCallback() {
                    @Override
                    public void onCheckCompleted(boolean isCategoryUnique) {
                        if (noteId == null || noteId.isEmpty()) {
                            Note tempNote = new Note("", content, -1, new Date(), new Date());
                            if (totalBalance - tempNote.getLinesTotalValue() < monthlyBalance) {
                                callback.onResult(false);
                            } else {
                                callback.onResult(true);
                            }
                        } else {
                            firestoreManager.getNoteReference(noteId).get().addOnSuccessListener(documentSnapshot -> {
                                String originalContent = documentSnapshot.getString("content");
                                Note tempNote = new Note("", content, -1, null, new Date());
                                if (totalBalance - tempNote.getUpdatesValue(originalContent) < monthlyBalance) {
                                    callback.onResult(false);
                                } else {
                                    callback.onResult(true);
                                }
                            }).addOnFailureListener(e -> {
                                callback.onError(e);
                            });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private void showBalanceWarningDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(NewNoteActivity.this);
        builder.setTitle(R.string.balance_warning_title); // Címsor beállítása
        builder.setMessage(R.string.not_enough_balance); // Üzenet beállítása

        // OK gomb hozzáadása, amely bezárja a dialogot
        builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
            dialog.dismiss(); // Dialog bezárása
        });

        // A dialog megjelenítése
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void checkTitleAndCategoryUniqueness(String title, int categoryIndex, final String noteId, final String content) {
        firestoreManager.checkNoteTitleUnique(title, noteId, new FirestoreManager.FirestoreCheckCallback() {
            @Override
            public void onCheckCompleted(boolean isTitleUnique) {
                if (isTitleUnique) {
                    firestoreManager.checkCategoryUnique(categoryIndex, noteId, new FirestoreManager.FirestoreCheckCallback() {
                        @Override
                        public void onCheckCompleted(boolean isCategoryUnique) {
                            if (categoryIndex == 0 || isCategoryUnique) {
                                if (noteId == null || noteId.isEmpty()) {
                                    firestoreManager.createNote(title, content, categoryIndex, new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                        @Override
                                        public void onSuccess() {
                                            Note newNote = new Note(title, content, categoryIndex, new Date(), new Date());
                                            newNote.processNewLines(new FinanceManager(), false);
                                            Toast.makeText(NewNoteActivity.this, R.string.new_note, Toast.LENGTH_SHORT).show();
                                            setResult(RESULT_OK);
                                            finish();
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            Toast.makeText(NewNoteActivity.this, R.string.fail_during_note, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    fetchOriginalContentAndProcessUpdates(noteId, title, content, categoryIndex);
                                }
                            } else {
                                Toast.makeText(NewNoteActivity.this, R.string.reserved_category, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(NewNoteActivity.this, R.string.category_check_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(NewNoteActivity.this, R.string.reserved_title, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(NewNoteActivity.this, R.string.title_check_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchOriginalContentAndProcessUpdates(final String noteId, final String title, final String content, final int categoryIndex) {
        firestoreManager.getNoteReference(noteId).get().addOnSuccessListener(documentSnapshot -> {
            String originalContent = documentSnapshot.getString("content");
            firestoreManager.updateNote(noteId, title, content, categoryIndex, new FirestoreManager.OnFirestoreOperationCompleteListener() {
                @Override
                public void onSuccess() {
                    Note note = new Note(title, content, categoryIndex, null, new Date());
                    note.processUpdates(new FinanceManager(), originalContent);
                    Toast.makeText(NewNoteActivity.this, R.string.note_updated, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(NewNoteActivity.this, R.string.note_update_error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}

