create extension if not exists pgcrypto;
create extension if not exists pg_trgm;

create table if not exists users (
    id uuid primary key default gen_random_uuid(),
    email text unique not null,
    display_name text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists threads (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id),
    title text,
    model text,
    status text not null default 'active',
    system_prompt text,
    summary text,
    metadata jsonb,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_threads_user_updated on threads(user_id, updated_at desc);

create table if not exists messages (
    id uuid primary key default gen_random_uuid(),
    thread_id uuid not null references threads(id),
    author text not null,
    position int not null,
    status text not null default 'complete',
    content text,
    model text,
    usage_prompt_tokens int,
    usage_completion_tokens int,
    latency_ms int,
    error_code text,
    error_message text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_messages_thread_pos on messages(thread_id, position);