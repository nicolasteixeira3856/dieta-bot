package com.nutri.android.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.util.Arrays;
import java.util.List;

@Entity(tableName = "profile")
public class ProfileEntity {
    @PrimaryKey
    public int id = 1;
    @NonNull
    public String ceilingMode = "same";
    public int kcalSame = 2000;
    public int kcalWeekday = 2000;
    public int kcalWeekend = 2300;
    @NonNull
    public List<Integer> kcalDays = Arrays.asList(2000, 2000, 2000, 2000, 2000, 2000, 2000);
    @NonNull
    public String eat = "zero";
    public int pct = 50;
    public int onboardingDone = 0;
    @NonNull
    public String firstDay = "";

    public ProfileEntity() {}

    @Ignore
    public ProfileEntity(
            int id,
            @NonNull String ceilingMode,
            int kcalSame,
            int kcalWeekday,
            int kcalWeekend,
            @NonNull List<Integer> kcalDays,
            @NonNull String eat,
            int pct,
            int onboardingDone,
            @NonNull String firstDay
    ) {
        this.id = id;
        this.ceilingMode = ceilingMode;
        this.kcalSame = kcalSame;
        this.kcalWeekday = kcalWeekday;
        this.kcalWeekend = kcalWeekend;
        this.kcalDays = kcalDays;
        this.eat = eat;
        this.pct = pct;
        this.onboardingDone = onboardingDone;
        this.firstDay = firstDay;
    }
}
