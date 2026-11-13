import { screen } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { normalizar } from '../app/BusquedaGlobal'
import { kmPorDiaSemana, proximosLogros, ritmoHistorico } from '../app/inicio/PanelesInicio'
import { navActiva } from '../app/ShellEscritorio'
import { horaIdeal, mejorVentana, type HoraClima } from '../contexts/clima/api'
import { leerRespuesta } from '../contexts/clima/infrastructure/climaOpenMeteo'
import { eventosMock, fechaEventoEjemplo } from '../mocks/data'
import { progresoNivel } from '../shared/kernel/nivel'
import { proyectar, simplificar } from '../shared/ui/Visual'
import { carreraPrueba, prepararApp, renderRuta } from '../test/utils'

// Viernes 13 nov 2026, 07:00 en Loja (12:00 UTC).
const VIERNES = Date.UTC(2026, 10, 13, 12, 0)
const hora = (h: number, temperatura: number, lluviaPct = 0): HoraClima => ({
  ms: Date.UTC(2026, 10, 13, h + 5, 0),
  temperatura,
  lluviaPct,
  codigoWmo: 3,
})

describe('clima para correr', () => {
  it('lee el pronostico por horas de Open-Meteo', () => {
    const l = leerRespuesta({
      current: {
        temperature_2m: 16.4,
        weather_code: 3,
        is_day: 1,
        apparent_temperature: 15.2,
        wind_speed_10m: 8,
        relative_humidity_2m: 72,
        uv_index: 3,
      },
      hourly: {
        time: [1794571200, 1794574800],
        temperature_2m: [13.2, 15.6],
        precipitation_probability: [10, 60],
        weather_code: [3, 61],
      },
    })
    expect(l.sensacion).toBe(15.2)
    expect(l.humedad).toBe(72)
    expect(l.horas).toEqual([
      { ms: 1794571200000, temperatura: 13, lluviaPct: 10, codigoWmo: 3 },
      { ms: 1794574800000, temperatura: 16, lluviaPct: 60, codigoWmo: 61 },
    ])
  })

  it('elige la ventana ideal mas larga del resto del dia', () => {
    const horas = [
      hora(7, 12),
      hora(8, 14),
      hora(9, 17),
      hora(10, 23),
      hora(11, 19),
      hora(14, 16, 60),
    ]
    expect(horaIdeal(horas[0]!)).toBe(true)
    expect(horaIdeal(horas[3]!)).toBe(false)
    expect(mejorVentana(horas, VIERNES)).toEqual({
      desde: 7,
      hasta: 10,
      lluvia: { hora: 14, pct: 60 },
    })
    expect(mejorVentana([hora(9, 25)], VIERNES)).toBeNull()
    expect(mejorVentana(undefined, VIERNES)).toBeNull()
  })
})

describe('eventos de ejemplo relativos', () => {
  it('los proximos siempre estan en el futuro y el pasado en el pasado', () => {
    const ahora = Date.now()
    for (const e of eventosMock) {
      if (e.pasado) expect(e.inicioMs!).toBeLessThan(ahora)
      else expect(e.inicioMs!).toBeGreaterThan(ahora)
    }
  })

  it('calcula el proximo dia de la semana en Loja', () => {
    const f = fechaEventoEjemplo(6, 6, 0, 0, VIERNES)
    expect(f.inicioMs).toBe(Date.UTC(2026, 10, 14, 11, 0))
    expect(f.fecha).toBe('Sábado 14 de Noviembre')
    expect(fechaEventoEjemplo(5, 6, 0, 0, VIERNES).fecha).toBe('Viernes 20 de Noviembre')
  })
})

