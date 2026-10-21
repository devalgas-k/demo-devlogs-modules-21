package com.example.pagination;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        try (var context = SpringApplication.run(DemoApplication.class, args)) {
            var jdbc = context.getBean(JdbcTemplate.class);
            // Idempotent, isolated demo tenant; never deletes existing data.
            for (long id = 1; id <= 30; id++) {
                jdbc.update("INSERT INTO feed (tenant_id,id,created_at,payload) SELECT 1,?,TIMESTAMP '2026-01-01 00:00:00',? "
                        + "WHERE NOT EXISTS (SELECT 1 FROM feed WHERE tenant_id=1 AND id=?)", id, "item-" + id, id);
            }
            // Public demo key only. A deployed API must obtain a stable private key from secret management.
            var codec = new CursorCodec("demo-only-key-do-not-use-in-production".getBytes(StandardCharsets.UTF_8), Clock.systemUTC());
            var service = new PaginationService(context.getBean(FeedRepository.class), codec);
            var first = service.page(1, 5, null);
            var second = service.page(1, 5, first.nextCursor());
            System.out.println("first=" + first.items().stream().map(FeedRepository.Item::id).toList());
            System.out.println("second=" + second.items().stream().map(FeedRepository.Item::id).toList());
            System.out.println("cursorPresent=" + (second.nextCursor() != null));
        }
    }
}
