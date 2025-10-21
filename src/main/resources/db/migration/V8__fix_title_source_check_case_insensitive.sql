
DO $$
BEGIN
    IF EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'ck_threads_title_source') THEN
        ALTER TABLE threads DROP CONSTRAINT ck_threads_title_source;
    END IF;
END
$$;

ALTER TABLE threads
    ADD CONSTRAINT ck_threads_title_source
        CHECK (lower(title_source) IN ('auto','ai','user'));

