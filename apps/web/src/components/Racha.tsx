import { mapaCalor, rachaEnRiesgo, type Racha } from '../lib/rachas'
import { useApp, useRacha } from '../store/app'

const plural = (n: number, uno: string, varios: string) => `${n} ${n === 1 ? uno : varios}`

/** Insignia con llama: racha diaria actual. */
export function InsigniaRacha({
  compacta = false,
  testId = 'racha',
}: {
  compacta?: boolean
  testId?: string
}) {
  const r = useRacha()
  const riesgo = rachaEnRiesgo(r, Date.now())
  return (
    <div
      className={`inline-flex shrink-0 items-center gap-1 rounded-full border shadow-sm ${
        compacta ? 'px-2.5 py-1' : 'px-3 py-1.5'
      } ${r.diaria > 0 ? 'bg-primary/15 border-primary/40' : 'bg-surface-container-highest/90 border-white/5'}`}
      title={riesgo ? 'Corre hoy para mantener tu racha' : undefined}
    >
      <span
        className={`material-symbols-outlined text-[16px] ${r.diaria > 0 ? 'text-primary' : 'text-text-secondary'}`}
        style={{ fontVariationSettings: r.diaria > 0 ? "'FILL' 1" : undefined }}
      >
        local_fire_department
      </span>
      <span
        className="font-label-md text-label-md font-semibold text-primary whitespace-nowrap"
        data-testid={testId}
      >
        Racha {r.diaria}d
      </span>
      {riesgo && (
        <span className="text-[11px] text-warning font-semibold" data-testid="racha-riesgo">
          · en riesgo
        </span>
      )}
    </div>
  )
}

/** Tarjeta de Perfil: rachas diaria y semanal con mapa de calor de 12 semanas. */
export function TarjetaRacha() {
  const r = useRacha()
  const carreras = useApp((s) => s.carreras)
  const semanas = mapaCalor(carreras, Date.now())
  const max = Math.max(1, ...semanas.flat().map((c) => c.km))
  const nivel = (km: number) =>
    km === 0
      ? 'bg-surface-container-highest'
      : km < max / 3
        ? 'bg-primary/35'
        : km < (2 * max) / 3
          ? 'bg-primary/65'
          : 'bg-primary'
  return (
    <section
      className="bg-surface-container border border-border-subtle rounded-2xl p-5 mb-6"
      data-testid="tarjeta-racha"
      aria-labelledby="titulo-racha"
    >
      <div className="flex items-center justify-between mb-4">
        <h2
          id="titulo-racha"
          className="font-title-md text-title-md text-text-primary flex items-center gap-2"
        >
          <span
            className="material-symbols-outlined text-primary"
            style={{ fontVariationSettings: "'FILL' 1" }}
          >
            local_fire_department
          </span>
          Rachas
        </h2>
        {rachaEnRiesgo(r, Date.now()) && (
          <span className="text-xs font-semibold text-warning">Corre hoy para no perderla</span>
        )}
      </div>
      <div className="grid grid-cols-2 gap-3 mb-5">
        <div className="rounded-xl bg-primary/10 border border-primary/30 p-3">
          <p className="text-xs text-text-secondary">Racha diaria</p>
          <p className="font-headline-sm text-headline-sm text-primary" data-testid="racha-diaria">
            {plural(r.diaria, 'día', 'días')}
          </p>
          <p className="text-xs text-text-secondary">
            Mejor: {plural(r.mejorDiaria, 'día', 'días')}
          </p>
        </div>
        <div className="rounded-xl bg-secondary/10 border border-secondary/30 p-3">
          <p className="text-xs text-text-secondary">Racha semanal</p>
          <p
            className="font-headline-sm text-headline-sm text-secondary"
            data-testid="racha-semanal"
          >
            {plural(r.semanal, 'semana', 'semanas')}
          </p>
          <p className="text-xs text-text-secondary">
            Esta semana {Math.min(r.carrerasSemana, r.metaSemanal)}/{r.metaSemanal} carreras ·
            Mejor: {r.mejorSemanal}
          </p>
        </div>
      </div>
      <div
        className="flex gap-1"
        role="img"
        aria-label="Actividad de las últimas 12 semanas"
        data-testid="mapa-calor"
      >
        <div className="flex flex-col gap-1 mr-1 text-[10px] text-text-secondary leading-3">
          {['L', '', 'X', '', 'V', '', 'D'].map((d, i) => (
            <span key={i} className="h-3">
              {d}
            </span>
          ))}
        </div>
        {semanas.map((s, i) => (
          <div key={i} className="flex flex-col gap-1 flex-1">
            {s.map((c) => (
              <span
                key={c.dia}
                title={c.km ? `${c.km.toFixed(1)} km` : undefined}
                className={`h-3 w-full max-w-4 rounded-[3px] ${c.futuro ? 'bg-transparent' : nivel(c.km)}`}
              />
            ))}
          </div>
        ))}
      </div>
    </section>
  )
}

export function CelebracionRacha({ antes, despues }: { antes: Racha; despues: Racha }) {
  const subeDiaria = despues.diaria > antes.diaria
  const subeSemanal = despues.semanal > antes.semanal
  if (!subeDiaria && !subeSemanal) return null
  return (
    <div
      role="status"
      data-testid="celebracion-racha"
      className="mb-6 rounded-3xl border border-primary/40 bg-gradient-to-br from-primary/25 to-secondary/15 p-5 flex items-center gap-4 animate-[pulse_1.2s_ease-in-out_2]"
    >
      <span
        className="material-symbols-outlined text-[48px] text-primary"
        style={{ fontVariationSettings: "'FILL' 1" }}
      >
        local_fire_department
      </span>
      <div>
        {subeDiaria && (
          <p className="font-headline-sm text-headline-sm text-text-primary">
            ¡Racha de {plural(despues.diaria, 'día', 'días')}!
          </p>
        )}
        {subeSemanal && (
          <p className="text-text-primary font-semibold">
            Meta semanal cumplida: {plural(despues.semanal, 'semana', 'semanas')} seguidas
          </p>
        )}
        <p className="text-sm text-text-secondary">Guarda la carrera para sumarla.</p>
      </div>
    </div>
  )
}
