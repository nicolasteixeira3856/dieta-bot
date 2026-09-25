package com.nutri.android.data;

import androidx.room.TypeConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

public class NutriConverters {
    @TypeConverter
    public String intListToText(List<Integer> value) {
        JSONArray arr = new JSONArray();
        List<Integer> src = value != null ? value : Arrays.asList(2000, 2000, 2000, 2000, 2000, 2000, 2000);
        for (Integer n : src) {
            arr.put(n);
        }
        return arr.toString();
    }

    @TypeConverter
    public List<Integer> textToIntList(String value) {
        List<Integer> fallback = Arrays.asList(2000, 2000, 2000, 2000, 2000, 2000, 2000);
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        try {
            JSONArray arr = new JSONArray(value);
            List<Integer> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                out.add(arr.getInt(i));
            }
            return out.size() == 7 ? out : fallback;
        } catch (JSONException e) {
            return fallback;
        }
    }

    @TypeConverter
    public String stringListToText(List<String> value) {
        JSONArray arr = new JSONArray();
        if (value != null) {
            for (String s : value) {
                arr.put(s);
            }
        }
        return arr.toString();
    }

    @TypeConverter
    public List<String> textToStringList(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            JSONArray arr = new JSONArray(value);
            List<String> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                out.add(arr.getString(i));
            }
            return out;
        } catch (JSONException e) {
            return Collections.emptyList();
        }
    }
}
