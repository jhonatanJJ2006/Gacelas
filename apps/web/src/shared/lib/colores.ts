const COLORES = [
  '#A855F7',
  '#10B981',
  '#3B82F6',
  '#EF4444',
  '#A855F7',
  '#EC4899',
  '#14B8A6',
  '#F97316',
]

export const colorCorredor = (i: number) => COLORES[i % COLORES.length]
