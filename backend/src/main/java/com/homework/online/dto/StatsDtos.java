package com.homework.online.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 工作台统计 DTO */
public final class StatsDtos {

    private StatsDtos() {
    }

    public static class StatCard {
        public String label;
        public String value;
        public String unit;
        public String icon;
        public boolean highlight;

        public StatCard(String label, String value, String unit, String icon, boolean highlight) {
            this.label = label;
            this.value = value;
            this.unit = unit;
            this.icon = icon;
            this.highlight = highlight;
        }
    }

    public static class TodoItem {
        public String kind;
        public Long id;
        public Long assignmentId;
        public String title;
        public String subtitle;
        public String meta;
        public LocalDateTime submittedAt;
        public LocalDateTime dueAt;
        public boolean late;
        public boolean overdue;
    }

    public static class RecentItem {
        public String kind;
        public Long id;
        public Long assignmentId;
        public String title;
        public String subtitle;
        public String meta;
        public String status;
        public LocalDateTime dueAt;
        public LocalDateTime gradedAt;
    }

    public static class OverviewResponse {
        public String role;
        public List<StatCard> cards;
        public List<TodoItem> todo;
        public List<RecentItem> recent;
    }
}
