import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { CampoContrasena } from '../../components/CampoContrasena'
import { cerrarTecladoAlTocarFuera } from '../teclado'

describe('contraseña y teclado', () => {
  it('el ojo muestra y oculta la contraseña', () => {
    render(<CampoContrasena id="p" aria-label="Contraseña" defaultValue="x" />)
    const campo = screen.getByLabelText('Contraseña') as HTMLInputElement
    expect(campo.type).toBe('password')
    fireEvent.click(screen.getByRole('button', { name: 'Mostrar contraseña' }))
    expect(campo.type).toBe('text')
    fireEvent.click(screen.getByRole('button', { name: 'Ocultar contraseña' }))
    expect(campo.type).toBe('password')
  })
  it('tocar fuera quita el foco del campo', () => {
    const quitar = cerrarTecladoAlTocarFuera()
    render(
      <div>
        <input aria-label="c" />
        <p>fuera</p>
      </div>
    )
    const c = screen.getByLabelText('c')
    c.focus()
    const ev = new Event('pointerdown', { bubbles: true })
    Object.defineProperty(ev, 'pointerType', { value: 'touch' })
    screen.getByText('fuera').dispatchEvent(ev)
    expect(document.activeElement).not.toBe(c)
    quitar()
  })
})
