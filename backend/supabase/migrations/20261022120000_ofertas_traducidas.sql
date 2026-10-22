-- Ofertas traducidas. Cada oferta puede llevar sus textos (etiqueta, título, duración, lugar, extra y descripción) en otros idiomas:
-- una fila por oferta e idioma (`va` valenciano, `en` inglés, `fr` francés) en `app.offer_translations`. El español es el original, el
-- de `app.offers`. Un campo `null` se ve en el idioma original, así que se puede traducir solo una parte. La API los sirve dentro de
-- cada oferta, en `translations`: la app guarda el catálogo entero y elige el idioma sin volver a pedirlo.
--
-- Una oferta nueva con sus traducciones se da de alta con un `insert` en `app.offers` y otro en `app.offer_translations` (ver
-- `backend/README.md`, «Mantenimiento»); no hace falta publicar una versión nueva de la app.

create table app.offer_translations (
    offer_id    text not null references app.offers (id) on delete cascade,
    lang        text not null check (lang in ('va', 'en', 'fr')),
    tag         text,
    title       text,
    duration    text,
    place       text,
    extra       text,
    description text,
    primary key (offer_id, lang),
    check (coalesce(tag, title, duration, place, extra, description) is not null)
);

-- Seguridad: cerrada para la API pública, como el resto del esquema `app`.
alter table app.offer_translations enable row level security;
revoke all on app.offer_translations from public, anon, authenticated;

-- Cambiar una traducción cambia la versión del catálogo (el ETag de la caché de la app), igual que cambiar un horario.
create trigger offer_translations_touch after insert or update or delete on app.offer_translations
    for each row execute function app.touch_offer_from_slot();

-- La oferta con sus traducciones: `translations` es un objeto por idioma con solo los campos traducidos (`{}` si no tiene ninguna).
create or replace function app.offer_json(o app.offers) returns jsonb
language sql stable as $$
    select jsonb_build_object(
        'id', o.id,
        'kind', o.kind,
        'tag', o.tag,
        'title', o.title,
        'duration', o.duration,
        'place', o.place,
        'extra', o.extra,
        'priceFromCents', o.price_from_cents,
        'description', o.description,
        'placeId', o.place_id,
        'slots', coalesce((select jsonb_agg(s.slot order by s.slot) from app.offer_slots s where s.offer_id = o.id), '[]'::jsonb),
        'translations', coalesce((
            select jsonb_object_agg(t.lang, jsonb_strip_nulls(jsonb_build_object(
                'tag', t.tag, 'title', t.title, 'duration', t.duration, 'place', t.place, 'extra', t.extra, 'description', t.description)))
            from app.offer_translations t where t.offer_id = o.id
        ), '{}'::jsonb)
    )
$$;

