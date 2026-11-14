/**
 * Genera la marca "Piedra Clara": SVG, icono adaptativo, vector drawable
 * y la intro Lottie. No toca apps/android ni apps/web.
 *
 *   node design/alt-option/herramientas/construir-marca.mjs
 */
import fs from 'fs';
import { createRequire } from 'module';
const opentype = createRequire(import.meta.url)('/tmp/ot/node_modules/opentype.js');

const raiz = new URL('..', import.meta.url).pathname;
const TINTA = '#241C16';
const COBRE = '#C4623A';
const ARCILLA = '#A33C22';
const PAPEL = '#F3EBDF';
const SOL = '#F6D2A8';
const RIO = '#1E6460';
const OJO = '#F3EBDF';

const CUERPO = [
  'M46 76 C34 70 26 62 30 56 C36 60 40 70 48 76 Z',
  'M214 46 C220 40 208 32 186 33 C174 32 164 38 160 46 C154 56 146 64 126 66 C98 70 70 68 52 74 C44 76 40 82 46 86 C72 94 112 96 150 88 C160 84 168 76 174 68 C182 58 190 52 200 48 C206 46 210 46 214 46 Z',
  'M172 36 L162 18 L180 32 Z',
  'M52 84 C50 104 48 120 50 136 L62 136 C62 118 64 102 66 86 Z',
  'M76 88 C78 106 80 122 82 136 L70 136 C70 120 72 104 74 90 Z',
  'M124 86 C122 106 118 122 116 136 L128 136 C130 120 132 104 134 88 Z',
  'M148 82 C154 102 164 120 172 134 L184 130 C174 114 164 96 156 82 Z',
];
const CUERNO = 'M168 30 C156 10 138 2 120 12';
const OJO_D = 'M193.2 42 A2.8 2.8 0 1 0 198.8 42 A2.8 2.8 0 1 0 193.2 42 Z';
// Caja medida en Chrome (getBBox del grupo, incluido el cuerno).
const CAJA = { x: 28.96, y: 7.69, w: 186.58, h: 128.31 };
const CENTRO = { x: CAJA.x + CAJA.w / 2, y: CAJA.y + CAJA.h / 2 };

const fraunces = opentype.parse(fs.readFileSync(`${raiz}prototipo/fuentes/fraunces.ttf`));
const outfit = opentype.parse(fs.readFileSync(`${raiz}prototipo/fuentes/outfit.ttf`));

function dibujar(font, texto, size, variation, tracking = 0) {
  let x = 0;
  const partes = [];
  for (const ch of texto) {
    const g = font.charToGlyph(ch);
    const p = g.getPath(x, 0, size, { variation, kerning: false });
    partes.push(p.toPathData(2));
    x += (g.advanceWidth * size) / font.unitsPerEm + tracking;
  }
  return { d: partes.join(' '), w: x - tracking };
}

function gacelaGrupo(tinta, cobre, ojo) {
  const fills = CUERPO.map((d) => `<path d="${d}" fill="${tinta}"/>`).join('');
  return `${fills}<path d="${CUERNO}" fill="none" stroke="${cobre}" stroke-width="5.5" stroke-linecap="round"/><path d="${OJO_D}" fill="${ojo}"/>`;
}

function svg(vb, inner, w, h) {
  return `<?xml version="1.0" encoding="UTF-8"?>\n<svg xmlns="http://www.w3.org/2000/svg" viewBox="${vb}" width="${w}" height="${h}">${inner}</svg>\n`;
}

// --- Simbolo suelto (ojo del color del papel) ---
fs.writeFileSync(
  `${raiz}marca/gacela.svg`,
  svg('0 0 260 160', gacelaGrupo(TINTA, COBRE, OJO), 520, 320),
);

