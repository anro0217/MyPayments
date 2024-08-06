package com.myapps.mypayments.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.myapps.mypayments.R;
import com.myapps.mypayments.adapters.NotesAdapter;
import com.myapps.mypayments.models.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotesFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotesAdapter notesAdapter;
    private List<Note> notesList = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notes, container, false);

        recyclerView = root.findViewById(R.id.notesRecyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        notesAdapter = new NotesAdapter(notesList, this, getContext());
        notesAdapter.setRecyclerView(recyclerView);
        recyclerView.setAdapter(notesAdapter);

        loadNotesFromFirestore();

        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                // Az érintés kezdetén ellenőrizzük.
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    View childView = rv.findChildViewUnder(e.getX(), e.getY());

                    boolean shouldHideDeleteIcons = true;

                    if (childView != null && rv.getChildViewHolder(childView) instanceof NotesAdapter.ViewHolder) {
                        NotesAdapter.ViewHolder touchedViewHolder = (NotesAdapter.ViewHolder) rv.getChildViewHolder(childView);

                        // Ellenőrizzük, hogy az érintés a delete ikon területén belül történt-e.
                        if (touchedViewHolder.deleteIcon.getVisibility() == View.VISIBLE) {
                            int[] iconLocation = new int[2];
                            touchedViewHolder.deleteIcon.getLocationOnScreen(iconLocation);
                            int iconX = iconLocation[0];
                            int iconY = iconLocation[1];
                            int iconWidth = touchedViewHolder.deleteIcon.getWidth();
                            int iconHeight = touchedViewHolder.deleteIcon.getHeight();

                            float touchX = e.getRawX();
                            float touchY = e.getRawY();

                            // Ha az érintés az ikonon belül történt, ne rejtsük el az ikonokat.
                            if (touchX >= iconX && touchX <= iconX + iconWidth &&
                                    touchY >= iconY && touchY <= iconY + iconHeight) {
                                shouldHideDeleteIcons = false;
                            }
                        }
                    }

                    // Ha nem az ikonra történt az érintés, elrejtjük az összes törlés ikont.
                    if (shouldHideDeleteIcons) {
                        for (int i = 0; i < rv.getChildCount(); i++) {
                            NotesAdapter.ViewHolder viewHolder = (NotesAdapter.ViewHolder) rv.findViewHolderForAdapterPosition(i);
                            if (viewHolder != null && viewHolder.deleteIcon.getVisibility() == View.VISIBLE) {
                                viewHolder.deleteIcon.setVisibility(View.INVISIBLE);
                            }
                        }
                    }
                }
                return false;
            }
        });
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isVisible()) {
            loadNotesFromFirestore();
        }
    }

    public void loadNotesFromFirestore() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users")
                    .document(currentUser.getUid())
                    .collection("notes")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            notesList.clear(); // Töröljük a korábbi jegyzeteket
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Note note = document.toObject(Note.class);
                                note.setNoteId(document.getId()); // Beállítjuk a dokumentum azonosítót
                                notesList.add(note);
                            }
                            notesAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(getContext(), R.string.notes_loading_error, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
