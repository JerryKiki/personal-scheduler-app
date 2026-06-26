package com.personal.scheduler;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

final class DateText {
    private static final String[] KOREAN_DAYS = {"일", "월", "화", "수", "목", "금", "토"};

    private DateText() {
    }

    static String todayHeader() {
        Calendar cal = Calendar.getInstance();
        String md = new SimpleDateFormat("M/d", Locale.KOREA).format(cal.getTime());
        return md + " " + KOREAN_DAYS[cal.get(Calendar.DAY_OF_WEEK) - 1];
    }

    static int todayBit() {
        return dayBit(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
    }

    static int dayBit(int calendarDayOfWeek) {
        return 1 << (calendarDayOfWeek - 1);
    }

    static String repeatText(int mask) {
        if (mask == 0) {
            return "반복 없음";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < KOREAN_DAYS.length; i++) {
            if ((mask & (1 << i)) != 0) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(KOREAN_DAYS[i]);
            }
        }
        return builder.toString();
    }
}
