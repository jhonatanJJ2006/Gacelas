import { act, screen } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useIdentidad } from '../contexts/identidad/application/estado'
import { AJUSTES_POR_DEFECTO } from '../shared/kernel/tipos'
import { ESTILO_GACELAS, ESTILO_GACELAS_CLARO, estiloMapa } from '../shared/lib/mapaEstilo'
import {
  aplicarTema,
  esModoTema,
  normalizarTema,
  resolverTema,
  suscribirTema,
  temaActual,
} from '../shared/lib/tema'
import { prepararApp, renderRuta } from '../test/utils'

describe('tema: reglas', () => {
  it('por defecto es oscuro y acepta solo los tres valores', () => {
    expect(AJUSTES_POR_DEFECTO.tema).toBe('oscuro')
    expect(['claro', 'oscuro', 'sistema'].every(esModoTema)).toBe(true)
    expect(esModoTema('auto')).toBe(false)
    expect(normalizarTema('auto')).toBe('oscuro')
    expect(normalizarTema(undefined)).toBe('oscuro')
    expect(normalizarTema('sistema')).toBe('sistema')
  })

  it('sistema sigue la preferencia del sistema operativo', () => {
    expect(resolverTema('sistema', true)).toBe('oscuro')
    expect(resolverTema('sistema', false)).toBe('claro')
    expect(resolverTema('claro', true)).toBe('claro')
    expect(resolverTema('oscuro', false)).toBe('oscuro')
  })

  it('aplicarTema pone las clases, el color-scheme y avisa a los suscriptores', () => {
    const visto: string[] = []
    const baja = suscribirTema((t) => visto.push(t))
    aplicarTema('claro')
    const raiz = document.documentElement
    expect(raiz).toHaveClass('tema-claro')
    expect(raiz).not.toHaveClass('dark')
    expect(raiz.style.colorScheme).toBe('light')
    expect(raiz.dataset.tema).toBe('claro')
    expect(temaActual()).toBe('claro')
    aplicarTema('oscuro')
    expect(raiz).toHaveClass('dark')
    expect(raiz).not.toHaveClass('tema-claro')
    expect(raiz.style.colorScheme).toBe('dark')
    baja()
    expect(visto).toEqual(['claro', 'oscuro'])
  })

  it('el mapa tiene estilo claro y oscuro', () => {
    expect(estiloMapa('oscuro')).toBe(ESTILO_GACELAS)
    expect(estiloMapa('claro')).toBe(ESTILO_GACELAS_CLARO)
    type Estilo = { featureType?: string; elementType?: string; stylers?: { color?: string }[] }[]
    const tierra = (e: unknown) =>
      (e as Estilo).find((s) => !s.featureType && s.elementType === 'geometry')?.stylers?.[0]?.color
    expect(tierra(ESTILO_GACELAS_CLARO)).not.toBe(tierra(ESTILO_GACELAS))
  })
})

describe('tema: Ajustes', () => {
  beforeEach(() => prepararApp())
  afterEach(() => {
    vi.unstubAllGlobals()
    aplicarTema('oscuro')
  })

  it('el selector tiene tres opciones y marca la activa', async () => {
    renderRuta('/ajustes')
    const grupo = await screen.findByRole('radiogroup', { name: 'Tema' })
    expect(grupo).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: 'Oscuro' })).toBeChecked()
    expect(screen.getByRole('radio', { name: 'Claro' })).not.toBeChecked()
    expect(screen.getByRole('radio', { name: 'Sistema' })).not.toBeChecked()
  })

  it('elegir Claro y Oscuro cambia el documento y se guarda en local', async () => {
    renderRuta('/ajustes')
    await userEvent.click(await screen.findByRole('radio', { name: 'Claro' }))
    expect(document.documentElement).toHaveClass('tema-claro')
    expect(JSON.parse(localStorage.getItem('gacelas:ajustes')!).tema).toBe('claro')
    expect(screen.getByRole('radio', { name: 'Claro' })).toBeChecked()
    await userEvent.click(screen.getByRole('radio', { name: 'Oscuro' }))
    expect(document.documentElement).not.toHaveClass('tema-claro')
    expect(document.documentElement).toHaveClass('dark')
    expect(JSON.parse(localStorage.getItem('gacelas:ajustes')!).tema).toBe('oscuro')
  })

  it('Sistema usa la preferencia del sistema y reacciona a su cambio', async () => {
    let oyente: ((e: { matches: boolean }) => void) | undefined
    let oscuro = false
    vi.stubGlobal('matchMedia', (q: string) => ({
      get matches() {
        return q.includes('dark') ? oscuro : false
      },
      media: q,
      addEventListener: (_: string, f: (e: { matches: boolean }) => void) => (oyente = f),
      removeEventListener: () => (oyente = undefined),
    }))
    renderRuta('/ajustes')
    await userEvent.click(await screen.findByRole('radio', { name: 'Sistema' }))
    expect(useIdentidad.getState().ajustes.tema).toBe('sistema')
    expect(document.documentElement).toHaveClass('tema-claro')
    await act(async () => {
      oscuro = true
      oyente?.({ matches: true })
    })
    expect(document.documentElement).toHaveClass('dark')
    expect(document.documentElement).not.toHaveClass('tema-claro')
  })
})
