package com.myapps.mypayments.adapters;

import static android.content.Context.MODE_PRIVATE;
import static java.security.AccessController.getContext;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.myapps.mypayments.R;
import com.myapps.mypayments.activities.NewNoteActivity;
import com.myapps.mypayments.fragments.NotesFragment;
import com.myapps.mypayments.models.Note;
import com.myapps.mypayments.utils.FinanceManager;
import com.myapps.mypayments.utils.FirestoreManager;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {

    private NotesFragment fragmentReference;
    private String updatedAtText;
    public static final int CATEGORY_MONTHLY_PAYMENTS = 7;
    public static final int CATEGORY_MONTHLY_SAVES = 2;
    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView titleTextView;
        public TextView updatedAtTextView;
        public ImageView deleteIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            updatedAtTextView = itemView.findViewById(R.id.updatedAtTextView);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
        }
    }

    private List<Note> noteList;
    private RecyclerView recyclerView;
    private Context context;
    private SimpleDateFormat dateFormat;

    public NotesAdapter(List<Note> noteList) {
        this.noteList = noteList;
    }

    public NotesAdapter(List<Note> noteList, NotesFragment fragment, Context context) {
        this.noteList = noteList;
        this.fragmentReference = fragment;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.note_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Note note = noteList.get(position);
        holder.titleTextView.setText(note.getTitle());

        updateDateFormat();
        updatedAtText = dateFormat.format(note.getUpdatedAt());
        holder.updatedAtTextView.setText(updatedAtText);

        // Itt adjuk hozzá a click listener-t
        holder.itemView.setOnClickListener(v -> {
            if (holder.deleteIcon.getVisibility() == View.VISIBLE) {
                holder.deleteIcon.setVisibility(View.INVISIBLE);
            } else {
                // Indításra kerül a NewNoteActivity a kiválasztott jegyzet adatokkal
                Intent intent = new Intent(v.getContext(), NewNoteActivity.class);
                intent.putExtra("noteId", noteList.get(position).getNoteId());
                intent.putExtra("title", note.getTitle());
                intent.putExtra("content", note.getContent());
                intent.putExtra("categoryIndex", note.getCategoryIndex());
                v.getContext().startActivity(intent);

                holder.deleteIcon.setVisibility(View.INVISIBLE);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            hideAllDeleteIconsExcept(holder);
            holder.deleteIcon.setVisibility(View.VISIBLE);
            return true;
        });

        holder.deleteIcon.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());

            if (note.getCategoryIndex() == CATEGORY_MONTHLY_PAYMENTS) {
                builder.setTitle(R.string.confirm_delete);
                builder.setMessage(R.string.confirm_delete_multiple_notes_one);
            } else if (note.getCategoryIndex() == CATEGORY_MONTHLY_SAVES) {
                builder.setTitle(R.string.confirm_delete);
                builder.setMessage(R.string.confirm_delete_multiple_notes_two);
            } else {
                builder.setTitle(R.string.confirm_delete);
                builder.setMessage(R.string.confirm_delete_content);
            }

            builder.setPositiveButton(R.string.delete, (dialog, which) -> {
                deleteNoteFromFirestore(note, position);
                holder.deleteIcon.setVisibility(View.INVISIBLE);
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> holder.deleteIcon.setVisibility(View.INVISIBLE));

            builder.show();
        });
    }

    private void updateDateFormat() {
        SharedPreferences prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String lang = prefs.getString("My_Lang", "en");
        Locale currentLocale = new Locale(lang);

        switch (lang) {
            case "en":
                dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);
                Log.d("NotesAdapter", "Date format set to English: MMMM dd, yyyy");
                break;
            case "hu":
                dateFormat = new SimpleDateFormat("yyyy. MMMM dd.", new Locale("hu", "HU"));
                Log.d("NotesAdapter", "Date format set to Hungarian: yyyy. MMMM dd.");
                break;
            case "ja":
                dateFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.JAPAN);
                Log.d("NotesAdapter", "Date format set to Japanese: yyyy年MM月dd日");
                break;
            default:
                dateFormat = new SimpleDateFormat("yyyy. MMMM dd.", currentLocale);
                Log.d("NotesAdapter", "Date format set to default: yyyy. MMMM dd. for locale " + currentLocale.toString());
                break;
        }
    }

    public void hideAllDeleteIconsExcept(ViewHolder exceptionHolder) {
        for (int i = 0; i < getItemCount(); i++) {
            ViewHolder holder = (ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            if (holder != null && holder != exceptionHolder && holder.deleteIcon.getVisibility() == View.VISIBLE) {
                holder.deleteIcon.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void deleteNoteFromFirestore(Note note, int position) {
        FirestoreManager firestoreManager = FirestoreManager.getInstance();

        Runnable deletionCompleteRunnable = () -> {
            noteList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, noteList.size());
        };

        switch (note.getCategoryIndex()) {
            case CATEGORY_MONTHLY_PAYMENTS:
                firestoreManager.deleteNoteByCategory(CATEGORY_MONTHLY_SAVES, new FirestoreManager.OnFirestoreOperationCompleteListener() {
                    @Override
                    public void onSuccess() {
                        note.processNewLines(new FinanceManager(), true);
                        firestoreManager.deleteNote(note.getNoteId(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                            @Override
                            public void onSuccess() {
                                recyclerView.post(() -> {
                                    deletionCompleteRunnable.run();
                                    recyclerView.getAdapter().notifyDataSetChanged();
                                    Toast.makeText(recyclerView.getContext(), R.string.notes_deleted, Toast.LENGTH_SHORT).show();
                                    fragmentReference.loadNotesFromFirestore();
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(recyclerView.getContext(), R.string.note_delete_error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(recyclerView.getContext(), R.string.note_delete_error, Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case CATEGORY_MONTHLY_SAVES:
                firestoreManager.findNoteByCategory(CATEGORY_MONTHLY_PAYMENTS, new FirestoreManager.FirestoreNotesCallback() {
                    @Override
                    public void onNoteFound(DocumentSnapshot noteDocument) {
                        firestoreManager.deleteNote(note.getNoteId(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                            @Override
                            public void onSuccess() {
                                if (noteDocument != null) {
                                    Note paymentNote = noteDocument.toObject(Note.class);
                                    if (paymentNote != null) {
                                        paymentNote.processNewLines(new FinanceManager(), true);
                                    }
                                }
                                firestoreManager.deleteNoteByCategory(CATEGORY_MONTHLY_PAYMENTS, new FirestoreManager.OnFirestoreOperationCompleteListener() {
                                    @Override
                                    public void onSuccess() {
                                        recyclerView.post(() -> {
                                            deletionCompleteRunnable.run();
                                            recyclerView.getAdapter().notifyDataSetChanged();
                                            Toast.makeText(recyclerView.getContext(), R.string.notes_deleted, Toast.LENGTH_SHORT).show();
                                            fragmentReference.loadNotesFromFirestore();
                                        });
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        Toast.makeText(recyclerView.getContext(), R.string.note_delete_error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(recyclerView.getContext(), R.string.note_delete_error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(recyclerView.getContext(), R.string.note_delete_error, Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            default:
                note.processNewLines(new FinanceManager(), true);
                firestoreManager.deleteNote(note.getNoteId(), new FirestoreManager.OnFirestoreOperationCompleteListener() {
                    @Override
                    public void onSuccess() {
                        deletionCompleteRunnable.run();
                        Toast.makeText(recyclerView.getContext(), R.string.note_deleted, Toast.LENGTH_SHORT).show();
                        fragmentReference.loadNotesFromFirestore();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(recyclerView.getContext(), R.string.note_delete_error, Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }
    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }
}
