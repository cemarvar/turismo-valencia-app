-- Cancelar una reserva.
--
-- Quién puede cancelarla: el dispositivo que la hizo, o quien tenga su localizador y el correo con el que se hizo (la recuperó en otro
-- móvil). Si no es ninguna de las dos cosas, el resultado es el mismo que si la reserva no existiera: así no se averigua qué localizadores hay.
-- Cuándo: hasta que empieza la actividad (si su día es hoy, hasta la hora del horario; un horario «Cualquier hora» vale hasta que acaba el
-- día). Cancelar una reserva ya cancelada no es un error: devuelve la misma reserva (la app puede reintentar sin miedo).
-- Las plazas se liberan solas: el aforo solo cuenta las reservas confirmadas (`create_reservation`).
--
-- Devuelve {changed, reservation}. Errores de negocio: reservation_not_found (404) · reservation_past (409).
create function app.cancel_reservation(p_device uuid, p_locator text, p_email text) returns jsonb
language plpgsql as $$
declare
    v_today    date := (now() at time zone 'Europe/Madrid')::date;
    v_now_time time := (now() at time zone 'Europe/Madrid')::time;
    v_email    text := lower(btrim(coalesce(p_email, '')));
    v_row      app.reservations;
begin
    select * into v_row
    from app.reservations
    where locator = upper(btrim(p_locator))
      and (device_id = p_device or (v_email <> '' and email = v_email))
    for update;
    if not found then
        raise exception 'reservation_not_found';
    end if;

    if v_row.status = 'cancelled' then
        return jsonb_build_object('changed', false, 'reservation', app.reservation_json(v_row));
    end if;

    if v_row.activity_date < v_today
       or (v_row.activity_date = v_today and v_row.slot ~ '^[0-9]{2}:[0-9]{2}$' and v_row.slot::time <= v_now_time) then
        raise exception 'reservation_past';
    end if;

    update app.reservations set status = 'cancelled' where id = v_row.id returning * into v_row;
    return jsonb_build_object('changed', true, 'reservation', app.reservation_json(v_row));
end;
$$;

-- Como el resto de funciones del esquema: nadie salvo el propietario puede ejecutarla.
revoke all on function app.cancel_reservation(uuid, text, text) from public, anon, authenticated;