-- Las traducciones de las 16 ofertas iniciales: las mismas que la app trae incluidas en `assets/i18n/<idioma>/content.json`.
insert into app.offer_translations (offer_id, lang, tag, title, duration, place, extra, description) values
    ('albufera-barca', 'va', 'Excursió', 'Passeig en barca per l’Albufera', '1 h', 'El Palmar', 'castellà/anglés', 'Recorre el llac en una barca elèctrica amb guia bilingüe, entre arrossars i aus migratòries.'),
    ('albufera-barca', 'en', 'Excursion', 'Boat trip on La Albufera', '1 h', 'El Palmar', 'Spanish/English', 'Tour the lake in an electric boat with a bilingual guide, among rice fields and migratory birds.'),
    ('albufera-barca', 'fr', 'Excursion', 'Balade en barque sur L’Albufera', '1 h', 'El Palmar', 'espagnol/anglais', 'Parcourez le lac dans une barque électrique avec un guide bilingue, entre rizières et oiseaux migrateurs.'),
    ('taller-paella', 'va', 'Gastronòmica', 'Taller de paella valenciana', '3 h', 'Mercat Central', 'inclou menjar', 'Aprén a cuinar l’autèntica paella valenciana amb un xef local. Inclou ingredients i degustació.'),
    ('taller-paella', 'en', 'Gastronomic', 'Valencian paella workshop', '3 h', 'Central Market', 'meal included', 'Learn to cook an authentic Valencian paella with a local chef. Ingredients and tasting included.'),
    ('taller-paella', 'fr', 'Gastronomique', 'Atelier de paella valenciana', '3 h', 'Marché central', 'repas inclus', 'Apprenez à cuisiner l’authentique paella valenciana avec un chef local. Ingrédients et dégustation inclus.'),
    ('kayak-albufera', 'va', 'Excursió', 'Caiac per l’Albufera', '2 h', 'El Palmar', 'guia i armilla incloses', 'Ruta en caiac pels canals del Parc Natural a l’alba. Inclou guia, caiac i armilla.'),
    ('kayak-albufera', 'en', 'Excursion', 'Kayaking on La Albufera', '2 h', 'El Palmar', 'guide and life jacket included', 'Kayak route along the canals of the Natural Park at dawn. Guide, kayak and life jacket included.'),
    ('kayak-albufera', 'fr', 'Excursion', 'Kayak sur L’Albufera', '2 h', 'El Palmar', 'guide et gilet inclus', 'Parcours en kayak sur les canaux du parc naturel à l’aube. Guide, kayak et gilet inclus.'),
    ('excursion-albufera-paella', 'va', 'Excursió', 'Excursió a l’Albufera amb paella', '5 h', 'L’Albufera', 'inclou dinar', 'Visita al Parc Natural amb passeig en barca i dinar de paella tradicional inclòs.'),
    ('excursion-albufera-paella', 'en', 'Excursion', 'La Albufera excursion with paella', '5 h', 'La Albufera', 'lunch included', 'Visit to the Natural Park with a boat trip and a traditional paella lunch included.'),
    ('excursion-albufera-paella', 'fr', 'Excursion', 'Excursion à L’Albufera avec paella', '5 h', 'L’Albufera', 'déjeuner inclus', 'Visite du parc naturel avec balade en barque et déjeuner de paella traditionnelle inclus.'),
    ('paddle-surf', 'va', 'Nàutica', 'Paddle surf a Les Arenes', '1,5 h', 'Platja de les Arenes', 'material inclòs', 'Classe de paddle surf per a principiants a la Mediterrània amb monitor titulat. Material inclòs.'),
    ('paddle-surf', 'en', 'Water sports', 'Paddle surfing at Las Arenas', '1.5 h', 'Las Arenas beach', 'equipment included', 'Beginner paddle surf lesson in the Mediterranean with a qualified instructor. Equipment included.'),
    ('paddle-surf', 'fr', 'Nautique', 'Paddle à Las Arenas', '1,5 h', 'Plage de Las Arenas', 'matériel inclus', 'Cours de paddle pour débutants en Méditerranée avec un moniteur diplômé. Matériel inclus.'),
    ('taller-horchata', 'va', 'Gastronòmica', 'Taller d’orxata i fartons', '2 h', 'Mercat Central', 'inclou degustació', 'Aprén a preparar orxata de xufa artesanal i fartons en este taller únic a València.'),
    ('taller-horchata', 'en', 'Gastronomic', 'Horchata and fartons workshop', '2 h', 'Central Market', 'tasting included', 'Learn to make traditional tiger-nut horchata and fartons in this unique workshop in Valencia.'),
    ('taller-horchata', 'fr', 'Gastronomique', 'Atelier d’horchata et de fartons', '2 h', 'Marché central', 'dégustation incluse', 'Apprenez à préparer de l’horchata de souchet artisanale et des fartons dans cet atelier unique à Valence.'),
    ('tour-ruzafa', 'va', 'Gastronòmica', 'Tour Russafa: art i gastronomia', '3 h', 'Russafa', 'degustacions incloses', 'Descobrix el barri més trendy de València amb degustacions en els seus millors bars i visita a galeries d’art.'),
    ('tour-ruzafa', 'en', 'Gastronomic', 'Ruzafa tour: art and food', '3 h', 'Ruzafa', 'tastings included', 'Discover Valencia’s trendiest neighbourhood with tastings in its best bars and visits to art galleries.'),
    ('tour-ruzafa', 'fr', 'Gastronomique', 'Tour de Ruzafa : art et gastronomie', '3 h', 'Ruzafa', 'dégustations incluses', 'Découvrez le quartier le plus tendance de Valence avec des dégustations dans ses meilleurs bars et des visites de galeries d’art.'),
    ('ruta-ciutat-vella', 'va', 'Tour', 'Ruta guiada Ciutat Vella', '2 h', 'Centre històric', 'castellà/anglés', 'Passeig guiat pel Carme, la Llotja, el Mercat Central i la Plaça de l’Ajuntament amb guia oficial.'),
    ('ruta-ciutat-vella', 'en', 'Tour', 'Guided walk through Ciutat Vella', '2 h', 'Historic centre', 'Spanish/English', 'A guided walk through El Carmen, La Lonja, the Central Market and Plaza del Ayuntamiento with an official guide.'),
    ('ruta-ciutat-vella', 'fr', 'Visite', 'Parcours guidé de Ciutat Vella', '2 h', 'Centre historique', 'espagnol/anglais', 'Balade guidée dans El Carmen, la Lonja, le Marché central et la Plaza del Ayuntamiento avec un guide officiel.'),
    ('visita-catedral', 'va', 'Tour', 'Visita guiada a la Catedral', '1,5 h', 'Catedral de València', 'castellà/anglés', 'Descobrix els secrets de la Catedral: el Sant Calze, la capella gòtica i les vistes des del Micalet.'),
    ('visita-catedral', 'en', 'Tour', 'Guided visit to the Cathedral', '1.5 h', 'Valencia Cathedral', 'Spanish/English', 'Discover the Cathedral’s secrets: the Holy Chalice, the Gothic chapel and the views from the Micalet.'),
    ('visita-catedral', 'fr', 'Visite', 'Visite guidée de la cathédrale', '1,5 h', 'Cathédrale de Valence', 'espagnol/anglais', 'Découvrez les secrets de la cathédrale : le Saint Calice, la chapelle gothique et la vue depuis le Micalet.'),
    ('tour-lonja-catedral', 'va', 'Tour', 'Llotja de la Seda i Catedral', '2 h', 'Centre històric', 'guia bilingüe', 'Visita combinada als dos grans monuments del centre històric amb guia bilingüe.'),
    ('tour-lonja-catedral', 'en', 'Tour', 'La Lonja de la Seda and the Cathedral', '2 h', 'Historic centre', 'bilingual guide', 'A combined visit to the two great monuments of the historic centre with a bilingual guide.'),
    ('tour-lonja-catedral', 'fr', 'Visite', 'La Lonja de la Seda et la cathédrale', '2 h', 'Centre historique', 'guide bilingue', 'Visite combinée des deux grands monuments du centre historique avec un guide bilingue.'),
    ('tour-ciudad-artes', 'va', 'Tour', 'Tour Ciutat de les Arts i les Ciències', '2,5 h', 'Ciutat de les Arts i les Ciències', 'entrades incloses', 'Visita guiada al complex de Calatrava amb entrada al Museu de les Ciències i L’Hemisfèric inclosos.'),
    ('tour-ciudad-artes', 'en', 'Tour', 'City of Arts and Sciences tour', '2.5 h', 'City of Arts and Sciences', 'tickets included', 'Guided visit to Calatrava’s complex with admission to the Museu de les Ciències and L’Hemisfèric included.'),
    ('tour-ciudad-artes', 'fr', 'Visite', 'Visite de la Cité des Arts et des Sciences', '2,5 h', 'Cité des Arts et des Sciences', 'billets inclus', 'Visite guidée du complexe de Calatrava avec entrée au Museu de les Ciències et à L’Hemisfèric incluse.'),
    ('ruta-bici', 'va', 'Tour', 'Ruta en bicicleta per València', '2 h', 'Jardí del Túria', 'bicicleta inclosa', 'Pedala pel Jardí del Túria, les platges i la Ciutat de les Arts. Bicicleta inclosa.'),
    ('ruta-bici', 'en', 'Tour', 'Bike ride around Valencia', '2 h', 'Turia Gardens', 'bike included', 'Pedal through the Turia Gardens, the beaches and the City of Arts. Bike included.'),
    ('ruta-bici', 'fr', 'Visite', 'Balade à vélo dans Valence', '2 h', 'Jardin du Turia', 'vélo inclus', 'Pédalez dans le Jardin du Turia, sur les plages et à la Cité des Arts. Vélo inclus.'),
    ('tour-carmen-nocturno', 'va', 'Tour', 'Tour nocturn pel Carme', '2 h', 'Barri del Carme', 'art urbà i llegendes', 'Passeig nocturn pel barri més bohemi de València: art urbà, bars històrics i llegendes medievals.'),
    ('tour-carmen-nocturno', 'en', 'Tour', 'Night tour of El Carmen', '2 h', 'El Carmen district', 'street art and legends', 'A night-time walk through Valencia’s most bohemian neighbourhood: street art, historic bars and medieval legends.'),
    ('tour-carmen-nocturno', 'fr', 'Visite', 'Visite nocturne d’El Carmen', '2 h', 'Quartier d’El Carmen', 'art urbain et légendes', 'Balade nocturne dans le quartier le plus bohème de Valence : art urbain, bars historiques et légendes médiévales.'),
    ('tourist-card-72', 'va', 'Entrades', 'Valencia Tourist Card 72 h', '72 h', 'Tota la ciutat', 'transport i museus', 'La targeta que t’obri València sencera: transport gratis, 14 museus inclosos, tapa de regal i descomptes de fins al 50 % en més de 130 serveis turístics.'),
    ('tourist-card-72', 'en', 'Tickets', 'Valencia Tourist Card 72 h', '72 h', 'The whole city', 'transport and museums', 'The card that opens up the whole of Valencia: free transport, 14 museums included, a free tapa and discounts of up to 50% on more than 130 tourist services.'),
    ('tourist-card-72', 'fr', 'Billets', 'Valencia Tourist Card 72 h', '72 h', 'Toute la ville', 'transports et musées', 'La carte qui vous ouvre tout Valence : transports gratuits, 14 musées inclus, une tapa offerte et jusqu’à 50 % de réduction sur plus de 130 services touristiques.'),
    ('bus-turistico', 'va', 'Entrades', 'Bus Turístic de València', '24–48 h', '17 parades', '100 % elèctric', 'L’autobús turístic de dos pisos, 100 % elèctric, recorre els 17 punts clau de València. Puja i baixa quan vulgues durant 24 o 48 hores.'),
    ('bus-turistico', 'en', 'Tickets', 'Valencia Tourist Bus', '24–48 h', '17 stops', '100% electric', 'The double-decker tourist bus, 100% electric, runs through the 17 key points of Valencia. Hop on and off whenever you like for 24 or 48 hours.'),
    ('bus-turistico', 'fr', 'Billets', 'Bus touristique de Valence', '24–48 h', '17 arrêts', '100 % électrique', 'Le bus touristique à deux étages, 100 % électrique, parcourt les 17 points clés de Valence. Montez et descendez quand vous voulez pendant 24 ou 48 heures.'),
    ('entrada-oceanografic', 'va', 'Entrades', 'Entrada a l’Oceanogràfic', 'Dia complet', 'Ciutat de les Arts i les Ciències', 'delfinari inclòs', 'Entrada al major aquari d’Europa amb l’espectacle del delfinari inclòs. Evita cues comprant en línia.'),
    ('entrada-oceanografic', 'en', 'Tickets', 'Oceanogràfic ticket', 'Full day', 'City of Arts and Sciences', 'dolphinarium included', 'Admission to Europe’s largest aquarium with the dolphinarium show included. Avoid queues by buying online.'),
    ('entrada-oceanografic', 'fr', 'Billets', 'Billet pour l’Oceanogràfic', 'Journée complète', 'Cité des Arts et des Sciences', 'delphinarium inclus', 'Entrée au plus grand aquarium d’Europe avec le spectacle du delphinarium inclus. Évitez les files en achetant en ligne.');
