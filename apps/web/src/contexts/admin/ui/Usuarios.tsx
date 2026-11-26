import { useMemo, useState } from 'react'
import {
  Avatar,
  Boton,
  Cabecera,
  Chip,
  Confirmar,
  ErrorCarga,
  Icono,
  MiniBarras,
  Vacio,
  entero,
  useCarga,
} from './kit'
import { MOTIVOS, estaAbierto } from './casos'
import type { FuenteAdmin, UsuarioAdmin } from './fuente'

type Filtro = 'todos' | 'reportes' | 'suspendidos'

const hace = (n: number) => {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return d.toLocaleDateString('en-CA')
}

/** Usuarios: cifras, tabla con busqueda y ficha del usuario elegido. */
export function Usuarios({ f }: { f: FuenteAdmin }) {
  const us = useCarga(() => f.usuarios(), [f])
  const cola = useCarga(() => f.reportes(), [f])
  const met = useCarga(() => f.metricas(), [f])
  const serie = useCarga(() => f.estadisticas(hace(13), hace(0)), [f])
  const [q, setQ] = useState('')
  const [filtro, setFiltro] = useState<Filtro>('todos')
  const [abierto, setAbierto] = useState<string | null>(null)
  const [confirmar, setConfirmar] = useState(false)

  // Reportes abiertos contra cada usuario (casos de tipo usuario por id del perfil).
  const reportesDe = useMemo(() => {
    const m = new Map<string, { n: number; ultimo: string; motivo: string }>()
    for (const r of cola.datos ?? []) {
      if (r.target_type !== 'usuario' || !estaAbierto(r)) continue
      const e = m.get(r.target_id)
      m.set(r.target_id, {
        n: (e?.n ?? 0) + 1,
        ultimo: e?.ultimo ?? r.detail,
        motivo: e?.motivo ?? r.reason,
      })
    }
    return m
  }, [cola.datos])

  const todos = us.datos ?? []
  const lista = todos
    .filter((u) => `${u.username} ${u.full_name ?? ''}`.toLowerCase().includes(q.toLowerCase()))
    .filter((u) =>
      filtro === 'reportes'
        ? reportesDe.has(u.id)
        : filtro === 'suspendidos'
          ? !!u.suspended_at
          : true
    )
  const actual = lista.find((u) => u.id === abierto) ?? lista[0] ?? null
  const dias = serie.datos?.serie ?? []
  const suspendidos = todos.filter((u) => u.suspended_at).length

  return (
    <>
      <Cabecera
        kicker="Comunidad Runcity"
        extra={
          <>
            <label className="adm-fila adm-input" style={{ gap: 8, minWidth: 240 }}>
              <Icono nombre="search" tam={18} />
              <input
                aria-label="Buscar usuarios"
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder="Buscar por nombre o usuario"
                style={{
                  background: 'none',
                  border: 0,
                  color: 'inherit',
                  font: 'inherit',
                  width: '100%',
                  outline: 'none',
                }}
              />
            </label>
            {(
              [
                ['todos', 'Todos'],
                ['reportes', 'Con reportes'],
                ['suspendidos', 'Suspendidos'],
              ] as [Filtro, string][]
            ).map(([k, t]) => (
              <button
                key={k}
                type="button"
                className="adm-chip c-neu"
                aria-pressed={filtro === k}
                onClick={() => setFiltro(k)}
              >
                {t}
              </button>
            ))}
          </>
        }
      >
        Usuarios
      </Cabecera>
      <ErrorCarga mensaje={us.error} />

      <div className="adm-tres" style={{ marginBottom: 16 }} data-testid="usuarios-kpis">
        <div className="adm-kpi adm-fila" style={{ justifyContent: 'space-between' }}>
          <div>
            <p className="adm-t2" style={{ fontSize: 12 }}>
              Usuarios totales
            </p>
            <p className="adm-num" style={{ fontSize: 28 }}>
              {met.datos ? entero(met.datos.usuarios) : '-'}
            </p>
            <p className="adm-t3" style={{ fontSize: 12 }}>
              {us.datos ? `${suspendidos} suspendidos` : ''}
            </p>
          </div>
        </div>
        <div className="adm-kpi adm-fila" style={{ justifyContent: 'space-between' }}>
          <div>
            <p className="adm-t2" style={{ fontSize: 12 }}>
              Altas en 7 días
            </p>
            <p className="adm-num" style={{ fontSize: 28 }}>
              {met.datos ? `+${entero(met.datos.usuarios_7d)}` : '-'}
            </p>
          </div>
          <MiniBarras valores={dias.map((d) => d.nuevos)} tono="ok" />
        </div>
        <div className="adm-kpi adm-fila" style={{ justifyContent: 'space-between' }}>
          <div>
            <p className="adm-t2" style={{ fontSize: 12 }}>
              Carreras en 7 días
            </p>
            <p className="adm-num" style={{ fontSize: 28 }}>
              {met.datos ? entero(met.datos.carreras_7d) : '-'}
            </p>
          </div>
          <MiniBarras valores={dias.map((d) => d.carreras)} tono="warn" />
        </div>
      </div>

      <div className="adm-g2 adm-g-usr">
        <section className="adm-card adm-pad" style={{ alignSelf: 'start', overflowX: 'auto' }}>
          {lista.length === 0 ? (
            <Vacio titulo="Sin usuarios en esta lista" texto="Prueba con otra búsqueda o filtro." />
          ) : (
            <table className="adm-tabla" data-testid="tabla-usuarios">
              <thead>
                <tr>
                  <th>Usuario</th>
                  <th>Rol</th>
                  <th>Km</th>
                  <th>Estado</th>
                  <th>Rep.</th>
                </tr>
              </thead>
              <tbody>
                {lista.map((u) => {
                  const rep = reportesDe.get(u.id)?.n ?? 0
                  return (
                    <tr
                      key={u.id}
                      aria-selected={actual?.id === u.id}
                      onClick={() => setAbierto(u.id)}
                    >
                      <td>
                        <div className="adm-fila">
                          <Avatar
                            nombre={u.full_name ?? u.username}
                            tono={u.suspended_at ? 'bad' : 'pri'}
                          />
                          <div style={{ minWidth: 0 }}>
                            <p style={{ fontWeight: 700 }}>{u.full_name ?? u.username}</p>
                            <p className="adm-t3" style={{ fontSize: 12 }}>
                              @{u.username}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td>
                        {u.role ? (
                          <Chip tono="info">
                            {u.role === 'admin' ? 'Administrador' : 'Moderador'}
                          </Chip>
                        ) : (
                          <Chip>Corredor</Chip>
                        )}
                      </td>
                      <td className="adm-num">{entero(u.km)}</td>
                      <td>
                        {u.suspended_at ? (
                          <Chip tono="bad">Suspendida</Chip>
                        ) : rep > 0 ? (
                          <Chip tono="warn">Revisar</Chip>
                        ) : (
                          <Chip tono="ok">Activa</Chip>
                        )}
                      </td>
                      <td
                        className="adm-num"
                        style={{ color: rep > 0 ? 'hsl(var(--peligro-texto))' : undefined }}
                      >
                        {rep}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )}
          <p className="adm-t3" style={{ fontSize: 12, marginTop: 10 }}>
            Último acceso no se muestra: el servidor no registra ese dato todavía.
          </p>
        </section>

        {actual && (
          <Ficha
            u={actual}
            rep={reportesDe.get(actual.id) ?? null}
            f={f}
            recargar={us.recargar}
            onSuspender={() => setConfirmar(true)}
          />
        )}
      </div>

      <Confirmar
        abierto={confirmar && actual != null}
        titulo={actual?.suspended_at ? 'Reactivar cuenta' : 'Suspender cuenta'}
        texto={
          actual?.suspended_at
            ? `${actual.full_name ?? actual.username} volverá a usar la app.`
            : `${actual?.full_name ?? actual?.username} no podrá usar la app mientras esté suspendida.`
        }
        confirmar={actual?.suspended_at ? 'Reactivar' : 'Suspender'}
        peligro={!actual?.suspended_at}
        onCerrar={() => setConfirmar(false)}
        onConfirmar={() => {
          if (actual) void f.suspender(actual.id, !actual.suspended_at).then(us.recargar)
          setConfirmar(false)
        }}
      />
    </>
  )
}

function Ficha({
  u,
  rep,
  f,
  recargar,
  onSuspender,
}: {
  u: UsuarioAdmin
  rep: { n: number; ultimo: string; motivo: string } | null
  f: FuenteAdmin
  recargar: () => void
  onSuspender: () => void
}) {
  const nombre = u.full_name ?? u.username
  return (
    <aside
      className="adm-card"
      style={{ alignSelf: 'start', overflow: 'hidden' }}
      aria-label="Ficha del usuario"
      data-testid="ficha-usuario"
    >
      <div
        style={{
          background: 'hsl(var(--realce-primario))',
          padding: '22px 18px',
          textAlign: 'center',
        }}
      >
        <Avatar nombre={nombre} tam={64} tono="warn" />
        <h2 style={{ fontSize: 20, fontWeight: 800, marginTop: 8 }}>{nombre}</h2>
        <p className="adm-t2" style={{ fontSize: 13 }}>
          @{u.username}
        </p>
        <div className="adm-fila" style={{ justifyContent: 'center', marginTop: 8, gap: 6 }}>
          <Chip>
            {u.role === 'admin'
              ? 'Administrador'
              : u.role === 'moderador'
                ? 'Moderador'
                : 'Corredor'}
          </Chip>
          {u.suspended_at ? (
            <Chip tono="bad">Suspendida</Chip>
          ) : rep ? (
            <Chip tono="warn">Revisar</Chip>
          ) : null}
        </div>
      </div>
      <div style={{ padding: 16, display: 'grid', gap: 12 }}>
        <div className="adm-tres" style={{ gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
          <Dato icono="flag" valor={entero(u.carreras)} etiqueta="Carreras" />
          <Dato icono="map" valor={entero(u.km)} etiqueta="Km" />
          <Dato
            icono="warning"
            valor={String(rep?.n ?? 0)}
            etiqueta="Reportes"
            tono={rep ? 'bad' : undefined}
          />
        </div>
        {rep && (
          <div className="adm-card-el" style={{ padding: 12 }}>
            <p className="adm-lbl" style={{ color: 'hsl(var(--advertencia-texto))' }}>
              Señal de riesgo
            </p>
            <p style={{ fontSize: 14, marginTop: 2 }}>
              {MOTIVOS[rep.motivo] ?? rep.motivo}
              {rep.ultimo ? `: ${rep.ultimo}` : ''}
            </p>
          </div>
        )}
        <p className="adm-t3" style={{ fontSize: 12 }}>
          Alta: {new Date(u.created_at).toLocaleDateString('es-EC')}
        </p>
        <Boton tipo={u.suspended_at ? 'pri' : 'bad'} completo onClick={onSuspender}>
          {u.suspended_at ? 'Reactivar cuenta' : 'Suspender cuenta'}
        </Boton>
        <label className="adm-label" style={{ marginTop: 0 }}>
          Rol
          <select
            aria-label="Rol del usuario"
            value={u.role ?? ''}
            onChange={(e) =>
              void f
                .cambiarRol(u.id, (e.target.value || null) as UsuarioAdmin['role'])
                .then(recargar)
            }
            className="adm-input"
          >
            <option value="">Corredor</option>
            <option value="moderador">Moderador</option>
            <option value="admin">Administrador</option>
          </select>
        </label>
      </div>
    </aside>
  )
}

function Dato({
  icono,
  valor,
  etiqueta,
  tono,
}: {
  icono: string
  valor: string
  etiqueta: string
  tono?: 'bad'
}) {
  return (
    <div className="adm-card-el" style={{ padding: '10px 6px', textAlign: 'center' }}>
      <span
        className={tono ? 'c-bad' : 'adm-t2'}
        style={{ color: tono ? 'hsl(var(--tt))' : undefined }}
      >
        <Icono nombre={icono} tam={18} />
      </span>
      <p className="adm-num" style={{ fontSize: 20 }}>
        {valor}
      </p>
      <p className="adm-t3" style={{ fontSize: 11 }}>
        {etiqueta}
      </p>
    </div>
  )
}
