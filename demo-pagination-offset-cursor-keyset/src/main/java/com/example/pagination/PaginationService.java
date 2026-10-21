package com.example.pagination;

import java.util.List;

public final class PaginationService {
    public record Page(List<FeedRepository.Item> items, String nextCursor) {}
    private final FeedRepository repository;
    private final CursorCodec codec;
    public PaginationService(FeedRepository repository, CursorCodec codec) {
        this.repository = repository; this.codec = codec;
    }
    public Page page(long tenant, int size, String cursor) {
        FeedRepository.validateSize(size);
        var boundary = cursor == null ? null : codec.decode(cursor, tenant, size);
        var fetched = repository.seek(tenant, boundary, size + 1);
        boolean more = fetched.size() > size;
        var items = List.copyOf(fetched.subList(0, Math.min(size, fetched.size())));
        String next = null;
        if (more) {
            var last = items.getLast();
            next = codec.encode(tenant, size, new FeedRepository.Boundary(last.createdAt(), last.id()));
        }
        return new Page(items, next);
    }
}
