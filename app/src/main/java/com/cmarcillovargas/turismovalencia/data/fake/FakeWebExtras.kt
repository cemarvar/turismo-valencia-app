package com.cmarcillovargas.turismovalencia.data.fake

import com.cmarcillovargas.turismovalencia.data.model.Category
import com.cmarcillovargas.turismovalencia.data.model.ContentItem
import com.cmarcillovargas.turismovalencia.data.model.InfoKind.Address
import com.cmarcillovargas.turismovalencia.data.model.InfoKind.Price
import com.cmarcillovargas.turismovalencia.data.model.InfoKind.Schedule
import com.cmarcillovargas.turismovalencia.data.model.InfoKind.Tip
import com.cmarcillovargas.turismovalencia.data.model.InfoKind.Transport
import com.cmarcillovargas.turismovalencia.data.model.InfoRow
import com.cmarcillovargas.turismovalencia.data.model.MapPoint
import com.cmarcillovargas.turismovalencia.data.model.Stat

// TODO(api): fichas que la web lista en sus índices pero que no salen en el Figma: Bioparc y Ciudad de las Artes y las Ciencias
// (en «Qué ver») y Deportes (en «Eventos»). Su contenido completo está en las páginas de la web (assets/web/pages).

/** Lugares de «Qué ver» de la web que no están en el diseño. */
internal val FakeQueVerWeb: List<ContentItem> = listOf(
    ContentItem(
        id = "bioparc",
        category = Category.QueVer,
        subcategory = "Naturaleza",
        eyebrow = "Zoo · Parque de Cabecera",
        title = "Bioparc",
        area = "Parque de Cabecera",
        distanceMeters = 3_200,
        rating = 4.7,
        ratingCount = "8,2 k",
        thumb = null,
        hero = null,
        summary = "Un pedazo de África en Valencia.",
        description = "Más de 6.000 animales de 150 especies en 100.000 m² de hábitats africanos donde las barreras son " +
            "prácticamente invisibles. Un zoológico de nueva generación a orillas del antiguo cauce del Turia.",
        mapPoint = MapPoint(40f, 420f),
        stats = listOf(Stat("6.000", "ANIMALES"), Stat("150", "ESPECIES"), Stat("100.000 m²", "HÁBITATS")),
        info = listOf(
            InfoRow(Schedule, "Horario", "Abierto los 365 días del año"),
            InfoRow(Address, "Dirección", "Av. Pío Baroja, 3 · Parque de Cabecera"),
            InfoRow(Price, "Entrada", "Adulto 32,50 € · reducida 24,00 €"),
            InfoRow(Transport, "Cómo llegar", "Metro L3 y L5 · Nou d’Octubre o Av. del Cid"),
        ),
        offlineMb = 9,
        keywords = listOf("bioparc", "zoo", "animales", "africa", "niños", "familia"),
    ),
    ContentItem(
        id = "cac",
        category = Category.QueVer,
        subcategory = "Museos",
        eyebrow = "Arquitectura · Calatrava",
        title = "Ciudad de las Artes y las Ciencias",
        area = "Antiguo cauce del Turia",
        distanceMeters = 2_100,
        rating = 4.8,
        ratingCount = "21 k",
        thumb = null,
        hero = null,
        summary = "Seis recintos únicos en casi dos kilómetros del antiguo cauce del Turia.",
        description = "Obra de Santiago Calatrava y Félix Candela, combina ciencia interactiva, el mayor acuario de Europa, " +
            "cine en 3D y un palau de les arts. Se recomienda dedicarle un día completo y comprar las entradas online.",
        mapPoint = MapPoint(470f, 605f),
        stats = listOf(Stat("6", "RECINTOS"), Stat("2 km", "DE LARGO"), Stat("47,75 €", "PACK 3 RECINTOS")),
        info = listOf(
            InfoRow(Schedule, "Horario", "Según recinto · consulta los horarios online"),
            InfoRow(Address, "Dirección", "Av. del Professor López Piñero, 7"),
            InfoRow(Price, "Entradas", "Pack de 3 recintos desde 47,75 €"),
            InfoRow(Transport, "Cómo llegar", "Metro L3, L5, L7 y L9 · Alameda · 15 min por el Jardín del Turia"),
        ),
        offlineMb = 12,
        keywords = listOf("cac", "ciudad de las artes", "ciencias", "calatrava", "hemisferic", "museu", "umbracle", "palau"),
    ),
)

/** Eventos de la web que no están en el diseño. */
internal val FakeEventosWeb: List<ContentItem> = listOf(
    ContentItem(
        id = "deportes",
        onMap = false,
        category = Category.Eventos,
        subcategory = "Deportes",
        eyebrow = "Deporte · 365 días",
        title = "Deportes",
        area = "Toda la ciudad",
        distanceMeters = 0,
        rating = null,
        ratingCount = null,
        thumb = null,
        hero = null,
        summary = "Valencia, ciudad del running, del ciclismo y del mar.",
        description = "El clima mediterráneo, 9 kilómetros de parque lineal, 20 km de playa y 160 km de carril bici hacen de " +
            "Valencia el destino ideal para practicar deporte durante todo el año. Maratón, carreras populares, vela y mucho más.",
        dateLabel = "Todo el año",
        mapPoint = MapPoint(300f, 500f),
        stats = listOf(Stat("9 km", "JARDÍN DEL TURIA"), Stat("20 km", "DE PLAYA"), Stat("160 km", "CARRIL BICI")),
        info = listOf(
            InfoRow(Schedule, "Cuándo", "Todo el año · el Maratón de Valencia es el 6 de diciembre"),
            InfoRow(Tip, "Consejo", "Reserva con mucha antelación la inscripción del Maratón: se agota en horas"),
        ),
        isFree = true,
        offlineMb = 6,
        keywords = listOf("deporte", "running", "maraton", "ciclismo", "bici", "vela", "carrera"),
    ),
)
