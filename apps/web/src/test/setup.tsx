import { afterEach, vi } from 'vitest'
import { cleanup } from '@testing-library/react'
import '@testing-library/jest-dom'
import type { ReactNode } from 'react'

const mockUseGLTF = Object.assign(
  vi.fn(() => ({
    scene: {
      clone: vi.fn(() => ({})),
    },
    animations: [],
  })),
  { preload: vi.fn() }
)

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

vi.mock('three/examples/jsm/utils/SkeletonUtils.js', () => ({
  clone: vi.fn((o: unknown) => o),
}))

vi.mock('three', () => ({
  Group: class {},
}))

afterEach(() => {
  cleanup()
})

// jsdom no implementa canvas: sin contexto, la app usa el modo sin WebGL.
HTMLCanvasElement.prototype.getContext = vi.fn(
  () => null
) as unknown as HTMLCanvasElement['getContext']
