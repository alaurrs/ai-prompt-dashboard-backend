CREATE INDEX IF NOT EXISTS idx_memory_episode_user_time
    ON memory_episode (user_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_memory_episode_thread_time
    ON memory_episode (thread_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_memory_chunk_user_time
    ON memory_chunk (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_memory_chunk_thread_time
    ON memory_chunk (thread_id, created_at DESC);