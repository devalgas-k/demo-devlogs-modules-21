package com.example.pagination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class PaginationTest {
    JdbcTemplate jdbc;
    FeedRepository repository;
    PaginationService service;
    CursorCodec codec;
    final byte[] key = "test-only-key-with-at-least-32-bytes".getBytes(StandardCharsets.UTF_8);
    final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    @BeforeEach void prepare() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(ds);
        jdbc = new JdbcTemplate(ds); repository = new FeedRepository(jdbc);
        codec = new CursorCodec(key, clock); service = new PaginationService(repository, codec);
        for (int i = 1; i <= 30; i++) insert(1, i);
        insert(2, 999);
    }
    void insert(long tenant, long id) {
        jdbc.update("INSERT INTO feed VALUES (?,?,TIMESTAMP '2026-01-01 00:00:00',?)", tenant, id, "item-" + id);
    }
    List<Long> ids(List<FeedRepository.Item> items) { return items.stream().map(FeedRepository.Item::id).toList(); }
    @Test void traversalMatchesOffsetDespiteTiesAndFiltersOtherTenants() {
        var collected = new ArrayList<Long>(); String cursor = null;
        do {
            var page = service.page(1, 7, cursor); collected.addAll(ids(page.items())); cursor = page.nextCursor();
        } while (cursor != null);
        assertThat(collected).containsExactlyElementsOf(ids(repository.offset(1, 0, 100)));
        assertThat(collected).hasSize(30).doesNotHaveDuplicates().doesNotContain(999L);
    }
    @Test void insertBeforeBoundaryDuplicatesOffsetButNotKeyset() {
        var first = service.page(1, 5, null); insert(1, 31);
        assertThat(ids(repository.offset(1, 5, 5))).contains(26L);
        assertThat(ids(service.page(1, 5, first.nextCursor()).items())).containsExactly(25L,24L,23L,22L,21L);
    }
    @Test void deletingBoundaryShiftsOffsetButKeysetNeedsNoExistingAnchor() {
        var first = service.page(1, 5, null);
        jdbc.update("DELETE FROM feed WHERE tenant_id=1 AND id=26");
        assertThat(ids(repository.offset(1, 5, 5))).containsExactly(24L,23L,22L,21L,20L);
        assertThat(ids(service.page(1, 5, first.nextCursor()).items())).containsExactly(25L,24L,23L,22L,21L);
    }
    @Test void cursorRejectsTamperingMismatchAndExpiration() {
        String token = service.page(1, 5, null).nextCursor();
        assertThatThrownBy(() -> service.page(2, 5, token)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.page(1, 6, token)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.page(1, 5, "!" + token)).isInstanceOf(IllegalArgumentException.class);
        var expired = new CursorCodec(key, Clock.offset(clock, Duration.ofMinutes(16)));
        assertThatThrownBy(() -> expired.decode(token, 1, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.decode("x".repeat(1025), 1, 5)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void validBase64TamperingIsRejected() {
        String token = service.page(1,5,null).nextCursor();
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String changed = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.replace("v1|1|", "v1|2|").getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> codec.decode(changed + "." + parts[1],2,5)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void mutableSortKeyCanOmitAnUnseenItemEvenWithKeyset() {
        var first = service.page(1,5,null);
        jdbc.update("UPDATE feed SET created_at=TIMESTAMP '2027-01-01 00:00:00' WHERE tenant_id=1 AND id=25");
        assertThat(ids(service.page(1,5,first.nextCursor()).items())).containsExactly(24L,23L,22L,21L,20L);
    }
    @Test void pageSizeAndOffsetBoundsAreValidated() {
        for (int size : new int[]{-1,0,101}) assertThatThrownBy(() -> service.page(1,size,null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.offset(1,-1,5)).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.page(3,5,null).items()).isEmpty();
        assertThat(service.page(1,100,null).nextCursor()).isNull();
    }
    @Test void timestampPrecisionSurvivesTokenRoundTrip() {
        var boundary = new FeedRepository.Boundary(LocalDateTime.parse("2026-01-01T01:02:03.123456"),42);
        assertThat(codec.decode(codec.encode(1,5,boundary),1,5)).isEqualTo(boundary);
    }
}
