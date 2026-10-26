/** Estrellas de solo lectura (se rellenan por mitades). */
export function Estrellas({ valor, tamano = 20 }: { valor: number; tamano?: number }) {
  return (
    <span className="inline-flex text-primary-400" aria-hidden="true">
      {[1, 2, 3, 4, 5].map((i) => (
        <span
          key={i}
          className="material-symbols-outlined"
          style={{
            fontSize: tamano,
            fontVariationSettings: valor >= i - 0.25 ? "'FILL' 1" : "'FILL' 0",
          }}
        >
          {valor >= i - 0.75 && valor < i - 0.25 ? 'star_half' : 'star'}
        </span>
      ))}
    </span>
  )
}

/** Selector de 1 a 5 estrellas accesible (grupo de radios). */
export function SelectorEstrellas({
  valor,
  onCambiar,
}: {
  valor: number
  onCambiar: (v: number) => void
}) {
  return (
    <div role="radiogroup" aria-label="Valoración" className="flex gap-1">
      {[1, 2, 3, 4, 5].map((i) => (
        <button
          key={i}
          type="button"
          role="radio"
          aria-checked={valor === i}
          aria-label={`${i} ${i === 1 ? 'estrella' : 'estrellas'}`}
          onClick={() => onCambiar(i)}
          className="w-11 h-11 flex items-center justify-center rounded-full text-primary-400 hover:bg-surface-container-high"
        >
          <span
            className="material-symbols-outlined text-[28px]"
            style={{ fontVariationSettings: i <= valor ? "'FILL' 1" : "'FILL' 0" }}
          >
            star
          </span>
        </button>
      ))}
    </div>
  )
}
