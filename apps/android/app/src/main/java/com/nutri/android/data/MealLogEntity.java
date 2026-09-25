package com.nutri.android.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "meal_log")
public class MealLogEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String date = "";
    @NonNull
    public String window = "";
    @NonNull
    public String text = "";
    public int kcal;
    public int p;
    public int stable;

    public MealLogEntity() {}

    @Ignore
    public MealLogEntity(
            @NonNull String date,
            @NonNull String window,
            @NonNull String text,
            int kcal,
            int p,
            int stable
    ) {
        this.date = date;
        this.window = window;
        this.text = text;
        this.kcal = kcal;
        this.p = p;
        this.stable = stable;
    }
}
