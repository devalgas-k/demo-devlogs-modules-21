package com.example.pagination;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class FeedRepository {
    public record Item(long id, LocalDateTime createdAt, String payload) {}
    public record Boundary(LocalDateTime createdAt, long id) {}
    private final JdbcTemplate jdbc;
    public FeedRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private static final String COLUMNS = "SELECT id, created_at, payload FROM feed WHERE tenant_id = ?";
    private List<Item> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> new Item(rs.getLong("id"),
                rs.getTimestamp("created_at").toLocalDateTime(), rs.getString("payload")), args);
    }
    public List<Item> offset(long tenant, long offset, int size) {
        validateSize(size);
        if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        return query(COLUMNS + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?", tenant, size, offset);
    }
    public List<Item> seek(long tenant, Boundary after, int limit) {
        if (limit < 1 || limit > 101) throw new IllegalArgumentException("limit must be 1..101");
        if (after == null) return query(COLUMNS + " ORDER BY created_at DESC, id DESC LIMIT ?", tenant, limit);
        return query(COLUMNS + " AND (created_at, id) < (?, ?) ORDER BY created_at DESC, id DESC LIMIT ?",
                tenant, Timestamp.valueOf(after.createdAt()), after.id(), limit);
    }
    public static void validateSize(int size) {
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be 1..100");
    }
}
