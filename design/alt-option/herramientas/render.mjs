/**
 * Captura las pantallas del prototipo y los cuadros de la intro Lottie.
 * Uso: node design/alt-option/herramientas/render.mjs
 */
import { spawn } from 'child_process';
import http from 'http';
import fs from 'fs';
import path from 'path';

const raiz = path.resolve(new URL('..', import.meta.url).pathname);
const previews = path.join(raiz, 'previews');
fs.mkdirSync(previews, { recursive: true });

const server = http.createServer((req, res) => {
  const url = decodeURIComponent(req.url.split('?')[0]);
  const file = path.join(raiz, url === '/' ? 'prototipo/index.html' : url.replace(/^\//, ''));
  if (!file.startsWith(raiz) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {
    res.writeHead(404); res.end('no'); return;
  }
  const tipos = { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript', '.json': 'application/json', '.svg': 'image/svg+xml', '.ttf': 'font/ttf', '.png': 'image/png' };
  res.writeHead(200, { 'Content-Type': tipos[path.extname(file)] || 'application/octet-stream' });
  fs.createReadStream(file).pipe(res);
});
await new Promise((r) => server.listen(8765, '127.0.0.1', r));

const chrome = spawn('google-chrome', [
  '--headless=new', '--disable-gpu', '--no-sandbox', '--hide-scrollbars',
  '--remote-debugging-port=9333', '--user-data-dir=/tmp/chrome-piedra-render',
  '--window-size=420,900', 'about:blank',
], { stdio: 'ignore' });

function getJson(url) {
  return new Promise((resolve, reject) => {
    http.get(url, (res) => {
      let d = '';
      res.on('data', (c) => { d += c; });
      res.on('end', () => resolve(JSON.parse(d)));
    }).on('error', reject);
  });
}
await new Promise((r) => setTimeout(r, 600));
const ver = await getJson('http://127.0.0.1:9333/json/version');
const ws = new WebSocket(ver.webSocketDebuggerUrl);
await new Promise((r) => { ws.addEventListener('open', r, { once: true }); });

let seq = 0;
const pending = new Map();
const esperas = [];
ws.addEventListener('message', (ev) => {
  const msg = JSON.parse(ev.data);
  if (msg.id && pending.has(msg.id)) {
    const { resolve, reject } = pending.get(msg.id);
    pending.delete(msg.id);
    if (msg.error) reject(new Error(JSON.stringify(msg.error)));
    else resolve(msg.result);
  }
  if (msg.method) {
    for (let i = esperas.length - 1; i >= 0; i--) {
      if (esperas[i].method === msg.method) {
        const espera = esperas.splice(i, 1)[0];
        espera.resolve(msg.params);
      }
    }
  }
});
function send(method, params = {}, sessionId) {
  const id = ++seq;
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject });
    ws.send(JSON.stringify({ id, method, params, sessionId }));
  });
}
function esperar(method) {
  return new Promise((resolve) => esperas.push({ method, resolve }));
}

const { targetId } = await send('Target.createTarget', { url: 'about:blank' });
const { sessionId } = await send('Target.attachToTarget', { targetId, flatten: true });
const ses = (method, params) => send(method, params, sessionId);
await ses('Page.enable');
await ses('Runtime.enable');

async function abrir(url, width, height, scale) {
  await ses('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: scale, mobile: true });
  const cargado = esperar('Page.loadEventFired');
  await ses('Page.navigate', { url });
  await cargado;
  await ses('Runtime.evaluate', { expression: 'document.fonts ? document.fonts.ready : 1', awaitPromise: true });
  await ses('Runtime.evaluate', {
    expression: `new Promise((r) => { const listo = () => document.body && document.body.dataset.ready === '1' ? r(1) : setTimeout(listo, 30); listo(); })`,
    awaitPromise: true,
  });
}
async function foto(archivo, omitBackground = false) {
  const { data } = await ses('Page.captureScreenshot', { format: 'png', omitBackground });
  fs.writeFileSync(archivo, Buffer.from(data, 'base64'));
}

const pantallas = ['inicio', 'correr', 'carrera', 'resumen', 'rutas', 'perfil'];
for (const id of pantallas) {
  await abrir(`http://127.0.0.1:8765/prototipo/index.html?v=${id}#${id}`, 390, 900, 2);
  const medida = await ses('Runtime.evaluate', {
    returnByValue: true,
    expression: `(() => {
      window.ir('${id}');
      const tel = document.getElementById('telefono');
      tel.style.height = 'auto';
      document.querySelectorAll('.pantalla').forEach((el) => {
        const activa = el.id === '${id}';
        if (!activa) {
          if (!el.dataset.guardado) el.dataset.guardado = '1';
          el.remove();
        } else {
          el.style.display = el.classList.contains('columna') ? 'flex' : 'block';
          el.style.height = 'auto';
          el.style.overflow = 'visible';
        }
      });
      return Math.ceil(tel.getBoundingClientRect().height);
    })()`,
  });
  const alto = medida.result.value;
  await ses('Emulation.setDeviceMetricsOverride', { width: 390, height: alto, deviceScaleFactor: 2, mobile: true });
  await new Promise((r) => setTimeout(r, 40));
  await foto(path.join(previews, `${id}.png`));
  console.log('pantalla', id, alto);
}

await abrir('http://127.0.0.1:8765/prototipo/intro.html', 720, 1280, 1);
const framesDir = '/tmp/intro-frames';
fs.mkdirSync(framesDir, { recursive: true });
const n = 90;
for (let f = 0; f < n; f++) {
  await ses('Runtime.evaluate', { expression: `window.irA && window.irA(${f})` });
  await new Promise((r) => setTimeout(r, 16));
  await foto(path.join(framesDir, `f${String(f).padStart(3, '0')}.png`));
  if (f % 15 === 0) console.log('cuadro', f);
}
const tira = [2, 8, 16, 26, 40, 55, 89];
for (const f of tira) {
  fs.copyFileSync(path.join(framesDir, `f${String(f).padStart(3, '0')}.png`), path.join(previews, `intro-t${String(f).padStart(2, '0')}.png`));
}

ws.close();
chrome.kill();
server.close();
console.log('listo');
