import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Runner3D } from '../Runner3D'
import { isWebGLAvailable } from '../../lib/webgl'

vi.mock('../../lib/webgl', () => ({ isWebGLAvailable: vi.fn(() => true) }))

beforeEach(() => {
  vi.mocked(isWebGLAvailable).mockReturnValue(true)
})

describe('Runner3D', () => {
  it('renderiza el canvas de Three.js', () => {
    render(<Runner3D />)

    expect(screen.getByTestId('three-canvas')).toBeInTheDocument()
  })

  it('acepta diferentes animaciones como prop', () => {
    const { rerender } = render(<Runner3D animation="Idle" />)
    expect(screen.getByTestId('three-canvas')).toBeInTheDocument()

    rerender(<Runner3D animation="Run" />)
    expect(screen.getByTestId('three-canvas')).toBeInTheDocument()

    rerender(<Runner3D animation="Jog" />)
    expect(screen.getByTestId('three-canvas')).toBeInTheDocument()
  })

  it('aplica clases CSS personalizadas', () => {
    const { container } = render(<Runner3D className="custom-class" />)
    const wrapper = container.querySelector('.custom-class')
    expect(wrapper).toBeInTheDocument()
  })

  it('acepta configuración de cámara personalizada', () => {
    render(<Runner3D cameraPosition={[1, 2, 3]} />)
    expect(screen.getByTestId('three-canvas')).toBeInTheDocument()
  })

  it('acepta la prop autoRotate', () => {
    render(<Runner3D autoRotate={true} />)
    expect(screen.getByTestId('three-canvas')).toBeInTheDocument()
  })

  it('muestra un fallback estático cuando no hay WebGL', () => {
    vi.mocked(isWebGLAvailable).mockReturnValue(false)
    render(<Runner3D />)
    expect(screen.getByTestId('runner3d-fallback')).toBeInTheDocument()
    expect(screen.queryByTestId('three-canvas')).not.toBeInTheDocument()
  })
})
