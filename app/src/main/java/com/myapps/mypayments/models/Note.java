package com.myapps.mypayments.models;

import android.util.Log;

import com.myapps.mypayments.utils.FinanceManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Note {
    private String noteId;
    private String title;
    private String content;
    private int categoryIndex;
    private Date createdAt;
    private Date updatedAt;

    public Note() {}
    public Note(String title, String content, int categoryIndex, Date createdAt, Date updatedAt) {
        this.title = title;
        this.content = content;
        this.categoryIndex = categoryIndex;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    public String getNoteId() {
        return noteId;
    }
    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public int getCategoryIndex() {
        return categoryIndex;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void processNewLines(FinanceManager financeManager, boolean isReversed) {
        String[] lines = this.content.split("\n");

        double totalChange = 0.0;
        boolean isDelete = false;
        for (String line : lines) {
            if (!line.contains(":") && line.matches(".*\\d+.*")) {
                try {
                    totalChange += Double.parseDouble(line.split("[a-zA-ZáÁéÉíÍóÓöÖőŐúÚüÜűŰ]")[0]
                            .replace(" ", ""));
                } catch (NumberFormatException e) {
                    Log.e("NoteUpdate", "Error parsing amount in new line: " + line);
                }
            }
        }
        if (isReversed){
            totalChange = - totalChange;
            isDelete = true;
        }
        financeManager.performTransactionByNoteEdit(categoryIndex, totalChange, isDelete);
    }

    public void processUpdates(FinanceManager financeManager, String originalContent) {
        List<Double> originalAmounts = extractAmounts(originalContent);
        List<Double> updatedAmounts = extractAmounts(this.content);

        // Az összegek változásainak összegzéséhez először azonosítjuk a törölt és hozzáadott összegeket
        double totalChange = 0.0;
        int minLength = Math.min(originalAmounts.size(), updatedAmounts.size());

        // Összegek összehasonlítása a változások azonosításához
        for (int i = 0; i < minLength; i++) {
            double change = updatedAmounts.get(i) - originalAmounts.get(i);
            totalChange += change;
        }

        // Hozzáadjuk az újonnan hozzáadott sorok összegeit, ha az új jegyzet hosszabb
        for (int i = minLength; i < updatedAmounts.size(); i++) {
            totalChange += updatedAmounts.get(i);
        }

        // Levonjuk az eltávolított sorok összegeit, ha az eredeti jegyzet volt hosszabb
        for (int i = minLength; i < originalAmounts.size(); i++) {
            totalChange -= originalAmounts.get(i);
        }

        if (totalChange != 0) {
            financeManager.performTransactionByNoteEdit(categoryIndex, totalChange);
        }
    }

    public double getLinesTotalValue(){
        String[] lines = this.content.split("\n");

        double totalChange = 0.0;
        for (String line : lines) {
            if (!line.contains(":") && line.matches(".*\\d+.*")) {
                try {
                    totalChange += Double.parseDouble(line.split("[a-zA-ZáÁéÉíÍóÓöÖőŐúÚüÜűŰ]")[0]
                            .replace(" ", ""));
                } catch (NumberFormatException e) {
                    Log.e("NoteUpdate", "Error parsing amount in new line: " + line);
                }
            }
        }
        return totalChange;
    }
    public double getUpdatesValue(String originalContent){
        List<Double> originalAmounts = extractAmounts(originalContent);
        List<Double> updatedAmounts = extractAmounts(this.content);

        // Az összegek változásainak összegzéséhez először azonosítjuk a törölt és hozzáadott összegeket
        double totalChange = 0.0;
        int minLength = Math.min(originalAmounts.size(), updatedAmounts.size());

        // Összegek összehasonlítása a változások azonosításához
        for (int i = 0; i < minLength; i++) {
            double change = updatedAmounts.get(i) - originalAmounts.get(i);
            totalChange += change;
        }

        // Hozzáadjuk az újonnan hozzáadott sorok összegeit, ha az új jegyzet hosszabb
        for (int i = minLength; i < updatedAmounts.size(); i++) {
            totalChange += updatedAmounts.get(i);
        }

        // Levonjuk az eltávolított sorok összegeit, ha az eredeti jegyzet volt hosszabb
        for (int i = minLength; i < originalAmounts.size(); i++) {
            totalChange -= originalAmounts.get(i);
        }

        return totalChange;
    }

    private List<Double> extractAmounts(String content) {
        List<Double> amounts = new ArrayList<>();
        String[] lines = content.split("\n");

        for (String line : lines) {
            if (!line.contains(":") && line.matches(".*\\d+.*")) {
                try {
                    double amount = Double.parseDouble(line.split("[a-zA-ZáÁéÉíÍóÓöÖőŐúÚüÜűŰ]")[0]
                            .replace(" ", ""));
                    amounts.add(amount);
                } catch (NumberFormatException e) {
                    Log.e("NoteUpdate", "Error parsing amount in new line: " + line);
                }
            }
        }
        return amounts;
    }
}
