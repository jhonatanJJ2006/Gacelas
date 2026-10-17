import { notificacionesMock, rutasMock } from '../mocks/data'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import { AppRoutes } from '../App'
import { AJUSTES_POR_DEFECTO, type Ajustes, type CarreraGuardada, type Perfil } from '../data/tipos'
import { useApp } from '../store/app'
import { descartarCarrera } from '../store/carrera'

export const PERFIL_PRUEBA: Perfil = {
  email: 'jhona@utpl.edu.ec',
  nombre: 'Jhona Runner',
  username: 'jhona_runner',
  bio: '',
  ubicacion: 'Loja, Ecuador',
  edad: '',
  genero: 'Prefiero no decir',
  foto: null,
}

export function carreraPrueba(c: Partial<CarreraGuardada> = {}): CarreraGuardada {
  return {
    id: 'c1',
    titulo: 'Carrera de prueba',
    nota: '',
    publica: true,
    inicioMs: Date.now(),
    segundos: 1500,
    distanciaKm: 5,
    desnivelM: 20,
    parciales: [],
    puntos: [],
    ...c,
  }
}

/** Deja la app en un estado conocido: localStorage vacio y store listo. */
export function prepararApp(
  opciones: {
    sesion?: Perfil | null
    ajustes?: Partial<Ajustes>
    carreras?: CarreraGuardada[]
  } = {}
) {
  localStorage.clear()
  descartarCarrera()
  const sesion = opciones.sesion === undefined ? PERFIL_PRUEBA : opciones.sesion
  if (sesion)
    localStorage.setItem(
      'gacelas:auth',
      JSON.stringify({
        cuentas: { [sesion.email]: { perfil: sesion, hash: 'x' } },
        sesionEmail: sesion.email,
      })
    )
  if (opciones.carreras) localStorage.setItem('gacelas:carreras', JSON.stringify(opciones.carreras))
  useApp.setState({
    listo: true,
    sesion,
    ajustes: { ...AJUSTES_POR_DEFECTO, ...opciones.ajustes },
    carreras: opciones.carreras ?? [],
    rutasGuardadas: [],
    rutas: rutasMock,
    rutasMeGusta: [],
    rutasSociales: false,
    eventosCreados: [],
    notificaciones: notificacionesMock,
    inscritos: [],
  })
}

/** Renderiza la app completa en una ruta. */
export function renderRuta(ruta: string) {
  return render(
    <MemoryRouter initialEntries={[ruta]}>
      <AppRoutes />
    </MemoryRouter>
  )
}

type Exito = (p: GeolocationPosition) => void
type Fallo = (e: GeolocationPositionError) => void

/** GPS falso: permite emitir posiciones a mano desde la prueba. */
export function instalarGpsFalso() {
  const oyentes = new Map<number, { ok: Exito; mal?: Fallo }>()
  let siguiente = 1
  const geo = {
    watchPosition: vi.fn((ok: Exito, mal?: Fallo) => {
      const id = siguiente++
      oyentes.set(id, { ok, mal })
      return id
    }),
    clearWatch: vi.fn((id: number) => oyentes.delete(id)),
    getCurrentPosition: vi.fn((ok: Exito, mal: Fallo) => {
      void mal
      ok(posicion(-3.9931, -79.2042, Date.now(), 6))
    }),
  }
  Object.defineProperty(navigator, 'geolocation', { value: geo, configurable: true })
  return {
    geo,
    emitir(
      lat: number,
      lng: number,
      tiempoMs: number,
      precision = 5,
      altitud: number | null = null
    ) {
      oyentes.forEach((o) => o.ok(posicion(lat, lng, tiempoMs, precision, altitud)))
    },
    fallar(code: number) {
      const e = {
        code,
        message: 'x',
        PERMISSION_DENIED: 1,
        POSITION_UNAVAILABLE: 2,
        TIMEOUT: 3,
      } as GeolocationPositionError
      oyentes.forEach((o) => o.mal?.(e))
    },
    activos: () => oyentes.size,
  }
}

export function posicion(
  lat: number,
  lng: number,
  timestamp: number,
  accuracy = 5,
  altitude: number | null = null
): GeolocationPosition {
  return {
    timestamp,
    coords: {
      latitude: lat,
      longitude: lng,
      accuracy,
      altitude,
      altitudeAccuracy: null,
      heading: null,
      speed: null,
      toJSON: () => ({}),
    },
    toJSON: () => ({}),
  } as GeolocationPosition
}
