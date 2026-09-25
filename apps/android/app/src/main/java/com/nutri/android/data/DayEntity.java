package com.nutri.android.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "day")
public class DayEntity {
    @PrimaryKey
    @NonNull
    public String date = "";
    @Nullable
    public Integer workoutKcal;
    @NonNull
    public List<String> removedWindows = new ArrayList<>();
    @NonNull
    public List<String> askedWindows = new ArrayList<>();

    public DayEntity() {}

    @Ignore
    public DayEntity(
            @NonNull String date,
            @Nullable Integer workoutKcal,
            @NonNull List<String> removedWindows,
            @NonNull List<String> askedWindows
    ) {
        this.date = date;
        this.workoutKcal = workoutKcal;
        this.removedWindows = removedWindows;
        this.askedWindows = askedWindows;
    }
}
