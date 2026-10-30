import { screen, within } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { PRIVACIDAD, TERMINOS } from '../contexts/legal/domain/textos'
import { prepararApp, renderRuta } from '../test/utils'

const DOCUMENTOS = [
  {
    ruta: '/legal/terminos',
    titulo: 'Términos y condiciones',
    testid: 'doc-terminos',
    secciones: TERMINOS,
    otro: { nombre: 'Política de privacidad', ruta: '/legal/privacidad' },
  },
  {
    ruta: '/legal/privacidad',
    titulo: 'Política de privacidad',
    testid: 'doc-privacidad',
    secciones: PRIVACIDAD,
    otro: { nombre: 'Términos y condiciones', ruta: '/legal/terminos' },
  },
] as const

describe.each(DOCUMENTOS)('pagina legal $ruta', (doc) => {
  beforeEach(() => prepararApp({ sesion: null }))

  it('es una pagina con su propia URL, legible sin sesion y sin modales', async () => {
    renderRuta(doc.ruta)
    expect(await screen.findByRole('heading', { level: 1, name: doc.titulo })).toBeInTheDocument()
    expect(screen.getByTestId(doc.testid)).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('muestra todas las secciones con su texto sin cambios', async () => {
    renderRuta(doc.ruta)
    await screen.findByTestId(doc.testid)
    for (const s of doc.secciones) {
      expect(screen.getByRole('heading', { level: 2, name: s.titulo })).toBeInTheDocument()
      expect(screen.getByText(s.texto)).toBeInTheDocument()
    }
  })

  it('tiene un indice que enlaza cada seccion', async () => {
    renderRuta(doc.ruta)
    await screen.findByTestId(doc.testid)
    const indice = within(screen.getByRole('navigation', { name: 'Contenido del documento' }))
    const enlaces = indice.getAllByRole('link')
    expect(enlaces).toHaveLength(doc.secciones.length)
    enlaces.forEach((a, i) => {
      expect(a).toHaveAttribute('href', `#seccion-${i + 1}`)
      expect(document.getElementById(`seccion-${i + 1}`)).toBeInTheDocument()
    })
  })

  it('muestra la version vigente con fecha legible', async () => {
    renderRuta(doc.ruta)
    await screen.findByTestId(doc.testid)
    expect(screen.getByText('25 de octubre de 2026')).toHaveAttribute('datetime', '2026-10-25')
  })

  it('marca el documento actual y enlaza al otro', async () => {
    renderRuta(doc.ruta)
    await screen.findByTestId(doc.testid)
    const nav = within(screen.getByRole('navigation', { name: 'Documentos legales' }))
    expect(nav.getAllByRole('link')).toHaveLength(2)
    expect(nav.getByRole('link', { current: 'page' })).toHaveAttribute('href', doc.ruta)
    expect(screen.getByRole('link', { name: new RegExp(doc.otro.nombre) })).toHaveAttribute(
      'href',
      doc.otro.ruta
    )
  })

  it('con un enlace directo, Volver lleva a la entrada de la app', async () => {
    renderRuta(doc.ruta)
    await userEvent.click(await screen.findByRole('button', { name: 'Volver' }))
    expect(await screen.findByTestId('landing')).toBeInTheDocument()
  })

  it('el logo lleva a la entrada de la app', async () => {
    renderRuta(doc.ruta)
    await screen.findByTestId(doc.testid)
    expect(screen.getByRole('link', { name: 'Runcity, inicio' })).toHaveAttribute('href', '/')
  })
})

describe('pagina de privacidad con sesion', () => {
  it('ofrece ir a los consentimientos en Ajustes', async () => {
    prepararApp()
    renderRuta('/legal/privacidad')
    await screen.findByTestId('doc-privacidad')
    expect(screen.getByRole('link', { name: /Tus consentimientos/ })).toHaveAttribute(
      'href',
      '/ajustes/privacidad'
    )
  })

  it('sin sesion no ofrece ese enlace', async () => {
    prepararApp({ sesion: null })
    renderRuta('/legal/privacidad')
    await screen.findByTestId('doc-privacidad')
    expect(screen.queryByRole('link', { name: /Tus consentimientos/ })).not.toBeInTheDocument()
  })

  it('terminos no muestra el enlace a consentimientos aunque haya sesion', async () => {
    prepararApp()
    renderRuta('/legal/terminos')
    await screen.findByTestId('doc-terminos')
    expect(screen.queryByRole('link', { name: /Tus consentimientos/ })).not.toBeInTheDocument()
  })
})
