package com.personal.scheduler;

final class CategoryStats {
    final long categoryId;
    final String categoryName;
    final int done;
    final int total;

    CategoryStats(long categoryId, String categoryName, int done, int total) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.done = done;
        this.total = total;
    }

    int percent() {
        if (total == 0) {
            return 0;
        }
        return Math.round(done * 100f / total);
    }
}