describe('nivel y logros reales', () => {
  it('progreso al siguiente nivel con los umbrales de nivelCorredor', () => {
    expect(progresoNivel(0)).toEqual({
      nivel: 'Gacela Principiante',
      siguiente: 'Gacela Intermedia',
      faltanKm: 30,
      pct: 0,
    })
    expect(progresoNivel(90).pct).toBe(50)
    expect(progresoNivel(600)).toMatchObject({ nivel: 'Gacela Élite', siguiente: null, pct: 100 })
  })

  it('proximos logros salen de las carreras', () => {
    const logros = proximosLogros([carreraPrueba({ distanciaKm: 8 })])
    expect(logros.map((l) => l.id)).toEqual(['10k', '21k', 'racha7'])
    expect(logros[0]).toMatchObject({ pct: 80, texto: '8.0/10 km' })
  })

  it('km por dia y ritmo historico', () => {
    const lunes = new Date(2026, 10, 9, 7).getTime()
    const c = [
      carreraPrueba({ inicioMs: lunes, distanciaKm: 5, segundos: 1500 }),
      carreraPrueba({ id: 'c2', inicioMs: lunes + 2 * 86_400_000, distanciaKm: 3, segundos: 1080 }),
    ]
    expect(kmPorDiaSemana(c, lunes + 86_400_000)).toEqual([5, 0, 3, 0, 0, 0, 0])
    expect(ritmoHistorico(c)).toBe(323)
    expect(ritmoHistorico([])).toBeNull()
  })
})

describe('piezas del rediseno', () => {
  it('navegacion lateral: Comunidad no se enciende en sus subsecciones', () => {
    expect(navActiva('/comunidad', '/comunidad')).toBe(true)
    expect(navActiva('/comunidad/clubes/1', '/comunidad')).toBe(false)
    expect(navActiva('/comunidad/clubes/1', '/comunidad/clubes')).toBe(true)
    expect(navActiva('/comunidad/mapa', '/comunidad/en-vivo')).toBe(true)
  })

  it('busqueda sin tildes y mini mapa dentro del lienzo', () => {
    expect(normalizar('Jipiro Ñandú')).toBe('jipiro nandu')
    const pts = Array.from({ length: 200 }, (_, i) => ({
      lat: -4 + i / 1000,
      lng: -79.2 + i / 2000,
    }))
    expect(simplificar(pts, 50)).toHaveLength(50)
    for (const p of proyectar(pts, 120, 60)) {
      expect(p.x).toBeGreaterThanOrEqual(0)
      expect(p.x).toBeLessThanOrEqual(120)
      expect(p.y).toBeGreaterThanOrEqual(0)
      expect(p.y).toBeLessThanOrEqual(60)
    }
  })

  it('Inicio muestra una sola meta semanal en km', async () => {
    prepararApp({ ajustes: { metaKmSemana: 30 }, carreras: [carreraPrueba({ distanciaKm: 13 })] })
    renderRuta('/inicio')
    expect(await screen.findByRole('heading', { name: /te faltan 17 km/i })).toBeInTheDocument()
    expect(screen.getByTestId('semana-atletica')).toHaveTextContent('13.0 / 30 km')
  })

  it('Ctrl+K abre la busqueda global y Enter abre la ruta', async () => {
    prepararApp()
    renderRuta('/inicio')
    await screen.findByTestId('semana-atletica')
    await userEvent.keyboard('{Control>}k{/Control}')
    const campo = await screen.findByRole('combobox', { name: 'Buscar' })
    await userEvent.type(campo, 'jipiro')
    expect(screen.getAllByRole('option').some((o) => /jipiro/i.test(o.textContent ?? ''))).toBe(
      true
    )
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('combobox', { name: 'Buscar' })).not.toBeInTheDocument()
  })

  it('Perfil: el periodo filtra las cifras y Actividades pagina de 10 en 10', async () => {
    const dia = 86_400_000
    const carreras = Array.from({ length: 12 }, (_, i) =>
      carreraPrueba({
        id: `c${i}`,
        titulo: `Carrera ${i}`,
        distanciaKm: 2,
        inicioMs: Date.now() - (i < 2 ? i : 120 + i) * dia,
      })
    )
    prepararApp({ carreras })
    renderRuta('/perfil')
    const cifras = await screen.findByTestId('estadisticas-historicas')
    expect(cifras).toHaveTextContent('2 carreras, últimas 12 semanas')
    await userEvent.click(screen.getByRole('button', { name: 'Todo' }))
    expect(cifras).toHaveTextContent('12 carreras, desde el inicio')
    expect(screen.getAllByTestId('carrera-guardada')).toHaveLength(5)
    await userEvent.click(screen.getByRole('tab', { name: /Actividades/ }))
    expect(screen.getAllByTestId('carrera-guardada')).toHaveLength(10)
    expect(screen.getByText('Página 1 de 2')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(screen.getAllByTestId('carrera-guardada')).toHaveLength(2)
  })
})