// --- Logo horizontal ---
const palabra = dibujar(fraunces, 'Runcity', 78, { opsz: 144, wght: 560, SOFT: 30, WONK: 0 });
const lugar = dibujar(outfit, 'LOJA', 15, { wght: 560 }, 4.4);
const escalaLogo = 132 / CAJA.h;
const logoAnchoAnimal = CAJA.w * escalaLogo;
const logoX = 8;
const logoY = 18;
const txL = logoX - CAJA.x * escalaLogo;
const tyL = logoY - CAJA.y * escalaLogo;
const textoX = logoX + logoAnchoAnimal + 28;
const basePalabra = 108;
const baseLugar = 146;
const logoW = Math.ceil(textoX + Math.max(palabra.w, lugar.w) + 8);
const logoH = 176;
const logoInner = `
<g transform="translate(${txL.toFixed(2)} ${tyL.toFixed(2)}) scale(${escalaLogo.toFixed(4)})">${gacelaGrupo(TINTA, COBRE, OJO)}</g>
<path d="${palabra.d}" transform="translate(${textoX.toFixed(2)} ${basePalabra})" fill="${TINTA}"/>
<path d="${lugar.d}" transform="translate(${textoX.toFixed(2)} ${baseLugar})" fill="${ARCILLA}"/>
<path d="M${textoX.toFixed(2)} 124 h36" stroke="${COBRE}" stroke-width="2" stroke-linecap="round"/>`;
fs.writeFileSync(`${raiz}marca/logo.svg`, svg(`0 0 ${logoW} ${logoH}`, logoInner, logoW * 2, logoH * 2));

// --- Logo vertical (sello + nombre), el de la intro en reposo ---
const palV = dibujar(fraunces, 'Runcity', 64, { opsz: 144, wght: 560, SOFT: 30, WONK: 0 });
const lugV = dibujar(outfit, 'LOJA', 14, { wght: 560 }, 5);
const vEscala = 168 / CAJA.h;
const vAnimalW = CAJA.w * vEscala;
const vW = 420;
const vH = 460;
const vTx = (vW - vAnimalW) / 2 - CAJA.x * vEscala;
const vTy = 28 - CAJA.y * vEscala;
const vPalX = (vW - palV.w) / 2;
const vLugX = (vW - lugV.w) / 2;
const vInner = `
<circle cx="210" cy="130" r="108" fill="${SOL}"/>
<g transform="translate(${vTx.toFixed(2)} ${vTy.toFixed(2)}) scale(${vEscala.toFixed(4)})">${gacelaGrupo(TINTA, COBRE, SOL)}</g>
<path d="${palV.d}" transform="translate(${vPalX.toFixed(2)} 292)" fill="${TINTA}"/>
<path d="M${((vW - 48) / 2).toFixed(2)} 314 h48" stroke="${COBRE}" stroke-width="2" stroke-linecap="round"/>
<path d="${lugV.d}" transform="translate(${vLugX.toFixed(2)} 344)" fill="${ARCILLA}"/>`;
fs.writeFileSync(`${raiz}marca/logo-vertical.svg`, svg(`0 0 ${vW} ${vH}`, vInner, vW, vH));

// --- Icono adaptativo 108 ---
const iEscala = 0.34;
const iTx = 54 - CENTRO.x * iEscala;
const iTy = 56 - CENTRO.y * iEscala;
const iconoFondo = `<rect width="108" height="108" fill="${PAPEL}"/><circle cx="54" cy="60" r="40" fill="${SOL}"/><path d="M22 80 Q54 92 86 80" fill="none" stroke="${RIO}" stroke-width="1.7" stroke-linecap="round" opacity="0.85"/>`;
const iconoFrente = `<g transform="translate(${iTx.toFixed(2)} ${iTy.toFixed(2)}) scale(${iEscala})">${gacelaGrupo(TINTA, COBRE, SOL)}</g>`;
fs.writeFileSync(`${raiz}marca/icono-fondo.svg`, svg('0 0 108 108', iconoFondo, 108, 108));
fs.writeFileSync(`${raiz}marca/icono-primer-plano.svg`, svg('0 0 108 108', iconoFrente, 108, 108));
fs.writeFileSync(`${raiz}marca/icono.svg`, svg('0 0 108 108', iconoFondo + iconoFrente, 512, 512));
fs.writeFileSync(
  `${raiz}marca/icono-monocromo.svg`,
  svg('0 0 108 108', `<g transform="translate(${iTx.toFixed(2)} ${iTy.toFixed(2)}) scale(${iEscala})">${gacelaGrupo('#000', '#000', '#fff')}</g>`, 108, 108),
);

