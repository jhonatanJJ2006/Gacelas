import { afterEach, vi } from 'vitest'
import { cleanup } from '@testing-library/react'
import '@testing-library/jest-dom'
import { ReactNode } from 'react'

const mockUseGLTF = vi.fn(() => ({
  scene: {
    clone: vi.fn(() => ({})),
  },
  animations: [],
}))

mockUseGLTF.preload = vi.fn()

vi.mock('@react-three/fiber', () => ({
  Canvas: ({ children }: { children: ReactNode }) => {
    return <div data-testid="three-canvas">{children}</div>
  },
}))

vi.mock('@react-three/drei', () => ({
  useGLTF: mockUseGLTF,
  useAnimations: vi.fn(() => ({
    actions: {},
  })),
  OrbitControls: () => null,
}))

vi.mock('three', () => ({
  Group: class {},
}))

afterEach(() => {
  cleanup()
})
