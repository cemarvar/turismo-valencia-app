-- Esquema de Turismo Valencia, hecho desde cero para la app.
--
-- Todo vive en el esquema `app`, que NO se expone por la API automática de Supabase (solo `public` y `graphql_public` lo están,
-- ver `config.toml`). Además cada tabla tiene la seguridad por filas activada y sin ninguna política, y se retiran los permisos
-- de los roles `anon` y `authenticated`: por la clave pública no se puede leer ni escribir nada. La única puerta es la función
-- `api-v1` (supabase/functions/api-v1), que se conecta con su propia cadena de conexión.

create schema if not exists app;

revoke all on schema app from public;
revoke all on schema app from anon, authenticated;

-- ---------------------------------------------------------------------------------------------------------------------------
-- Ofertas: entradas, tours y experiencias reservables
-- ---------------------------------------------------------------------------------------------------------------------------
create table app.offers (
    id               text primary key check (id ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    kind             text not null check (kind in ('entrada', 'tour', 'experiencia')),
    tag              text not null,
    title            text not null,
    duration         text not null,
    place            text not null,
    -- Idiomas o lo que incluye: «español/inglés», «incluye comida».
    extra            text not null default '',
    -- Precio por adulto, en céntimos (los niños no pagan). Con enteros no hay errores de redondeo.
    price_from_cents integer not null check (price_from_cents >= 0),
    description      text not null,
    -- Ficha del lugar en la app donde empieza la actividad (para «Cómo llegar»).
    place_id         text,
    -- Orden en el que se enseñan las ofertas dentro de su tipo.
    position         smallint not null default 0,
    active           boolean not null default true,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now()
);

-- Horarios de cada oferta. `capacity` son las plazas por día y horario; `null` = sin límite.
create table app.offer_slots (
    offer_id text not null references app.offers (id) on delete cascade,
    slot     text not null check (slot ~ '^([01][0-9]|2[0-3]):[0-5][0-9]$' or slot = 'Cualquier hora'),
    capacity integer check (capacity is null or capacity > 0),
    primary key (offer_id, slot)
);

-- ---------------------------------------------------------------------------------------------------------------------------
-- Reservas
-- ---------------------------------------------------------------------------------------------------------------------------
-- Los localizadores son TV-<año de la actividad>-<número>. Empiezan en el 1000 para no coincidir con los de demostración
-- de la app (413 a 415), que son solo locales.
create sequence app.reservation_number_seq start with 1000 minvalue 1000;

create table app.reservations (
    id              uuid primary key default gen_random_uuid(),
    number          integer not null unique default nextval('app.reservation_number_seq'),
    locator         text not null unique check (locator ~ '^TV-[0-9]{4}-[0-9]{4,}$'),
    offer_id        text not null references app.offers (id),
    -- Copia de lo reservado: si la oferta se edita, la reserva sigue diciendo lo que se contrató.
    offer_title     text not null,
    offer_place     text not null,
    activity_date   date not null,
    slot            text not null,
    adults          smallint not null check (adults between 1 and 20),
    children        smallint not null default 0 check (children between 0 and 20),
    email           text not null check (length(email) <= 254 and email = lower(email) and email ~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$'),
    total_cents     integer not null check (total_cents >= 0),
    status          text not null default 'confirmed' check (status in ('confirmed', 'cancelled')),
    -- Identificador anónimo del móvil (lo genera la app): es lo que une a cada persona con sus reservas, sin cuentas.
    device_id       uuid not null,
    -- Clave que genera la app por cada intento de reservar: si la red falla y reintenta, no se duplica la reserva.
    idempotency_key uuid not null,
    created_at      timestamptz not null default now(),
    cancelled_at    timestamptz,
    constraint reservations_slot_fk foreign key (offer_id, slot) references app.offer_slots (offer_id, slot),
    constraint reservations_idempotency unique (device_id, idempotency_key)
);

create index reservations_device_idx on app.reservations (device_id, created_at desc);
create index reservations_email_idx on app.reservations (email);
-- Para contar las plazas ocupadas de un día y horario.
create index reservations_seats_idx on app.reservations (offer_id, activity_date, slot) where status = 'confirmed';

-- ---------------------------------------------------------------------------------------------------------------------------
-- Límite de peticiones (por IP, dispositivo y correo): contadores por ventana de tiempo
-- ---------------------------------------------------------------------------------------------------------------------------
create table app.rate_limits (
    bucket       text not null,
    window_start timestamptz not null,
    hits         integer not null default 0,
    primary key (bucket, window_start)
);

-- ---------------------------------------------------------------------------------------------------------------------------
-- La fecha de cambio de una oferta sube sola, y también cuando cambian sus horarios (sirve de versión para la caché de la app)
-- ---------------------------------------------------------------------------------------------------------------------------
create function app.touch_offer() returns trigger
language plpgsql as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

create trigger offers_touch before update on app.offers
    for each row execute function app.touch_offer();

create function app.touch_offer_from_slot() returns trigger
language plpgsql as $$
begin
    update app.offers set updated_at = now() where id = coalesce(new.offer_id, old.offer_id);
    return null;
end;
$$;

create trigger offer_slots_touch after insert or update or delete on app.offer_slots
    for each row execute function app.touch_offer_from_slot();

-- ---------------------------------------------------------------------------------------------------------------------------
-- Seguridad: cerrado para la API pública
-- ---------------------------------------------------------------------------------------------------------------------------
alter table app.offers enable row level security;
alter table app.offer_slots enable row level security;
alter table app.reservations enable row level security;
alter table app.rate_limits enable row level security;

revoke all on all tables in schema app from public, anon, authenticated;
revoke all on all sequences in schema app from public, anon, authenticated;
alter default privileges in schema app revoke all on tables from public, anon, authenticated;
alter default privileges in schema app revoke all on sequences from public, anon, authenticated;
alter default privileges in schema app revoke all on functions from public, anon, authenticated;