// --- VectorDrawables (listos para copiar a un source set, no al main) ---
function pathXml(d, attrs) {
  return `<path android:${attrs} android:pathData="${d}"/>`;
}
const frenteXml = `<?xml version="1.0" encoding="utf-8"?>
<!-- Opcion Piedra Clara. No vive en src/main: no entra en el APK de debug ni release. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
<group android:translateX="${iTx.toFixed(2)}" android:translateY="${iTy.toFixed(2)}" android:scaleX="${iEscala}" android:scaleY="${iEscala}">
${CUERPO.map((d) => pathXml(d, `fillColor="${TINTA}"`)).join('\n')}
<path android:fillColor="${SOL}" android:pathData="${OJO_D}"/>
<path android:fillColor="#00000000" android:strokeColor="${COBRE}" android:strokeWidth="5.5" android:strokeLineCap="round" android:pathData="${CUERNO}"/>
</group>
</vector>
`;
const frenteMono = `<?xml version="1.0" encoding="utf-8"?>
<!-- Opcion Piedra Clara, una sola tinta. No vive en src/main. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
<group android:translateX="${iTx.toFixed(2)}" android:translateY="${iTy.toFixed(2)}" android:scaleX="${iEscala}" android:scaleY="${iEscala}">
${CUERPO.map((d, n) => pathXml(n === 1 ? `${d} ${OJO_D}` : d, `fillColor="#FF000000"${n === 1 ? ' android:fillType="evenOdd"' : ''}`)).join('\n')}
<path android:fillColor="#00000000" android:strokeColor="#FF000000" android:strokeWidth="5.5" android:strokeLineCap="round" android:pathData="${CUERNO}"/>
</group>
</vector>
`;
const fondoXml = `<?xml version="1.0" encoding="utf-8"?>
<!-- Opcion Piedra Clara. No vive en src/main. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
<path android:fillColor="${PAPEL}" android:pathData="M0 0 H108 V108 H0 Z"/>
<path android:fillColor="${SOL}" android:pathData="M14 60 A40 40 0 1 1 94 60 A40 40 0 1 1 14 60 Z"/>
<path android:fillColor="#00000000" android:strokeColor="${RIO}" android:strokeWidth="1.7" android:strokeLineCap="round" android:pathData="M22 80 Q54 92 86 80"/>
</vector>
`;
fs.mkdirSync(`${raiz}android/drawable`, { recursive: true });
fs.mkdirSync(`${raiz}android/mipmap-anydpi-v26`, { recursive: true });
fs.mkdirSync(`${raiz}android/animator`, { recursive: true });
fs.writeFileSync(`${raiz}android/drawable/ic_launcher_foreground.xml`, frenteXml);
fs.writeFileSync(`${raiz}android/drawable/ic_launcher_monochrome.xml`, frenteMono);
fs.writeFileSync(`${raiz}android/drawable/ic_launcher_background.xml`, fondoXml);
fs.writeFileSync(
  `${raiz}android/mipmap-anydpi-v26/ic_launcher.xml`,
  `<?xml version="1.0" encoding="utf-8"?>
<!-- Opcion Piedra Clara. Copiar a src/<flavor>/res solo si se quiere un flavor aparte. -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>
</adaptive-icon>
`,
);
fs.writeFileSync(
  `${raiz}android/mipmap-anydpi-v26/ic_launcher_round.xml`,
  fs.readFileSync(`${raiz}android/mipmap-anydpi-v26/ic_launcher.xml`),
);

