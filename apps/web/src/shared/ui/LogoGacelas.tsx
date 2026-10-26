/**
 * Logo de marca A "Gacela en ruta" (docs/MARCA.md): icono de la app con la palabra GACELAS.
 * Sustituye al cuadro con el simbolo "sprint" que se usaba antes.
 */
export function IconoGacelas({ tam = 40, className = '' }: { tam?: number; className?: string }) {
  return (
    <img
      src="/favicon.svg"
      width={tam}
      height={tam}
      alt="Gacelas"
      className={`shrink-0 rounded-[22%] shadow-[0_0_18px_rgba(168,85,247,0.35)] ${className}`}
      style={{ width: tam, height: tam }}
    />
  )
}

export function LogoGacelas({
  tam = 48,
  subtitulo = 'LOJA, ECUADOR',
  grande = false,
}: {
  tam?: number
  subtitulo?: string | null
  grande?: boolean
}) {
  return (
    <div className="flex items-center gap-2.5" data-testid="logo-gacelas">
      <IconoGacelas tam={tam} />
      <div className="flex flex-col">
        <span
          className={`font-headline-md text-text-primary font-extrabold tracking-[0.08em] leading-none ${grande ? 'text-lg' : 'text-base'}`}
        >
          GACELAS
        </span>
        {subtitulo && (
          <span
            className={`font-semibold text-[#C084FC] tracking-widest ${grande ? 'text-xs' : 'text-[10px] mt-0.5'}`}
          >
            {subtitulo}
          </span>
        )}
      </div>
    </div>
  )
}
