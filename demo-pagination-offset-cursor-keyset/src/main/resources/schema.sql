CREATE TABLE IF NOT EXISTS feed (
  tenant_id BIGINT NOT NULL,
  id BIGINT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  payload VARCHAR(200) NOT NULL,
  PRIMARY KEY (tenant_id, id)
);
CREATE INDEX IF NOT EXISTS feed_seek_idx ON feed (tenant_id, created_at DESC, id DESC);