// --- Intro: vector estatico (ultimo cuadro) + animated-vector ---
const sEscala = 200 / CAJA.w;
const sTx = 180 - CENTRO.x * sEscala;
const sTy = 248 - CENTRO.y * sEscala;
const palS = dibujar(fraunces, 'Runcity', 46, { opsz: 144, wght: 560, SOFT: 30, WONK: 0 });
const lugS = dibujar(outfit, 'LOJA', 13, { wght: 560 }, 4.6);
const sPalX = (360 - palS.w) / 2;
const sLugX = (360 - lugS.w) / 2;
const splash = `<?xml version="1.0" encoding="utf-8"?>
<!-- Ultimo cuadro de la intro "El sello". No vive en src/main. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="360dp" android:height="640dp" android:viewportWidth="360" android:viewportHeight="640">
<path android:fillColor="${PAPEL}" android:pathData="M0 0 H360 V640 H0 Z"/>
<path android:name="sol" android:fillColor="${SOL}" android:pathData="M80 250 A100 100 0 1 1 280 250 A100 100 0 1 1 80 250 Z"/>
<group android:name="sello" android:translateX="${sTx.toFixed(2)}" android:translateY="${sTy.toFixed(2)}" android:scaleX="${sEscala.toFixed(4)}" android:scaleY="${sEscala.toFixed(4)}" android:pivotX="${CENTRO.x.toFixed(2)}" android:pivotY="${CENTRO.y.toFixed(2)}">
${CUERPO.map((d) => pathXml(d, `fillColor="${TINTA}"`)).join('\n')}
<path android:fillColor="${SOL}" android:pathData="${OJO_D}"/>
<path android:name="cuerno" android:fillColor="#00000000" android:strokeColor="${COBRE}" android:strokeWidth="5.5" android:strokeLineCap="round" android:trimPathEnd="1" android:pathData="${CUERNO}"/>
</group>
<group android:name="rotulo" android:translateY="0">
<path android:fillColor="${TINTA}" android:pathData="${palS.d}" android:translateX="${sPalX.toFixed(2)}" android:translateY="430"/>
<path android:fillColor="${COBRE}" android:pathData="M156 448 h48" android:strokeColor="${COBRE}" android:strokeWidth="2" android:strokeLineCap="round"/>
<path android:fillColor="${ARCILLA}" android:pathData="${lugS.d}" android:translateX="${sLugX.toFixed(2)}" android:translateY="478"/>
</group>
</vector>
`;
// VectorDrawable no admite translateX en <path>. El rotulo va en un group.
const splashOk = `<?xml version="1.0" encoding="utf-8"?>
<!-- Ultimo cuadro de la intro "El sello". No vive en src/main. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="360dp" android:height="640dp" android:viewportWidth="360" android:viewportHeight="640">
<path android:fillColor="${PAPEL}" android:pathData="M0 0 H360 V640 H0 Z"/>
<path android:name="sol" android:fillColor="${SOL}" android:pathData="M80 250 A100 100 0 1 1 280 250 A100 100 0 1 1 80 250 Z"/>
<group android:name="sello" android:pivotX="${CENTRO.x.toFixed(2)}" android:pivotY="${CENTRO.y.toFixed(2)}" android:scaleX="${sEscala.toFixed(4)}" android:scaleY="${sEscala.toFixed(4)}" android:translateX="${sTx.toFixed(2)}" android:translateY="${sTy.toFixed(2)}">
${CUERPO.map((d) => pathXml(d, `fillColor="${TINTA}"`)).join('\n')}
<path android:fillColor="${SOL}" android:pathData="${OJO_D}"/>
<path android:name="cuerno" android:fillColor="#00000000" android:strokeColor="${COBRE}" android:strokeWidth="5.5" android:strokeLineCap="round" android:trimPathEnd="1" android:pathData="${CUERNO}"/>
</group>
<group android:name="rotulo">
<group android:translateX="${sPalX.toFixed(2)}" android:translateY="430">
<path android:fillColor="${TINTA}" android:pathData="${palS.d}"/>
</group>
<path android:fillColor="#00000000" android:strokeColor="${COBRE}" android:strokeWidth="2" android:strokeLineCap="round" android:pathData="M156 448 h48"/>
<group android:translateX="${sLugX.toFixed(2)}" android:translateY="478">
<path android:fillColor="${ARCILLA}" android:pathData="${lugS.d}"/>
</group>
</group>
</vector>
`;
fs.writeFileSync(`${raiz}android/drawable/splash_piedra.xml`, splashOk);
fs.writeFileSync(
  `${raiz}android/animator/sello.xml`,
  `<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <objectAnimator android:propertyName="scaleX" android:valueFrom="0.82" android:valueTo="${sEscala.toFixed(4)}" android:duration="680" android:startOffset="180" android:interpolator="@android:interpolator/overshoot"/>
    <objectAnimator android:propertyName="scaleY" android:valueFrom="0.82" android:valueTo="${sEscala.toFixed(4)}" android:duration="680" android:startOffset="180" android:interpolator="@android:interpolator/overshoot"/>
    <objectAnimator android:propertyName="rotation" android:valueFrom="-8" android:valueTo="0" android:duration="680" android:startOffset="180" android:interpolator="@android:interpolator/fast_out_slow_in"/>
</set>
`,
);
fs.writeFileSync(
  `${raiz}android/animator/cuerno.xml`,
  `<?xml version="1.0" encoding="utf-8"?>
<objectAnimator xmlns:android="http://schemas.android.com/apk/res/android" android:propertyName="trimPathEnd" android:valueFrom="0" android:valueTo="1" android:duration="520" android:startOffset="720" android:interpolator="@android:interpolator/fast_out_slow_in"/>
`,
);
fs.writeFileSync(
  `${raiz}android/animator/rotulo.xml`,
  `<?xml version="1.0" encoding="utf-8"?>
<objectAnimator xmlns:android="http://schemas.android.com/apk/res/android" android:propertyName="translateY" android:valueFrom="16" android:valueTo="0" android:duration="480" android:startOffset="1080" android:interpolator="@android:interpolator/fast_out_slow_in"/>
`,
);
fs.writeFileSync(
  `${raiz}android/drawable/splash_piedra_anim.xml`,
  `<?xml version="1.0" encoding="utf-8"?>
<!-- Intro "El sello", 2,4 s de movimiento y reposo. No vive en src/main. -->
<animated-vector xmlns:android="http://schemas.android.com/apk/res/android" android:drawable="@drawable/splash_piedra">
    <target android:name="sello" android:animation="@animator/sello"/>
    <target android:name="cuerno" android:animation="@animator/cuerno"/>
    <target android:name="rotulo" android:animation="@animator/rotulo"/>
</animated-vector>
`,
);

