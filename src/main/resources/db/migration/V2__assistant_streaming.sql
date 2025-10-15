
alter table messages add column if not exists content_delta text;

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'ck_messages_author'
  ) then
alter table messages
    add constraint ck_messages_author
        check (author in ('user','assistant','system'));
end if;
end $$;

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'ck_messages_status'
  ) then
alter table messages
    add constraint ck_messages_status
        check (status in ('draft','streaming','complete','error'));
end if;
end $$;