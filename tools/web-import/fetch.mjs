// Descarga del repositorio de la web (cemarvar/TurismoValencia) las páginas JSX, su CSS y las rejillas de la portada
// a ./work/web, que es lo que lee convert.mjs. Solo lectura sobre GitHub; no escribe nada fuera de ./work.
import fs from 'fs';
import path from 'path';

const REPO = 'cemarvar/TurismoValencia';
const RAW = `https://raw.githubusercontent.com/${REPO}/main/`;
const WORK = process.env.WEB_IMPORT_WORK ?? path.resolve('work');

const tree = await (await fetch(`https://api.github.com/repos/${REPO}/git/trees/main?recursive=1`)).json();
if (!tree.tree) throw new Error('No se pudo leer el árbol del repositorio: ' + JSON.stringify(tree).slice(0, 200));

const get = async (repoPath) => {
  const res = await fetch(RAW + repoPath.split('/').map(encodeURIComponent).join('/'));
  if (!res.ok) throw new Error(repoPath + ' → ' + res.status);
  return res.text();
};

fs.mkdirSync(path.join(WORK, 'web', 'pages'), { recursive: true });
const pages = tree.tree.filter((t) => t.type === 'blob' && t.path.endsWith('.jsx') && t.path.includes('/pages/')).map((t) => t.path);
for (const p of pages) fs.writeFileSync(path.join(WORK, 'web', 'pages', p.replace(/\//g, '__')), await get(p));

const grids = [
  'src/Pagina_Inicio/components/App.jsx', // rutas de la web (las usa tools/verify)
  'src/Pagina_Inicio/components/ESENCIAL/GridEsencial.jsx',
  'src/Pagina_Inicio/components/PLANES/GridPlanes.jsx',
  'src/Pagina_Inicio/components/SOSTENIBLE/GridSostenible.jsx',
  'src/Que_Ver/components/ESPACIOS/GridEspacios.jsx',
  'src/Eventos/components/DESTACADOS/GridDestacados.jsx',
  'src/Gastronomia/components/CULINARIA/GridCulinaria.jsx',
  'src/Alojamientos/components/ALOJAMIENTOS/GridEstancia.jsx',
];
for (const g of grids) {
  const out = path.join(WORK, 'web', g);
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, await get(g));
}
console.log('páginas:', pages.length, '· rejillas:', grids.length, '· destino:', WORK);
