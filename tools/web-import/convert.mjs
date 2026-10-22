// Convierte las páginas JSX de la web en JSON normalizado para la app (assets/web/).
// Uso: node fetch.mjs && node convert.mjs && python images.py   (desde tools/web-import)
import fs from 'fs';
import path from 'path';
import vm from 'vm';

// Carpeta de trabajo: la crea fetch.mjs (por defecto ./work junto a este script).
const T = process.env.WEB_IMPORT_WORK ?? path.resolve('work');
const DIR = path.join(T, 'web', 'pages');
const CSSDIR = path.join(T, 'web', 'css');
fs.mkdirSync(CSSDIR, { recursive: true });
const RAW = 'https://raw.githubusercontent.com/cemarvar/TurismoValencia/main/';

// ---------- utilidades ----------
function matchBalanced(src, start) {
  const open = src[start];
  let depth = 0, inStr = null, esc = false;
  for (let i = start; i < src.length; i++) {
    const c = src[i];
    if (inStr) { if (esc) { esc = false; continue; } if (c === '\\') { esc = true; continue; } if (c === inStr) inStr = null; continue; }
    if (c === '"' || c === "'" || c === '`') { inStr = c; continue; }
    if (c === '/' && src[i + 1] === '/') { while (i < src.length && src[i] !== '\n') i++; continue; }
    if (c === '/' && src[i + 1] === '*') { i = src.indexOf('*/', i) + 1; continue; }
    if (c === '[' || c === '{') depth++;
    if (c === ']' || c === '}') { depth--; if (depth === 0) return i; }
  }
  return -1;
}
const clean = (s) => String(s ?? '')
  .replace(/\\'/g, "'") // un `l\'Olla` suelto en el JSX de la web
  .replace(/<br\s*\/?>/gi, ' ')
  .replace(/<[^>]+>/g, '')
  .replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').replace(/&middot;/g, '·').replace(/&mdash;/g, '—').replace(/&ndash;/g, '–')
  .replace(/&aacute;/g, 'á').replace(/&eacute;/g, 'é').replace(/&iacute;/g, 'í').replace(/&oacute;/g, 'ó').replace(/&uacute;/g, 'ú').replace(/&ntilde;/g, 'ñ')
  .replace(/\s+/g, ' ').trim();

async function fetchCss(repoPath) {
  const f = path.join(CSSDIR, repoPath.replace(/\//g, '__'));
  if (fs.existsSync(f)) return fs.readFileSync(f, 'utf8');
  const res = await fetch(RAW + repoPath);
  const txt = res.ok ? await res.text() : '';
  fs.writeFileSync(f, txt);
  return txt;
}

function parseCssImages(css) {
  const map = {}; let hero = null;
  const re = /([^{}]+)\{([^{}]*)\}/g; let m;
  while ((m = re.exec(css))) {
    const u = /background(?:-image)?\s*:[^;]*url\(\s*['"]?([^'")]+)['"]?\s*\)/.exec(m[2]);
    if (!u) continue;
    const img = u[1].replace(/^\/?img\//, '');
    for (const sel of m[1].split(',')) {
      const s = sel.trim();
      const cls = /\.([\w-]+)\s*$/.exec(s);
      if (!cls) continue;
      map[cls[1]] = img;
      if (!hero && /hero/i.test(cls[1]) && !/overlay|content|deco/i.test(cls[1])) hero = img;
    }
  }
  return { map, hero };
}

const HUMAN = {
  precio: 'Precio', horario: 'Horario', ubicacion: 'Ubicación', duracion: 'Duración', edades: 'Edades', fecha: 'Fecha', nivel: 'Nivel',
  mejor_epoca: 'Mejor época', distancia: 'Distancia', arquitecto: 'Arquitecto', ano: 'Año', altura: 'Altura', estilo: 'Estilo', autor: 'Autor',
  barrio: 'Barrio', direccion: 'Dirección', dir: 'Dirección', lugar: 'Lugar', genero: 'Género', mes: 'Mes', especialidad: 'Especialidad',
  fundacion: 'Fundación', nota: 'Nota', entrada: 'Entrada', cobertura: 'Cobertura', impacto: 'Impacto', edad: 'Edad', web: 'Web',
  recomendado: 'Recomendado', dato: 'Dato clave', obra: 'Obra destacada', desde: 'Desde', incluye: 'Incluye', coleccion: 'Colección',
  programacion: 'Programación', reducida: 'Tarifa reducida', cantidad: 'Cantidad', donde: 'Dónde', mercados: 'Mercados', dias: 'Días',
  curiosidad: 'Curiosidad', visita: 'Visita', historia: 'Historia', arquitectura: 'Arquitectura', consejo: 'Consejo', consejo_clave: 'Consejo clave',
  tipo: 'Tipo', categoria: 'Categoría', zona: 'Zona', region: 'Zona', area: 'Área', momentos: 'Momentos', gratuito: 'Gratuito', mesNum: 'Mes',
  count: 'Eventos', rango: 'Fechas', animales: 'Animales', especies: 'Especies', artistas: 'Artistas', ingredientes: 'Ingredientes',
  opcionales: 'Opcionales', destacados: 'Destacados', puntos: 'Puntos clave', actividades: 'Actividades', detalles: 'Detalles', acciones: 'Acciones',
  lugares: 'Lugares', itinerarios: 'Itinerarios', espacio: 'Espacio', detalle: 'Detalle', subtitulo: 'Subtítulo', desc: 'Descripción',
  info: 'Información', destacado: 'Destacado', destacada: 'Destacada',
};
const humanize = (k) => HUMAN[k] ?? (k.charAt(0).toUpperCase() + k.slice(1).replace(/([A-Z])/g, ' $1').replace(/_/g, ' ').toLowerCase()).replace(/^./, (c) => c.toUpperCase());
const humanizeVar = (k) => {
  const t = { consejosPracticos: 'Consejos prácticos', datosVisita: 'Datos de la visita', datosBarrio: 'Datos del barrio', datosUtiles: 'Datos útiles', datosComparativos: 'Comparativa',
    comoLlegar: 'Cómo llegar', faqs: 'Preguntas frecuentes', claves: 'Las claves', secretos: 'Los secretos', reglas: 'Las reglas', restaurantes: 'Dónde comer',
    horchaterias: 'Horchaterías', packs: 'Packs', paradas: 'Paradas', cronologia: 'Cronología', zonas: 'Zonas', consejos: 'Consejos', principios: 'Principios',
    pilares: 'Pilares', entradas: 'Entradas', tarifasResumen: 'Tarifas', mercadillosSemana: 'Mercadillos de la semana', artesanias: 'Artesanía', fechas: 'Fechas clave',
    eventosAno: 'Calendario del año', otrasOpciones: 'Otras opciones', exposicionesEspeciales: 'Exposiciones especiales', planesAireLibre: 'Planes al aire libre',
    masOperadores: 'Más operadores', toursTematicos: 'Tours temáticos', toursIglesias: 'Tours de iglesias', cienciaEscena: 'Ciencia en escena', simulador: 'Simulador',
    experiencias: 'Experiencias', actividades: 'Actividades', habitats: 'Hábitats', patrimoniosComunitat: 'En la Comunitat Valenciana', festivos2026: 'Festivos 2026',
    dias: 'Día a día', enLaCiudadEstaticas: 'En la ciudad', fueraCiudadEstaticas: 'Fuera de la ciudad', excursionesEstaticas: 'Excursiones', deportesAcuaticosEstaticos: 'Deportes acuáticos',
    paseosCatamaranEstaticos: 'Paseos en catamarán', venues: 'Dónde ir', parquesNaturales: 'Parques naturales', espaciosVerdes: 'Espacios verdes', alquilerBicis: 'Alquiler de bicis',
    toursGuiados: 'Tours guiados', toursEsenciales: 'Tours esenciales', visitasOficiales: 'Visitas oficiales', patrimoniosValencia: 'Patrimonio en Valencia', torres: 'Las torres',
    habitats: 'Hábitats', recintos: 'Recintos', actividades: 'Actividades', medios: 'Medios de transporte', mercados: 'Mercados', iniciativas: 'Iniciativas', espacios: 'Espacios' };
  return t[k] ?? k.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase());
};

const KNOWN = new Set(['dias','enLaCiudadEstaticas','fueraCiudadEstaticas','excursionesEstaticas','deportesAcuaticosEstaticos','paseosCatamaranEstaticos','venues','parquesNaturales','espaciosVerdes','alquilerBicis','toursGuiados','toursEsenciales','visitasOficiales','patrimoniosValencia','torres','consejosPracticos','datosVisita','datosBarrio','datosUtiles','datosComparativos','comoLlegar','faqs','claves','secretos','reglas','restaurantes','horchaterias','packs','paradas','cronologia','zonas','consejos','principios','pilares','entradas','tarifasResumen','mercadillosSemana','artesanias','fechas','eventosAno','otrasOpciones','exposicionesEspeciales','planesAireLibre','masOperadores','toursTematicos','toursIglesias','cienciaEscena','simulador','experiencias','actividades','habitats','patrimoniosComunitat','festivos2026']);
const TITLE_KEYS = ['nombre', 'titulo', 'titular', 'name', 'clave', 'regla', 'medio', 'q', 'label', 'dia', 'zona', 'mes', 'rango', 'tipo', 'area', 'region'];
const KICKER_KEYS = ['tipo', 'region', 'categoria', 'area', 'ubicacion'];
const BODY_KEYS = ['desc', 'a', 'detalle'];
const SKIP_KEYS = new Set(['imgClass', 'tipoClass', 'color', 'icono', 'emoji', 'id', 'url', 'tags', 'num', 'mesNum', 'destacada', 'destacado']);

function scalarFacts(o, used) {
  const facts = [], notes = [];
  for (const [k, v] of Object.entries(o)) {
    if (used.has(k) || SKIP_KEYS.has(k)) continue;
    if (typeof v === 'string' || typeof v === 'number') {
      const s = String(v).trim(); if (!s) continue;
      if (s.length > 130) notes.push([humanize(k), s]); else facts.push([humanize(k), s]);
    } else if (typeof v === 'boolean') {
      if (v) facts.push([humanize(k), 'Sí']);
    }
  }
  return { facts, notes };
}

function normalizeTags(tags) {
  if (!Array.isArray(tags)) return [];
  return tags.map((t) => typeof t === 'string' ? { l: t } : { l: t.label ?? t.nombre ?? '', free: t.type === 'free' || undefined }).filter((t) => t.l);
}

function normalizeItem(o, img, filterCat) {
  const used = new Set();
  const it = {};
  if (typeof o === 'string') { return { title: o }; }
  if (o.num != null) it.num = String(o.num);
  let tk = TITLE_KEYS.find((k) => o[k] != null && typeof o[k] !== 'object');
  if (tk) { it.title = String(o[tk]); used.add(tk); }
  const kk = KICKER_KEYS.find((k) => k !== tk && typeof o[k] === 'string' && o[k].length <= 70);
  if (kk) { it.kicker = o[kk]; used.add(kk); }
  if (typeof o.subtitulo === 'string') { it.subtitle = o.subtitulo; used.add('subtitulo'); }
  const bk = BODY_KEYS.find((k) => k !== tk && typeof o[k] === 'string');
  if (bk) { it.body = o[bk]; used.add(bk); }
  // datos [{d,v}] y similares
  for (const [k, v] of Object.entries(o)) {
    if (!Array.isArray(v) || k === 'tags') continue;
    used.add(k);
    if (v.length && typeof v[0] === 'object') {
      const rows = v.map((r) => {
        const vals = Object.values(r).filter((x) => typeof x === 'string' || typeof x === 'number');
        if ('d' in r && 'v' in r) return [r.d, r.v];
        if ('label' in r && 'val' in r) return [r.label, r.val];
        if ('medio' in r && 'detalle' in r) return [r.medio, r.detalle];
        if (vals.length >= 2) return [String(vals[0]), vals.slice(1).join(' · ')];
        return [String(vals[0] ?? ''), ''];
      });
      (it.facts ??= []).push(...rows);
    } else if (v.length) {
      (it.bullets ??= []).push(...v.map(String));
      if (it.bullets.length && k) it.bulletsTitle ??= humanize(k);
    }
  }
  const { facts, notes } = scalarFacts(o, used);
  if (facts.length) (it.facts ??= []).push(...facts);
  if (notes.length) it.notes = notes;
  const tags = normalizeTags(o.tags); if (tags.length) it.tags = tags;
  if (o.destacada || o.destacado === true) (it.tags ??= []).unshift({ l: 'Destacada' });
  if (img && o.imgClass && img[o.imgClass]) it.image = img[o.imgClass];
  if (typeof o.url === 'string' && /^https?:/.test(o.url)) it.url = o.url;
  if (typeof o.web === 'string' && /^https?:/.test(o.web)) it.url ??= o.web;
  if (filterCat && o[filterCat] != null) it.cat = String(o[filterCat]);
  return it;
}

function findHeadingBefore(jsx, idx) {
  const start = Math.max(0, idx - 1800);
  const chunk = jsx.slice(start, idx);
  const re = /<(h[1-4])[^>]*>([^<{]{3,120})<\/\1>|<(?:div|span|p)\s+className="[^"]*(?:section-title|sec-title|titulo-seccion|heading|title)[^"]*"[^>]*>([^<{]{3,120})</gi;
  let last = null, m;
  while ((m = re.exec(chunk))) last = clean(m[2] ?? m[3]);
  return last;
}

// ---------- extracción de texto fijo ----------
function staticTexts(jsx) {
  // h1, eyebrow y párrafos fijos (sin {expr}) antes del primer .map
  let body = jsx.slice(jsx.indexOf('export default'));
  const ret = body.indexOf('return (');
  if (ret > 0) body = body.slice(ret);
  const firstMap = body.search(/\.map\(/);
  const head = firstMap > 0 ? body.slice(0, firstMap) : body;
  const h1 = /<h1[^>]*>([\s\S]*?)<\/h1>/.exec(head);
  const eyebrow = /<(?:div|span|p)\s+className="[^"]*eyebrow[^"]*"[^>]*>([^<{]+)</i.exec(head);
  const paras = [];
  const re = /<p[^>]*>([\s\S]*?)<\/p>/g; let m;
  while ((m = re.exec(head))) {
    const t = clean(m[1]);
    if (t.length > 25 && !t.includes('{')) paras.push(t);
  }
  return { title: h1 ? clean(h1[1]) : null, eyebrow: eyebrow ? clean(eyebrow[1]) : null, lead: paras.slice(0, 3) };
}

function staticLists(jsx) {
  const out = [];
  const body = jsx.slice(jsx.indexOf('export default'));
  const re = /<h3[^>]*>([^<{]{3,120})<\/h3>\s*<ul[^>]*>([\s\S]*?)<\/ul>/g; let m;
  while ((m = re.exec(body))) {
    const items = [...m[2].matchAll(/<li[^>]*>([\s\S]*?)<\/li>/g)].map((x) => clean(x[1])).filter((t) => t && !t.includes('{'));
    if (items.length) out.push({ pos: m.index + jsx.indexOf('export default'), title: clean(m[1]), items });
  }
  return out;
}

function legalSections(jsx) {
  const body = jsx.slice(jsx.indexOf('export default'));
  const sections = []; let cur = null;
  const re = /<(h1|h2|h3|p|li)[^>]*>([\s\S]*?)<\/\1>/g; let m;
  while ((m = re.exec(body))) {
    const tag = m[1], t = clean(m[2]);
    if (!t || t.includes('{')) continue;
    if (tag === 'h1') continue;
    if (tag === 'h2' || tag === 'h3') { cur = { title: t, paras: [], bullets: [] }; sections.push(cur); }
    else { if (!cur) { cur = { title: '', paras: [], bullets: [] }; sections.push(cur); } (tag === 'li' ? cur.bullets : cur.paras).push(t); }
  }
  return sections.map((s) => ({ title: s.title, body: s.paras.join('\n\n'), bullets: s.bullets.length ? s.bullets : undefined }));
}

// ---------- familias ----------
const FAMILY = (f) => {
  if (f.includes('Que_Ver')) return 'quever';
  if (f.includes('Eventos')) return 'eventos';
  if (f.includes('Gastronomia')) return 'gastronomia';
  if (f.includes('Alojamientos')) return 'alojamientos';
  if (f.includes('Entradas/pages/Tours')) return 'tours';
  if (f.includes('Entradas/pages/Experiencias')) return 'experiencias';
  if (f.includes('Entradas')) return 'entradas';
  if (f.includes('esencial')) return 'esencial';
  if (f.includes('planes')) return 'planes';
  if (f.includes('sostenible')) return 'sostenible';
  if (f.includes('legal')) return 'legal';
  return 'otros';
};

const UI_VARS = new Set(['filtros', 'categorias', 'categoriasFiltro', 'tipoBadge', 'mercadosCat']);
// Cada museo es un objeto {nombre, nota, gratuito}: una viñeta «Nombre · nota · Gratuito» (antes salía «[object Object]»).
const museoLine = (m) => typeof m === 'object' ? [m.nombre, m.nota, m.gratuito ? 'Gratuito' : null].filter(Boolean).join(' · ') : String(m);
const museosFrom = (cats) => Array.isArray(cats) && cats.some((c) => Array.isArray(c.museos)) ? cats.filter((c) => Array.isArray(c.museos)).map((c) => ({ title: c.label, bullets: c.museos.map(museoLine) })) : null;

const pages = {};
const files = fs.readdirSync(DIR).sort();
for (const f of files) {
  const repoPath = f.replace(/__/g, '/');
  const id = path.posix.basename(repoPath, '.jsx');
  const jsx = fs.readFileSync(path.join(DIR, f), 'utf8');
  // CSS
  let img = {}, hero = null;
  for (const m of jsx.matchAll(/import\s+['"]([^'"]+\.css)['"]/g)) {
    const rel = path.posix.normalize(path.posix.join(path.posix.dirname(repoPath), m[1]));
    const css = await fetchCss(rel);
    const r = parseCssImages(css);
    Object.assign(img, r.map); hero ??= r.hero;
  }
  const st = staticTexts(jsx);
  const page = { id, family: FAMILY(repoPath), title: st.title ?? id, eyebrow: st.eyebrow ?? undefined, lead: st.lead, hero: hero ?? undefined, blocks: [] };

  if (page.family === 'legal') {
    page.lead = []; page.blocks.push({ type: 'items', title: '', items: legalSections(jsx) });
    pages[id] = page; continue;
  }

  // variables de datos
  const vars = {};
  const re = /^(?:var|const|let)\s+([A-Za-z_$][\w$]*)\s*=\s*([\[{])/gm; let m;
  while ((m = re.exec(jsx))) {
    const start = m.index + m[0].length - 1; const end = matchBalanced(jsx, start); if (end < 0) continue;
    try { vars[m[1]] = { v: vm.runInNewContext('(' + jsx.slice(start, end + 1) + ')', {}, { timeout: 1000 }), pos: jsx.indexOf(m[1] + '.map(', end) }; } catch { /* ignorar */ }
    re.lastIndex = end;
  }
  const cats = vars.categorias?.v ?? vars.categoriasFiltro?.v;
  const catLabels = Array.isArray(cats) && cats.length && typeof cats[0] === 'object' ? cats.filter((c) => !/^(todos?|all)$/i.test(c.id)).map((c) => ({ id: c.id, label: c.label })) : null;

  const blocks = [];
  for (const [name, { v, pos }] of Object.entries(vars)) {
    if (UI_VARS.has(name)) continue;
    const at = pos >= 0 ? pos : jsx.indexOf(name, jsx.indexOf('export default'));
    if (!Array.isArray(v)) {
      if (v && typeof v === 'object' && name === 'simulador') {
        const { facts, notes } = scalarFacts(v, new Set());
        blocks.push({ pos: at, type: 'items', title: humanizeVar(name), items: [{ title: v.titulo ?? v.nombre ?? humanizeVar(name), body: v.desc, facts, notes: notes.length ? notes : undefined }] });
      }
      continue;
    }
    if (!v.length) continue;
    const title = findHeadingBefore(jsx, at) ?? humanizeVar(name);
    if (typeof v[0] === 'string') {
      blocks.push({ pos: at, type: 'bullets', title: humanizeVar(name) === name ? title : humanizeVar(name), items: v.map(String) });
      continue;
    }
    // ¿lista de pares label/valor? (datosVisita, datosBarrio, datosUtiles…)
    const keys = Object.keys(v[0]);
    if ((keys.length === 2 && keys.includes('label') && keys.includes('val')) || (keys.length === 2 && keys.includes('q') && keys.includes('a') && false)) {
      blocks.push({ pos: at, type: 'facts', title: humanizeVar(name), rows: v.map((r) => [r.label, r.val]) });
      continue;
    }
    const filterCat = catLabels && v.some((x) => x.categoria != null) ? 'categoria' : null;
    const block = { pos: at, type: 'items', title: KNOWN.has(name) ? humanizeVar(name) : (findHeadingBefore(jsx, at) ?? humanizeVar(name)), items: v.map((o) => normalizeItem(o, img, filterCat)) };
    if (filterCat) block.filters = catLabels;
    blocks.push(block);
  }
  const museos = museosFrom(vars.categorias?.v);
  if (museos) blocks.push({ pos: jsx.indexOf('museos.map', jsx.indexOf('export default')), type: 'items', title: 'Museos', items: museos });
  for (const l of staticLists(jsx)) blocks.push({ pos: l.pos, type: 'bullets', title: l.title, items: l.items });
  blocks.sort((a, b) => a.pos - b.pos);
  blocks.forEach((b) => { delete b.pos; });
  // quitar duplicados de título genérico
  page.blocks = blocks;
  pages[id] = page;
}

fs.writeFileSync(path.join(T, 'web', 'pages.json'), JSON.stringify(pages, null, 1));
let nb = 0, ni = 0, noimg = 0;
for (const p of Object.values(pages)) for (const b of p.blocks) { nb++; for (const i of b.items ?? []) { ni++; if (!i.image) noimg++; } }
console.log('paginas', Object.keys(pages).length, 'bloques', nb, 'items', ni, 'sin imagen', noimg, 'tamaño', Math.round(fs.statSync(path.join(T, 'web', 'pages.json')).size / 1024) + 'KB');

// ---------- salida para la app ----------
const OUT = path.resolve('../../app/src/main/assets/web');
fs.rmSync(OUT, { recursive: true, force: true });
fs.mkdirSync(path.join(OUT, 'pages'), { recursive: true });

const index = { pages: {}, home: {} };
for (const [id, p] of Object.entries(pages)) {
  let thumb = null;
  for (const b of p.blocks) for (const it of b.items ?? []) { if (!thumb && it.image) thumb = it.image; }
  index.pages[id] = { family: p.family, title: p.title, eyebrow: p.eyebrow, hero: p.hero, thumb: thumb ?? p.hero };
  fs.writeFileSync(path.join(OUT, 'pages', id + '.json'), JSON.stringify(p));
}

// Rejillas de la portada (Esencial, Planes, Sostenible)
function gridOf(file) {
  const src = fs.readFileSync(path.join(T, 'web', file), 'utf8');
  const i = src.search(/var\s+elementos\s*=\s*\[/);
  const start = src.indexOf('[', i); const end = matchBalanced(src, start);
  const arr = vm.runInNewContext('(' + src.slice(start, end + 1) + ')', {});
  return arr.map((e) => ({ image: e.imagen.replace(/^\/?img\//, ''), title: e.titulo.trim(), route: e.ruta.replace(/^\//, '') }));
}
index.home.esencial = gridOf('src/Pagina_Inicio/components/ESENCIAL/GridEsencial.jsx');
index.home.planes = gridOf('src/Pagina_Inicio/components/PLANES/GridPlanes.jsx');
index.home.sostenible = gridOf('src/Pagina_Inicio/components/SOSTENIBLE/GridSostenible.jsx');

// Índices de las secciones (Qué ver, Eventos, Gastronomía, Alojamientos): título, texto y tarjetas, en el orden de la web
function categoryIndex(file) {
  const src = fs.readFileSync(path.join(T, 'web', file), 'utf8');
  const tiles = gridOf(file);
  const h = /<h2[^>]*>\s*([^<]+?)\s*<\/h2>/.exec(src);
  const p = /<p[^>]*className="txt2_\w+"[^>]*>\s*([^<]+?)\s*<\/p>/.exec(src);
  return { title: h ? h[1].trim() : '', subtitle: p ? p[1].trim() : '', tiles };
}
index.categories = {
  quever: categoryIndex('src/Que_Ver/components/ESPACIOS/GridEspacios.jsx'),
  eventos: categoryIndex('src/Eventos/components/DESTACADOS/GridDestacados.jsx'),
  gastronomia: categoryIndex('src/Gastronomia/components/CULINARIA/GridCulinaria.jsx'),
  alojamientos: categoryIndex('src/Alojamientos/components/ALOJAMIENTOS/GridEstancia.jsx'),
};
fs.writeFileSync(path.join(OUT, 'index.json'), JSON.stringify(index));

// Lista de imágenes necesarias (relativas a public/img)
const imgs = new Set();
const add = (x) => { if (x) imgs.add(x); };
for (const p of Object.values(index.pages)) { add(p.hero); add(p.thumb); }
for (const p of Object.values(pages)) for (const b of p.blocks) for (const it of b.items ?? []) add(it.image);
for (const g of Object.values(index.home)) for (const e of g) add(e.image);
for (const c of Object.values(index.categories)) for (const e of c.tiles) add(e.image);
fs.writeFileSync(path.join(T, 'web', 'images_needed.txt'), [...imgs].sort().join('\n'));
console.log('assets escritos; imagenes referenciadas:', imgs.size);