// --- Lottie ---
function parsePath(d) {
  const tokens = d.match(/[MLCQZmlcqz]|-?\d*\.?\d+(?:e[-+]?\d+)?/g);
  const contours = [];
  let i = 0;
  let cx = 0, cy = 0, sx = 0, sy = 0, cur = null;
  const num = () => parseFloat(tokens[i++]);
  while (i < tokens.length) {
    const t = tokens[i++];
    if (t === 'M' || t === 'm') {
      const x = num() + (t === 'm' ? cx : 0);
      const y = num() + (t === 'm' ? cy : 0);
      if (cur && cur.v.length) contours.push(finish(cur));
      cur = { v: [[x, y]], i: [[0, 0]], o: [[0, 0]], c: false };
      cx = sx = x; cy = sy = y;
    } else if (t === 'L' || t === 'l') {
      const x = num() + (t === 'l' ? cx : 0);
      const y = num() + (t === 'l' ? cy : 0);
      const last = cur.v.length - 1;
      cur.o[last] = [0, 0];
      cur.v.push([+x.toFixed(2), +y.toFixed(2)]);
      cur.i.push([0, 0]);
      cur.o.push([0, 0]);
      cx = x; cy = y;
    } else if (t === 'C' || t === 'c') {
      let x1 = num(), y1 = num(), x2 = num(), y2 = num(), x = num(), y = num();
      if (t === 'c') { x1 += cx; y1 += cy; x2 += cx; y2 += cy; x += cx; y += cy; }
      const last = cur.v.length - 1;
      cur.o[last] = [+(x1 - cur.v[last][0]).toFixed(2), +(y1 - cur.v[last][1]).toFixed(2)];
      cur.v.push([+x.toFixed(2), +y.toFixed(2)]);
      cur.i.push([+(x2 - x).toFixed(2), +(y2 - y).toFixed(2)]);
      cur.o.push([0, 0]);
      cx = x; cy = y;
    } else if (t === 'Q' || t === 'q') {
      let x1 = num(), y1 = num(), x = num(), y = num();
      if (t === 'q') { x1 += cx; y1 += cy; x += cx; y += cy; }
      const c1x = cx + (2 / 3) * (x1 - cx);
      const c1y = cy + (2 / 3) * (y1 - cy);
      const c2x = x + (2 / 3) * (x1 - x);
      const c2y = y + (2 / 3) * (y1 - y);
      const last = cur.v.length - 1;
      cur.o[last] = [+(c1x - cur.v[last][0]).toFixed(2), +(c1y - cur.v[last][1]).toFixed(2)];
      cur.v.push([+x.toFixed(2), +y.toFixed(2)]);
      cur.i.push([+(c2x - x).toFixed(2), +(c2y - y).toFixed(2)]);
      cur.o.push([0, 0]);
      cx = x; cy = y;
    } else if (t === 'Z' || t === 'z') {
      cur.c = true;
      cx = sx; cy = sy;
    } else throw new Error('trazo no soportado: ' + t);
  }
  if (cur && cur.v.length) contours.push(finish(cur));
  return contours;
}
function finish(c) {
  c.v = c.v.map(([x, y]) => [+x.toFixed(2), +y.toFixed(2)]);
  c.i = c.i.map(([x, y]) => [+x.toFixed(2), +y.toFixed(2)]);
  c.o = c.o.map(([x, y]) => [+x.toFixed(2), +y.toFixed(2)]);
  return { c: c.c, v: c.v, i: c.i, o: c.o };
}
function rgb(hex) {
  return [parseInt(hex.slice(1, 3), 16) / 255, parseInt(hex.slice(3, 5), 16) / 255, parseInt(hex.slice(5, 7), 16) / 255, 1];
}
const estatico = (k) => ({ a: 0, k });
function keys(pares) {
  const out = [];
  for (let n = 0; n < pares.length; n++) {
    const [t, s] = pares[n];
    const f = { t, s };
    if (n < pares.length - 1) {
      const dim = s.length;
      f.i = { x: Array(dim).fill(0.22), y: Array(dim).fill(1) };
      f.o = { x: Array(dim).fill(0.45), y: Array(dim).fill(0) };
    }
    out.push(f);
  }
  return { a: 1, k: out };
}
function trCapa(ks) {
  return {
    o: ks.o || estatico(100),
    r: ks.r || estatico(0),
    p: ks.p || estatico([0, 0, 0]),
    a: ks.a || estatico([0, 0, 0]),
    s: ks.s || estatico([100, 100, 100]),
  };
}
function trForma() {
  return { ty: 'tr', p: estatico([0, 0]), a: estatico([0, 0]), s: estatico([100, 100]), r: estatico(0), o: estatico(100), sk: estatico(0), sa: estatico(0), nm: 'tr' };
}
let ind = 1;
function capa(nm, ks, shapes, extra = {}) {
  return { ddd: 0, ind: ind++, ty: 4, nm, sr: 1, ks: trCapa(ks), ao: 0, shapes: [...shapes, trForma()], ip: 0, op: 90, st: 0, bm: 0, ...extra };
}
function formasRelleno(ds, color) {
  return [...ds.flatMap(parsePath).map((k, n) => ({ ty: 'sh', nm: 'c' + n, ks: estatico(k) })), { ty: 'fl', c: estatico(rgb(color)), o: estatico(100), r: 1, nm: 'relleno' }];
}
const hijo = {
  o: keys([[0, [0]], [8, [0]], [20, [100]]]),
  r: keys([[0, [-8]], [28, [0]]]),
  s: keys([[0, [258, 258, 100]], [18, [312, 312, 100]], [28, [300, 300, 100]]]),
  p: estatico([540, 760, 0]),
  a: estatico([CENTRO.x, CENTRO.y, 0]),
};
const capas = [];
capas.push(capa('papel', { p: estatico([540, 960, 0]), a: estatico([0, 0, 0]) }, [
  { ty: 'rc', p: estatico([0, 0]), s: estatico([1080, 1920]), r: estatico(0), nm: 'hoja' },
  { ty: 'fl', c: estatico(rgb(PAPEL)), o: estatico(100), nm: 'relleno' },
]));
capas.push(capa('sol', {
  p: estatico([540, 760, 0]),
  o: keys([[0, [0]], [16, [100]]]),
  s: keys([[0, [88, 88, 100]], [20, [100, 100, 100]]]),
}, [
  { ty: 'el', p: estatico([0, 0]), s: estatico([460, 460]), nm: 'disco' },
  { ty: 'fl', c: estatico(rgb(SOL)), o: estatico(100), nm: 'relleno' },
]));
capas.push(capa('gacela', hijo, formasRelleno(CUERPO, TINTA)));
const k = 2.8 * 0.5522847498;
const ojoCubico = `M193.2 42 C193.2 ${42 - k} ${196 - k} 39.2 196 39.2 C${196 + k} 39.2 198.8 ${42 - k} 198.8 42 C198.8 ${42 + k} ${196 + k} 44.8 196 44.8 C${196 - k} 44.8 193.2 ${42 + k} 193.2 42 Z`;
capas.push(capa('ojo', hijo, formasRelleno([ojoCubico], SOL)));
capas.push(capa('cuerno', hijo, [
  ...parsePath(CUERNO).map((k, n) => ({ ty: 'sh', nm: 'c' + n, ks: estatico({ ...k, c: false }) })),
  { ty: 'st', c: estatico(rgb(COBRE)), o: estatico(100), w: estatico(5.5), lc: 2, lj: 2, nm: 'trazo' },
  { ty: 'tm', s: estatico(0), e: keys([[16, [0]], [38, [100]]]), o: estatico(0), m: 1, nm: 'recorte' },
]));
const palL = dibujar(fraunces, 'Runcity', 92, { opsz: 144, wght: 560, SOFT: 30, WONK: 0 });
const lugL = dibujar(outfit, 'LOJA', 22, { wght: 560 }, 6.2);
capas.push(capa('nombre', {
  p: keys([[34, [(1080 - palL.w) / 2, 1046, 0]], [52, [(1080 - palL.w) / 2, 1024, 0]]]),
  o: keys([[34, [0]], [52, [100]]]),
  a: estatico([0, 0, 0]),
}, formasRelleno([palL.d], TINTA)));
capas.push(capa('regla', {
  p: estatico([540, 1068, 0]),
  s: keys([[46, [0, 100, 100]], [60, [100, 100, 100]]]),
  o: keys([[46, [0]], [58, [100]]]),
}, [
  { ty: 'rc', p: estatico([0, 0]), s: estatico([72, 3]), r: estatico(2), nm: 'linea' },
  { ty: 'fl', c: estatico(rgb(COBRE)), o: estatico(100), nm: 'relleno' },
]));
capas.push(capa('lugar', {
  p: keys([[52, [(1080 - lugL.w) / 2, 1112, 0]], [66, [(1080 - lugL.w) / 2, 1096, 0]]]),
  o: keys([[52, [0]], [66, [100]]]),
}, formasRelleno([lugL.d], ARCILLA)));

