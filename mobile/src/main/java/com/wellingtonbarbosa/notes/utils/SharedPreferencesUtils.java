package com.wellingtonbarbosa.notes.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.wellingtonbarbosa.notes.model.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SharedPreferencesUtils {

    public static void saveNote(Note note, Context context) {
        if (note != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(note.getId(), note.getTitle());
            editor.apply();
        }
    }

    public static List<Note> getAllNotes(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        List<Note> noteList = new ArrayList<>();
        Map<String, ?> key = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : key.entrySet()) {
            String savedValue = (String) entry.getValue();

            if (savedValue != null) {
                Note note = new Note(entry.getKey(), savedValue);
                noteList.add(note);
            }
        }
        return noteList;
    }

    public static void removeNote(String id, Context context) {
        if (id != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(id);
            editor.apply();
        }
    }

    public static void removeAllNotes(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
