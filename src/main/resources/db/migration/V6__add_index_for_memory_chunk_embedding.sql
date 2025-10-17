CREATE INDEX IF NOT EXISTS idx_memory_chunk_embedding
    ON memory_chunk USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);