// Las claves de posicion llevan tangentes espaciales en cero para que el reproductor no falle.
for (const c of capas) {
  const p = c.ks && c.ks.p;
  if (p && p.a === 1) {
    for (let n = 0; n < p.k.length - 1; n++) {
      p.k[n].to = [0, 0, 0];
      p.k[n].ti = [0, 0, 0];
    }
  }
}
const orden = ['lugar', 'regla', 'nombre', 'cuerno', 'ojo', 'gacela', 'sol', 'papel'];
capas.sort((a, b) => orden.indexOf(a.nm) - orden.indexOf(b.nm));
const lottie = {
  v: '5.7.4', fr: 30, ip: 0, op: 90, w: 1080, h: 1920, nm: 'Runcity sello', ddd: 0, assets: [], layers: capas, fonts: { list: [] },
};
fs.mkdirSync(`${raiz}motion`, { recursive: true });
fs.writeFileSync(`${raiz}motion/intro-sello.json`, JSON.stringify(lottie));
fs.writeFileSync(`${raiz}herramientas/medidas.json`, JSON.stringify({
  logo: { w: logoW, h: logoH }, vertical: { w: vW, h: vH }, caja: CAJA, escalaIcono: iEscala, tx: iTx, ty: iTy,
}, null, 2));
console.log('marca escrita', { logoW, logoH, capas: capas.length });
