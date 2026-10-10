import { beforeEach, describe, expect, it } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { prepararApp, renderRuta } from '../test/utils'
import { useApp } from '../store/app'

describe('acceso y rutas protegidas', () => {
  beforeEach(() => prepararApp({ sesion: null }))

  it('sin sesion, una ruta protegida redirige a bienvenida', async () => {
    renderRuta('/perfil')
    expect(await screen.findByRole('button', { name: /crear cuenta/i })).toBeInTheDocument()
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
    await userEvent.type(screen.getByLabelText('Contraseña'), 'corta')
    await userEvent.click(screen.getByRole('button', { name: /crear cuenta/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/8 caracteres/)
    await userEvent.clear(screen.getByLabelText('Contraseña'))
    await userEvent.type(screen.getByLabelText('Contraseña'), 'gacela123')
    await userEvent.click(screen.getByRole('button', { name: /crear cuenta/i }))
    expect(await screen.findByText('Activa tu ubicación')).toBeInTheDocument()
    expect(useApp.getState().sesion?.email).toBe('jhona@utpl.edu.ec')
  })

  it('registro muestra terminos y privacidad', async () => {
    renderRuta('/registro')
    await userEvent.click(await screen.findByRole('button', { name: 'Términos de Servicio' }))
    expect(screen.getByRole('dialog', { name: 'Términos de Servicio' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Entendido' }))
    await userEvent.click(screen.getByRole('button', { name: 'Política de Privacidad' }))
    expect(screen.getByRole('dialog', { name: 'Política de Privacidad' })).toBeInTheDocument()
  })

  it('inicio de sesion con error y luego correcto', async () => {
    await useApp.getState().registrar('Jhona', 'jhona@utpl.edu.ec', 'gacela123')
    await useApp.getState().cerrarSesion()
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
