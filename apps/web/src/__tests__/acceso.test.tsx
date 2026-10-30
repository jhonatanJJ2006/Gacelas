import { screen, waitFor } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { useIdentidad } from '../contexts/identidad/application/estado'
import { prepararApp, renderRuta } from '../test/utils'

describe('acceso y rutas protegidas', () => {
  beforeEach(() => prepararApp({ sesion: null }))

  it('sin sesion, una ruta protegida redirige a bienvenida', async () => {
    renderRuta('/perfil')
    expect(await screen.findByRole('button', { name: /empezar/i })).toBeInTheDocument()
  })

  it('Saltar lleva a inicio de sesion', async () => {
    renderRuta('/bienvenida')
    await userEvent.click(await screen.findByRole('button', { name: /saltar/i }))
    expect(await screen.findByText('Bienvenido de vuelta')).toBeInTheDocument()
  })

  it('registro valida y crea la cuenta', async () => {
    renderRuta('/registro')
    await userEvent.type(await screen.findByLabelText('Nombre completo'), 'Jhona')
    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'jhona@utpl.edu.ec')
    await userEvent.click(screen.getByLabelText('Acepto los Términos y la Política de privacidad'))
    await userEvent.type(screen.getByLabelText('Contraseña'), 'corta')
    await userEvent.click(screen.getByRole('button', { name: /crear cuenta/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/8 caracteres/)
    await userEvent.clear(screen.getByLabelText('Contraseña'))
    await userEvent.type(screen.getByLabelText('Contraseña'), 'gacela123')
    await userEvent.click(screen.getByRole('button', { name: /crear cuenta/i }))
    expect(await screen.findByText('Activa tu ubicación')).toBeInTheDocument()
    expect(JSON.parse(localStorage.getItem('gacelas:consentimientos') ?? '[]')).toHaveLength(2)
    expect(useIdentidad.getState().sesion?.email).toBe('jhona@utpl.edu.ec')
  })

  it('registro enlaza terminos y privacidad', async () => {
    renderRuta('/registro')
    await screen.findByLabelText('Acepto los Términos y la Política de privacidad')
    expect(screen.getByRole('link', { name: 'Términos' })).toHaveAttribute(
      'href',
      '/legal/terminos'
    )
    expect(screen.getByRole('link', { name: 'Política de privacidad' })).toHaveAttribute(
      'href',
      '/legal/privacidad'
    )
  })

  it('terminos se leen sin sesion', async () => {
    renderRuta('/legal/terminos')
    expect(await screen.findByTestId('doc-terminos')).toBeInTheDocument()
    expect(screen.getByText('1. Aceptación')).toBeInTheDocument()
  })

  it('inicio de sesion con error y luego correcto', async () => {
    await useIdentidad.getState().registrar('Jhona', 'jhona@utpl.edu.ec', 'gacela123')
    await useIdentidad.getState().cerrarSesion()
    renderRuta('/inicio-sesion')
    await userEvent.type(await screen.findByLabelText('Correo electrónico'), 'jhona@utpl.edu.ec')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'incorrecta1')
    await userEvent.click(screen.getByRole('button', { name: /iniciar sesión/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña incorrectos')
    await userEvent.clear(screen.getByLabelText('Contraseña'))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Contraseña'), 'gacela123')
    await userEvent.click(screen.getByRole('button', { name: /iniciar sesión/i }))
    expect(await screen.findByText(/, Jhona/)).toBeInTheDocument()
  })

  it('recuperar contraseña valida el correo', async () => {
    renderRuta('/recuperar-contrasena')
    await userEvent.type(await screen.findByLabelText('Correo electrónico'), 'malo')
    await userEvent.click(screen.getByRole('button', { name: /enviar enlace/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/no es válido/)
    await userEvent.clear(screen.getByLabelText('Correo electrónico'))
    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'jhona@utpl.edu.ec')
    await userEvent.click(screen.getByRole('button', { name: /enviar enlace/i }))
    await waitFor(() =>
      expect(screen.queryByRole('button', { name: /enviar enlace/i })).not.toBeInTheDocument()
    )
  })

  it('con sesion, las pantallas de acceso llevan a inicio', async () => {
    prepararApp()
    renderRuta('/inicio-sesion')
    expect(await screen.findByText(/, Jhona/)).toBeInTheDocument()
  })
})
