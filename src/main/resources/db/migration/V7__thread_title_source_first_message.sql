ALTER TABLE threads ADD COLUMN IF NOT EXISTS title_source TEXT NOT NULL DEFAULT 'auto';
ALTER TABLE threads ADD COLUMN IF NOT EXISTS first_message_id uuid NULL;
ALTER TABLE threads ADD CONSTRAINT fk_threads_first_message FOREIGN KEY (first_message_id) REFERENCES messages (id);
DO $$
    BEGIN IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname='ck_threads_title_source') THEN ALTER TABLE threads
    ADD CONSTRAINT ck_threads_title_source CHECK (title_source IN ('auto', 'ai', 'user')); END IF; END
    $$;