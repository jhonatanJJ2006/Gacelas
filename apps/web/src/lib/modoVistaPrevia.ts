import type { CarreraGuardada, Perfil } from '../data/tipos'
import type { PublicacionRemota } from '../data/repositorios'
import { rutasMock } from '../mocks/data'
import { conOficiales } from './rutas'
import { RUTAS_LOJA } from '../contexts/rutas/api'
import { useApp } from '../store/app'
import { tracker } from '../features/carreras/application/carreraStore'

/*
 * Modo de previsualizacion solo para desarrollo (pnpm dev, URL con ?vista-previa).
 * Abre una sesion de ejemplo en memoria para revisar y capturar las pantallas con sesion.
 * No escribe en Supabase ni en localStorage, y no se incluye en el build de produccion.
 */
const PERFIL_EJEMPLO: Perfil = {
  email: 'andrea.montano@ejemplo.com',
  nombre: 'Andrea Montaño',
  username: 'andrea_corre',
  bio: 'Fondos de sábado en Jipiro.',
  ubicacion: 'Loja, Ecuador',
  edad: '',
  genero: 'Prefiero no decir',
  foto: null,
}

function carrerasEjemplo(): CarreraGuardada[] {
  const dia = 86_400_000
  const ahora = Date.now()
  const titulos = [
    'Trote suave',
    'Fondo por Jipiro',
    'Series cortas',
    'Fondo largo',
    'Recuperación',
  ]
  // Las primeras cinco son las de siempre; luego un historial de varios meses para los reportes,
  // varias empezando en rutas oficiales de Loja para que aparezcan como rutas favoritas.
  const favoritas = [0, 1, 0, 10, 0, 1]
  return Array.from({ length: 64 }, (_, i) => {
    const km = i < 5 ? [5.2, 7.8, 4.1, 10.3, 6.4][i] : 4 + ((i * 37) % 90) / 10
    const ruta = i % 2 === 0 ? RUTAS_LOJA[favoritas[(i / 2) % favoritas.length]] : undefined
    const inicioMs = ahora - (i < 5 ? i * 2 + 1 : 10 + (i - 5) * 4.2) * dia
    return {
      id: `vp-${i}`,
      titulo: titulos[i % titulos.length],
      nota: '',
      publica: true,
      inicioMs: Math.round(inicioMs),
      segundos: Math.round(km * (320 + ((i * 13) % 50))),
      distanciaKm: km,
      desnivelM: 20 + ((i * 15) % 140),
      parciales: [],
      puntos: (ruta?.puntos ?? []).slice(0, 2).map((p, j) => ({
        ...p,
        tiempoMs: inicioMs + j * 60_000,
        precisionM: 5,
        altitudM: null,
      })),
    }
  })
}

/** Publicacion de otra corredora para ver el menu "Reportar publicacion". */
function publicacionAjena(): PublicacionRemota {
  return {
    id: 'vp-ajena-1',
    autorId: 'vp-lucia',
    autor: 'Lucía Paredes',
    titulo: 'Subida al Villonaco',
    inicioMs: Date.now() - 3 * 3_600_000,
    distanciaKm: 8.4,
    segundos: 3020,
    desnivelM: 310,
    likes: 12,
    comentarios: 3,
    puntos: [],
    propia: false,
  }
}

/** Feed de ejemplo con fotos y comentarios; se carga solo en este modo (import dinamico). */
let feedDemo: PublicacionRemota[] = []
const publicacionesVista = () => [publicacionAjena(), ...feedDemo]

async function cargarFeedDemo() {
  const demo = await import('../mocks/feed-demo/feedDemo')
  feedDemo = demo.publicacionesDemo()
  const original = useApp.getState().comentariosPost
  useApp.setState({
    publicaciones: publicacionesVista(),
    comentariosPost: async (id) => demo.comentariosDemo(id) ?? original(id),
  })
}

/** Carrera simulada para ver En curso sin GPS: unos 2 km por Loja. */
function simularCarrera() {
  tracker.iniciar()
  const t0 = Date.now() - 12 * 60_000
  for (let k = 0; k < 120; k++)
    tracker.registrarPunto({
      lat: -3.9931 + k * 0.00012,
      lng: -79.2042 + Math.sin(k / 12) * 0.0006,
      tiempoMs: t0 + k * 6000,
      precisionM: 5,
      altitudM: 2060 + Math.round(Math.sin(k / 20) * 12),
    })
}

export function activarVistaPreviaSiCorresponde() {
  if (!import.meta.env.DEV) return
  if (!new URLSearchParams(window.location.search).has('vista-previa')) return
  const aplicar = () =>
    useApp.setState({
      sesion: PERFIL_EJEMPLO,
      carreras: carrerasEjemplo(),
      rutas: conOficiales(useApp.getState().rutas.length ? useApp.getState().rutas : rutasMock),
      publicaciones: publicacionesVista(),
    })
  // cargar() y el listener de sesion pueden dejarla en null: en este modo se vuelve a abrir siempre.
  aplicar()
  void cargarFeedDemo()
  if (window.location.pathname.startsWith('/correr/en-curso')) simularCarrera()
  useApp.subscribe((s) => {
    if (!s.sesion) aplicar()
    else if (!s.publicaciones.some((p) => p.id === 'vp-ajena-1'))
      useApp.setState({ publicaciones: [...s.publicaciones, ...publicacionesVista()] })
  })
  // Si la sesion se perdio un instante y el enrutador redirigio, se vuelve a la ruta pedida.
  const destino = window.location.pathname + window.location.search
  const inicio = Date.now()
  const restaurar = window.setInterval(() => {
    if (Date.now() - inicio > 4000) return window.clearInterval(restaurar)
    if (window.location.pathname + window.location.search !== destino && useApp.getState().sesion) {
      window.history.replaceState(null, '', destino)
      window.dispatchEvent(new PopStateEvent('popstate'))
    }
  }, 200)
}
