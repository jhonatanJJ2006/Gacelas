import { beforeEach, describe, expect, it } from 'vitest'
import { screen, within } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { carreraPrueba, prepararApp, renderRuta } from '../test/utils'
import { useApp } from '../store/app'

describe('perfil', () => {
  beforeEach(() =>
    prepararApp({
      carreras: [
        carreraPrueba({ titulo: 'Fondo largo', distanciaKm: 10, publica: true }),
        carreraPrueba({ id: 'c2', titulo: 'Suave', distanciaKm: 3, publica: false }),
      ],
    })
  )

  it('muestra datos reales, estadisticas y borra carreras', async () => {
    renderRuta('/perfil')
    expect(await screen.findByText('Jhona Runner')).toBeInTheDocument()
    expect(screen.getByTestId('total-carreras')).toHaveTextContent('2')
    expect(screen.getByText('13.0')).toBeInTheDocument()
    expect(screen.getAllByTestId('carrera-guardada')).toHaveLength(2)
    await userEvent.click(screen.getByRole('button', { name: 'Eliminar Suave' }))
    await userEvent.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Eliminar' })
    )
    expect(screen.queryByText('Suave')).not.toBeInTheDocument()
    expect(useApp.getState().carreras).toHaveLength(1)
  })

  it('editar perfil valida y guarda', async () => {
    renderRuta('/perfil/editar')
    const nombre = await screen.findByLabelText('Nombre completo')
    await userEvent.clear(nombre)
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))
    expect(screen.getByRole('alert')).toHaveTextContent(/nombre/i)
    await userEvent.type(nombre, 'Jhona Gacela')
    await userEvent.type(screen.getByLabelText('Bio'), 'Corro en Loja')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))
    expect(await screen.findByText('Jhona Gacela')).toBeInTheDocument()
    expect(screen.getByText('Corro en Loja')).toBeInTheDocument()
    expect(
      JSON.parse(localStorage.getItem('gacelas:auth')!).cuentas['jhona@utpl.edu.ec'].perfil.nombre
    ).toBe('Jhona Gacela')
  })

  it('avatar guarda la configuracion', async () => {
    renderRuta('/perfil/avatar')
    await userEvent.click(await screen.findByRole('button', { name: 'Equipación Verde' }))
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))
    expect(useApp.getState().ajustes.avatar.equipacion).toBe(1)
    expect(await screen.findByText('Jhona Runner')).toBeInTheDocument()
  })
})

describe('ajustes', () => {
  beforeEach(() => prepararApp())

  it('modo claro, unidades y cerrar sesion', async () => {
    renderRuta('/ajustes')
    await userEvent.click(await screen.findByRole('button', { name: 'Cambiar tema' }))
    expect(document.documentElement).toHaveClass('tema-claro')
    await userEvent.click(screen.getByRole('button', { name: 'mi' }))
    await userEvent.click(screen.getByRole('switch', { name: 'Autopausa' }))
    await userEvent.selectOptions(screen.getByLabelText('Cuenta regresiva'), '5')
    await userEvent.selectOptions(screen.getByLabelText('Ritmo objetivo'), '300')
    expect(useApp.getState().ajustes).toMatchObject({
      modoOscuro: false,
      unidadesMetricas: false,
      autopausa: false,
      cuentaRegresivaSeg: 5,
      ritmoObjetivoSeg: 300,
    })
    expect(JSON.parse(localStorage.getItem('gacelas:ajustes')!).unidadesMetricas).toBe(false)
    await userEvent.click(screen.getByRole('button', { name: /Acerca de/ }))
    expect(screen.getByRole('dialog')).toHaveTextContent('Gacelas Loja')
    await userEvent.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Cerrar' })
    )
    await userEvent.click(screen.getByRole('button', { name: /Cerrar sesión/ }))
    await userEvent.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Cerrar sesión' })
    )
    expect(await screen.findByText('Corre con la manada de Loja')).toBeInTheDocument()
    expect(useApp.getState().sesion).toBeNull()
  })

  it('notificaciones: reto mensual y preferencias de alertas en la bandeja', async () => {
    renderRuta('/notificaciones')
    expect(await screen.findByTestId('reto-notificacion')).toHaveTextContent('Andino')
    await userEvent.click(screen.getByRole('switch', { name: 'Eventos del club' }))
    expect(useApp.getState().ajustes.notifEventos).toBe(false)
  })

  it('privacidad: radios sugeridos y actividades', async () => {
    renderRuta('/ajustes/privacidad')
    await userEvent.click(await screen.findByRole('button', { name: '1 km' }))
    await userEvent.click(screen.getByRole('switch', { name: 'Ocultar datos biométricos' }))
    expect(useApp.getState().ajustes).toMatchObject({ radioHogarM: 1000, ocultarBiometricos: true })
  })

  it('privacidad y notificaciones persisten', async () => {
    renderRuta('/ajustes/privacidad')
    await userEvent.click(await screen.findByRole('radio', { name: /Solo yo/ }))
    await userEvent.click(screen.getByRole('switch', { name: /Ubicación precisa/ }))
    await userEvent.click(screen.getByRole('switch', { name: /Ocultar ubicación de casa/ }))
    expect(screen.queryByTestId('radio-hogar-valor')).not.toBeInTheDocument()
    expect(useApp.getState().ajustes).toMatchObject({
      visibilidad: 'privado',
      ubicacionPrecisa: false,
      ocultarHogar: false,
    })
    await userEvent.click(screen.getByRole('switch', { name: /Ocultar ubicación de casa/ }))
    expect(screen.getByTestId('radio-hogar-valor')).toHaveTextContent('m')
    expect(JSON.parse(localStorage.getItem('gacelas:ajustes')!).visibilidad).toBe('privado')
  })

  it('notificaciones desactivadas ocultan su tipo', async () => {
    renderRuta('/ajustes/notificaciones')
    await userEvent.click(await screen.findByRole('switch', { name: /Me gusta/ }))
    expect(useApp.getState().ajustes.notifLikes).toBe(false)
    renderRuta('/notificaciones')
    expect(await screen.findAllByTestId('notificacion')).toHaveLength(4)
  })
})
