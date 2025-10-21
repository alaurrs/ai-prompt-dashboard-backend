-- 1) Mémoire de profil (1:1 avec user)
CREATE TABLE IF NOT EXISTS user_memory (
                             user_id UUID PRIMARY KEY REFERENCES users(id),
                             profile_json JSONB NOT NULL DEFAULT '{}'::jsonb,
                             updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2) Résumé par thread (1:1 avec thread)
CREATE TABLE IF NOT EXISTS thread_summary (
                                thread_id UUID PRIMARY KEY REFERENCES threads(id),
                                summary_text TEXT NOT NULL DEFAULT '',
                                tokens_estimated INT NOT NULL DEFAULT 0,
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3) Épisodes (N par user)
CREATE TABLE IF NOT EXISTS memory_episode (
                                id UUID PRIMARY KEY,
                                user_id UUID NOT NULL REFERENCES users(id),
                                thread_id UUID NULL REFERENCES threads(id),
                                occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                title TEXT NOT NULL,
                                detail TEXT NOT NULL,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 4) Mémoire sémantique (pgvector)
CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE IF NOT EXISTS memory_chunk (
                              id UUID PRIMARY KEY,
                              user_id UUID NOT NULL REFERENCES users(id),
                              thread_id UUID NULL REFERENCES threads(id),
                              source TEXT NOT NULL, -- 'user', 'assistant', 'system', 'note'
                              content TEXT NOT NULL,
                              embedding vector(1536) NOT NULL, -- taille selon le modèle d'embedding choisi
                              created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ON memory_chunk (user_id);
CREATE INDEX ON memory_chunk USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
