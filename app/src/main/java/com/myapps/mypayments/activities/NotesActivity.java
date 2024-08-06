package com.myapps.mypayments.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.myapps.mypayments.R;
import com.myapps.mypayments.adapters.NotesAdapter;
import com.myapps.mypayments.models.Note;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotesActivity extends AppCompatActivity {

    private List<Note> notesList = new ArrayList<>();
    private RecyclerView recyclerView;
    private NotesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_notes);

        recyclerView = findViewById(R.id.notesRecyclerView);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new NotesAdapter(notesList);
        adapter.setRecyclerView(recyclerView);
        recyclerView.setAdapter(adapter);
        }
}
