-- Una reserva recuperada en otro móvil (con su localizador y su correo) pasa a ser también de ese móvil.
--
-- Hasta ahora «recuperar» solo devolvía la reserva: el móvil que la recuperaba la guardaba, pero `list_reservations` (que se llama con el
-- identificador del móvil) solo traía las que había hecho ese móvil, así que la recuperada no se ponía al día sola (si se cancelaba desde
-- el otro móvil, el nuevo seguía enseñándola confirmada). Ahora, si quien la recupera manda su identificador, se apunta en
-- `reservation_devices` y desde entonces `list_reservations` también la trae.
--
-- No da acceso a nada nuevo: quien tiene el localizador y el correo de una reserva ya puede verla y cancelarla; esto solo evita volver a
-- mandarlos cada vez. Si se borra la reserva (derecho de supresión), se borran sus apuntes (`on delete cascade`).
create table app.reservation_devices (
    reservation_id uuid not null references app.reservations (id) on delete cascade,
    -- Identificador anónimo del móvil que la recuperó (el mismo `X-Device-Id` de la API).
    device_id      uuid not null,
    linked_at      timestamptz not null default now(),
    primary key (reservation_id, device_id)
);

create index reservation_devices_device_idx on app.reservation_devices (device_id, linked_at desc);

-- Como el resto de tablas: seguridad por filas sin políticas y sin permisos para los roles públicos (solo entra la función de borde).
alter table app.reservation_devices enable row level security;
revoke all on app.reservation_devices from public, anon, authenticated;

-- Las reservas de un dispositivo (las que hizo y las que ha recuperado), las más recientes primero.
create or replace function app.list_reservations(p_device uuid) returns jsonb
language sql stable as $$
    select jsonb_build_object('reservations', coalesce(jsonb_agg(app.reservation_json(r) order by r.created_at desc, r.number desc), '[]'::jsonb))
    from (
        select x.*
        from app.reservations x
        where x.device_id = p_device
           or exists (select 1 from app.reservation_devices d where d.reservation_id = x.id and d.device_id = p_device)
        order by x.created_at desc, x.number desc
        limit 100
    ) r
$$;

-- Una reserva por localizador y correo (sirve para recuperarla en otro móvil). Con el identificador de quien la recupera, la deja apuntada
-- a su nombre (si ya era suya o ya estaba apuntada, no hace nada). Si el correo no coincide, no devuelve nada y no apunta nada.
create function app.find_reservation(p_locator text, p_email text, p_device uuid) returns jsonb
language plpgsql as $$
declare
    v_row app.reservations;
begin
    select * into v_row
    from app.reservations r
    where r.locator = upper(btrim(p_locator)) and r.email = lower(btrim(p_email));
    if not found then
        return null;
    end if;
    if p_device is not null and v_row.device_id <> p_device then
        insert into app.reservation_devices (reservation_id, device_id) values (v_row.id, p_device) on conflict do nothing;
    end if;
    return app.reservation_json(v_row);
end;
$$;

-- La versión de dos argumentos sigue valiendo (no apunta nada): así la función de borde anterior no se rompe entre aplicar esta
-- migración y publicar la nueva.
create or replace function app.find_reservation(p_locator text, p_email text) returns jsonb
language sql as $$
    select app.find_reservation(p_locator, p_email, null::uuid)
$$;

revoke all on function app.find_reservation(text, text, uuid) from public, anon, authenticated;
revoke all on function app.find_reservation(text, text) from public, anon, authenticated;
