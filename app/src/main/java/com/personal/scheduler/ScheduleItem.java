package com.personal.scheduler;

final class ScheduleItem {
    final long id;
    final long categoryId;
    final String content;
    final String amount;
    final int repeatMask;

    ScheduleItem(long id, long categoryId, String content, String amount, int repeatMask) {
        this.id = id;
        this.categoryId = categoryId;
        this.content = content;
        this.amount = amount;
        this.repeatMask = repeatMask;
    }

    boolean repeatsOn(int calendarDayOfWeek) {
        int bit = DateText.dayBit(calendarDayOfWeek);
        return (repeatMask & bit) != 0;
    }
}
