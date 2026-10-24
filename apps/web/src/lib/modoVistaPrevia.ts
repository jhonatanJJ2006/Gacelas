import type { CarreraGuardada, Perfil } from '../data/tipos'
import { rutasMock } from '../mocks/data'
import { useApp } from '../store/app'

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
  return [5.2, 7.8, 4.1, 10.3, 6.4].map((km, i) => ({
    id: `vp-${i}`,
    titulo: ['Trote suave', 'Fondo por Jipiro', 'Series cortas', 'Fondo largo', 'Recuperación'][i],
    nota: '',
    publica: true,
    inicioMs: ahora - (i * 2 + 1) * dia,
    segundos: Math.round(km * 340),
    distanciaKm: km,
    desnivelM: 20 + i * 15,
    parciales: [],
    puntos: [],
  }))
}

export function activarVistaPreviaSiCorresponde() {
  if (!import.meta.env.DEV) return
  if (!new URLSearchParams(window.location.search).has('vista-previa')) return
  const aplicar = () =>
    useApp.setState({
      sesion: PERFIL_EJEMPLO,
      carreras: carrerasEjemplo(),
      rutas: useApp.getState().rutas.length ? useApp.getState().rutas : rutasMock,
    })
  // cargar() y el listener de sesion pueden dejarla en null: en este modo se vuelve a abrir siempre.
  aplicar()
  useApp.subscribe((s) => {
    if (!s.sesion) aplicar()
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
