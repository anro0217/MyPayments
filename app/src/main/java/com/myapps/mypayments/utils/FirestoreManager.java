package com.myapps.mypayments.utils;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.myapps.mypayments.R;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FirestoreManager {

    private static FirestoreManager instance;
    private FirebaseFirestore db;
    private String userId;

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public static synchronized FirestoreManager getInstance() {
        if (instance == null) {
            instance = new FirestoreManager();
        }
        return instance;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public DocumentReference getNoteReference(String noteId) {
        return getDb().collection("users").document(userId).collection("notes").document(noteId);
    }

    public Task<DocumentSnapshot> getUserDocument() {
        DocumentReference userDocRef = getDb().collection("users").document(getUserId());
        return userDocRef.get();
    }

    public void getBalances(OnBalancesRetrievedListener listener) {
        DocumentReference financeRef = db.collection("users").document(userId);
        financeRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                double totalBalance = documentSnapshot.getDouble("totalBalance") != null ? documentSnapshot.getDouble("totalBalance") : 0.0;
                double monthlyBalance = documentSnapshot.getDouble("monthlyBalance") != null ? documentSnapshot.getDouble("monthlyBalance") : 0.0;
                double debtBalance = documentSnapshot.getDouble("debtBalance") != null ? documentSnapshot.getDouble("debtBalance") : 0.0;
                double savesBalance = documentSnapshot.getDouble("savesBalance") != null ? documentSnapshot.getDouble("savesBalance") : 0.0;
                listener.onBalancesRetrieved(totalBalance, monthlyBalance, debtBalance, savesBalance);
            } else {
                listener.onError(new Exception("Document does not exist"));
            }
        }).addOnFailureListener(e -> {
            listener.onError(e);
        });
    }

    public interface OnBalancesRetrievedListener {
        void onBalancesRetrieved(double totalBalance, double monthlyBalance, double debtBalance, double savesBalance);
        void onError(Exception e);
    }

    public void updateBalances(double totalBalance, double monthlyBalance, double debtBalance, double savesBalance, OnFirestoreOperationCompleteListener listener) {
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("totalBalance", totalBalance);
        updateMap.put("monthlyBalance", monthlyBalance);
        updateMap.put("debtBalance", debtBalance);
        updateMap.put("savesBalance", savesBalance);

        getDb().collection("users").document(getUserId()).update(updateMap)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e));
    }

    public void listenToBalanceUpdates(OnBalancesRetrievedListener listener) {
        DocumentReference financeRef = db.collection("users").document(userId);
        financeRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    listener.onError(e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    double totalBalance = snapshot.getDouble("totalBalance") != null ? snapshot.getDouble("totalBalance") : 0.0;
                    double monthlyBalance = snapshot.getDouble("monthlyBalance") != null ? snapshot.getDouble("monthlyBalance") : 0.0;
                    double debtBalance = snapshot.getDouble("debtBalance") != null ? snapshot.getDouble("debtBalance") : 0.0;
                    double savesBalance = snapshot.getDouble("savesBalance") != null ? snapshot.getDouble("savesBalance") : 0.0;

                    listener.onBalancesRetrieved(totalBalance, monthlyBalance, debtBalance, savesBalance);
                } else {
                    listener.onError(new Exception("Document does not exist"));
                }
            }
        });
    }


    public void createUserInFirestore(String displayName, String email) {
        DocumentReference userDocRef = getDb().collection("users").document(getUserId());

        Map<String, Object> newUser = new HashMap<>();
        newUser.put("username", displayName);
        newUser.put("email", email);
        newUser.put("debtBalance", 0);
        newUser.put("monthlyBalance", 0);
        newUser.put("totalBalance", 0);
        newUser.put("savesBalance", 0);
        newUser.put("createdAt", new Date());
        newUser.put("updatedAt", new Date());

        userDocRef.set(newUser); // A dokumentum létrehozása a gyűjteményben
    }

    public void createNote(String title, String content, int categoryIndex, OnFirestoreOperationCompleteListener listener) {
        Map<String, Object> newNoteMap = new HashMap<>();
        newNoteMap.put("title", title);
        newNoteMap.put("content", content);
        newNoteMap.put("categoryIndex", categoryIndex);
        newNoteMap.put("createdAt", new Date());
        newNoteMap.put("updatedAt", new Date());

        getDb().collection("users").document(userId)
                .collection("notes")
                .add(newNoteMap)
                .addOnSuccessListener(documentReference -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e));
    }

    public void updateNote(String noteId, String title, String content, int categoryIndex, OnFirestoreOperationCompleteListener listener) {
        Map<String, Object> noteUpdate = new HashMap<>();
        noteUpdate.put("title", title);
        noteUpdate.put("content", content);
        noteUpdate.put("categoryIndex", categoryIndex);
        noteUpdate.put("updatedAt", new Date());

        getNoteReference(noteId).update(noteUpdate)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e));
    }

    public void findNoteByCategory(int categoryIndex, FirestoreNotesCallback callback) {
        db.collection("users").document(userId).collection("notes")
                .whereEqualTo("categoryIndex", categoryIndex)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Ha találtunk jegyzetet, akkor visszatérünk az elsővel
                        callback.onNoteFound(queryDocumentSnapshots.getDocuments().get(0));
                    } else {
                        // Ha nincs ilyen kategóriájú jegyzet, akkor null-t adunk vissza
                        callback.onNoteFound(null);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e));
    }

    public interface FirestoreNotesCallback {
        void onNoteFound(DocumentSnapshot note);
        void onError(Exception e);
    }

    public void deleteNote(String noteId, OnFirestoreOperationCompleteListener listener) {
        getDb().collection("users").document(userId).collection("notes").document(noteId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e));
    }

    public void deleteNoteByCategory(int categoryIndex, OnFirestoreOperationCompleteListener listener) {
        Query query = db.collection("users").document(userId).collection("notes")
                .whereEqualTo("categoryIndex", categoryIndex);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DocumentSnapshot document : task.getResult().getDocuments()) {
                    document.getReference().delete().addOnSuccessListener(aVoid -> {
                        // Handle success for each deletion here if needed
                    }).addOnFailureListener(e -> {
                        if (listener != null) {
                            listener.onFailure(e);
                        }
                    });
                }
                if (listener != null) {
                    listener.onSuccess();  // Call success after initiating all deletions
                }
            } else {
                if (listener != null) {
                    listener.onFailure(task.getException());
                }
            }
        });
    }

    public interface OnFirestoreOperationCompleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void updateUsername(String newUsername, OnFirestoreOperationCompleteListener listener) {
        DocumentReference userDocRef = getDb().collection("users").document(getUserId());

        // Készítünk egy frissítési map-et, amely csak a username-t tartalmazza.
        Map<String, Object> update = new HashMap<>();
        update.put("username", newUsername);

        // Frissítjük a dokumentumot az új felhasználónévvel.
        userDocRef.update(update)
                .addOnSuccessListener(aVoid -> {
                    // Sikeres frissítés esetén
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    // Hiba esetén
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    public Task<String> getUsername() {
        DocumentReference userDocRef = getDb().collection("users").document(getUserId());
        return userDocRef.get().continueWith(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                return task.getResult().getString("username");
            } else {
                throw task.getException() != null ? task.getException() : new Exception("Document does not exist");
            }
        });
    }

    public void checkNoteTitleUnique(String title, String excludeNoteId, FirestoreCheckCallback callback) {
        Query query = getDb().collection("users").document(userId).collection("notes").whereEqualTo("title", title);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean isUnique = true;
                for (DocumentSnapshot document : task.getResult().getDocuments()) {
                    if (excludeNoteId == null || !document.getId().equals(excludeNoteId)) {
                        isUnique = false;
                        break;
                    }
                }
                callback.onCheckCompleted(isUnique);
            } else {
                callback.onError(task.getException());
            }
        });
    }

    public void checkCategoryUnique(int categoryIndex, String excludeNoteId, FirestoreCheckCallback callback) {
        Query query = getDb().collection("users").document(userId).collection("notes").whereEqualTo("categoryIndex", categoryIndex);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean isUnique = true;
                for (DocumentSnapshot document : task.getResult().getDocuments()) {
                    if (excludeNoteId == null || !document.getId().equals(excludeNoteId)) {
                        isUnique = false;
                        break;
                    }
                }
                callback.onCheckCompleted(isUnique);
            } else {
                callback.onError(task.getException());
            }
        });
    }

    public interface FirestoreCheckCallback {
        void onCheckCompleted(boolean isUnique);
        void onError(Exception e);
    }
}