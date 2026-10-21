\set ON_ERROR_STOP on
\timing on
SELECT version();
SHOW shared_buffers;
SHOW work_mem;
CREATE TABLE IF NOT EXISTS feed (
 tenant_id BIGINT NOT NULL, id BIGINT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 payload VARCHAR(200) NOT NULL, PRIMARY KEY (tenant_id,id)
);
CREATE INDEX IF NOT EXISTS feed_seek_idx ON feed(tenant_id,created_at DESC,id DESC);
-- Dedicated disposable database only. Tenant 42 reserves the benchmark dataset.
DELETE FROM feed WHERE tenant_id=42;
INSERT INTO feed
SELECT 42, g, TIMESTAMP '2026-01-01 00:00:00' + (g / 10) * INTERVAL '1 second', repeat('x',100)
FROM generate_series(1,1000000) g;
VACUUM (ANALYZE) feed;
-- Warm both paths once. Boundary id=100001 is the last row before offset=900000.
SELECT id FROM feed WHERE tenant_id=42 ORDER BY created_at DESC,id DESC LIMIT 50 OFFSET 900000;
SELECT id FROM feed WHERE tenant_id=42 AND (created_at,id) < (TIMESTAMP '2026-01-01 02:46:40',100001)
ORDER BY created_at DESC,id DESC LIMIT 50;
\echo OFFSET_FIRST
EXPLAIN (ANALYZE,BUFFERS,SETTINGS) SELECT id,created_at,payload FROM feed WHERE tenant_id=42
ORDER BY created_at DESC,id DESC LIMIT 50 OFFSET 0;
\echo OFFSET_DEEP_1
EXPLAIN (ANALYZE,BUFFERS,SETTINGS) SELECT id,created_at,payload FROM feed WHERE tenant_id=42
ORDER BY created_at DESC,id DESC LIMIT 50 OFFSET 900000;
\echo KEYSET_DEEP_1
EXPLAIN (ANALYZE,BUFFERS,SETTINGS) SELECT id,created_at,payload FROM feed WHERE tenant_id=42
AND (created_at,id) < (TIMESTAMP '2026-01-01 02:46:40',100001)
ORDER BY created_at DESC,id DESC LIMIT 50;
\echo OFFSET_DEEP_2
EXPLAIN (ANALYZE,BUFFERS,SETTINGS) SELECT id,created_at,payload FROM feed WHERE tenant_id=42
ORDER BY created_at DESC,id DESC LIMIT 50 OFFSET 900000;
\echo KEYSET_DEEP_2
EXPLAIN (ANALYZE,BUFFERS,SETTINGS) SELECT id,created_at,payload FROM feed WHERE tenant_id=42
AND (created_at,id) < (TIMESTAMP '2026-01-01 02:46:40',100001)
ORDER BY created_at DESC,id DESC LIMIT 50;
\echo OFFSET_DEEP_3
EXPLAIN (ANALYZE,BUFFERS,SETTINGS) SELECT id,created_at,payload FROM feed WHERE tenant_id=42
ORDER BY created_at DESC,id DESC LIMIT 50 OFFSET 900000;
\echo KEYSET_DEEP_3
EXPLAIN (ANALYZE,BUFFERS,SETTINGS) SELECT id,created_at,payload FROM feed WHERE tenant_id=42
AND (created_at,id) < (TIMESTAMP '2026-01-01 02:46:40',100001)
ORDER BY created_at DESC,id DESC LIMIT 50;
-- Exact equality of the selected rows, independently of timing.
WITH a AS (SELECT id FROM feed WHERE tenant_id=42 ORDER BY created_at DESC,id DESC LIMIT 50 OFFSET 900000),
b AS (SELECT id FROM feed WHERE tenant_id=42 AND (created_at,id)<(TIMESTAMP '2026-01-01 02:46:40',100001)
ORDER BY created_at DESC,id DESC LIMIT 50)
SELECT (SELECT array_agg(id ORDER BY id DESC) FROM a) = (SELECT array_agg(id ORDER BY id DESC) FROM b) AS identical_pages;
