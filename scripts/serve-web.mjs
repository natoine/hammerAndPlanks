// Serveur statique de la version web, avec les en-têtes d'isolation (COOP/COEP) qui autorisent
// ONNX Runtime Web à utiliser plusieurs threads (SharedArrayBuffer). Sans dépendance.
// Usage : node scripts/serve-web.mjs [dossier] [port]
import { createServer } from 'node:http';
import { createReadStream, statSync } from 'node:fs';
import { extname, join, normalize, resolve, sep } from 'node:path';

const root = resolve(process.argv[2] ?? 'platform/web/build/dist/webapp');
const port = Number(process.argv[3] ?? 8080);
const types = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript', '.mjs': 'text/javascript',
  '.wasm': 'application/wasm', '.json': 'application/json', '.png': 'image/png', '.jpg': 'image/jpeg',
  '.ttf': 'font/ttf', '.txt': 'text/plain; charset=utf-8', '.onnx': 'application/octet-stream',
  '.properties': 'text/plain; charset=utf-8', '.frag': 'text/plain', '.vert': 'text/plain',
};

/** Chemin du fichier demandé, ou null s'il sort du dossier servi. */
export function resolvePath(base, url) {
  const path = decodeURIComponent(new URL(url, 'http://localhost').pathname);
  const file = normalize(join(base, path.endsWith('/') ? path + 'index.html' : path));
  return file === base || file.startsWith(base + sep) ? file : null;
}

if (process.argv[1] && import.meta.url.endsWith(process.argv[1].split(sep).pop())) {
  createServer((req, res) => {
    const file = resolvePath(root, req.url ?? '/');
    let stats;
    try { stats = file && statSync(file); } catch { stats = null; }
    if (!stats || !stats.isFile()) { res.writeHead(404); res.end('Introuvable'); return; }
    res.writeHead(200, {
      'Content-Type': types[extname(file)] ?? 'application/octet-stream',
      'Content-Length': stats.size,
      'Cache-Control': 'no-cache',
      'Cross-Origin-Opener-Policy': 'same-origin',
      'Cross-Origin-Embedder-Policy': 'require-corp',
    });
    if (req.method === 'HEAD') { res.end(); return; }
    createReadStream(file).pipe(res);
  }).listen(port, '127.0.0.1', () => console.log(`Version web : http://localhost:${port} (${root})`));
}
