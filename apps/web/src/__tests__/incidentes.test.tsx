import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { TarjetaSeguridad } from '../features/incidentes/TarjetaSeguridad'
import { HojaReportarIncidente } from '../features/incidentes/HojaReportarIncidente'
import { ListaIncidentes } from '../features/incidentes/IncidentesRecientes'
import {
  AVISO_SEGURIDAD,
  incidentesEjemplo,
  nivelDeIndice,
  SEGURIDAD_EJEMPLO,
  TIPOS_INCIDENTE,
} from '../features/incidentes/incidentes'

describe('incidentes', () => {
  it('clasifica el indice en bajo, medio y alto', () => {
    expect(nivelDeIndice(0.5)).toBe('bajo')
    expect(nivelDeIndice(1)).toBe('medio')
    expect(nivelDeIndice(2)).toBe('alto')
  })

  it('la tarjeta muestra niveles por dia, tipos frecuentes, aviso y fuente', () => {
    render(<TarjetaSeguridad rutaId="r1" inicial={SEGURIDAD_EJEMPLO} />)
    expect(screen.getByText('Seguridad de la zona')).toBeInTheDocument()
    expect(screen.getByText('Sáb')).toBeInTheDocument()
    expect(screen.getByText('Ejemplo: siniestro de tránsito')).toBeInTheDocument()
    expect(screen.getByText(AVISO_SEGURIDAD)).toBeInTheDocument()
    expect(screen.getByText(/Fuente:/)).toBeInTheDocument()
  })

  it('la hoja de reporte solo exige el tipo', () => {
    render(
      <HojaReportarIncidente abierta ubicacion={{ lat: -3.99, lng: -79.2 }} onCerrar={() => {}} />
    )
    const enviar = screen.getByRole('button', { name: 'Enviar reporte' })
    expect(enviar).toBeDisabled()
    expect(TIPOS_INCIDENTE).toHaveLength(8)
    fireEvent.click(screen.getByRole('button', { name: /Perros agresivos/ }))
    expect(enviar).toBeEnabled()
  })

  it('lista incidentes con la opcion de confirmar', () => {
    render(<ListaIncidentes lista={incidentesEjemplo()} />)
    expect(screen.getAllByRole('button', { name: 'Yo también lo vi' })).toHaveLength(3)
  })
})
