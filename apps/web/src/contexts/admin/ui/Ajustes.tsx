import { useState, type ReactNode } from 'react'
import { VERSIONES_LEGALES } from '../../legal/api'
import { Avatar, Boton, Cabecera, Chip, Confirmar, Interruptor, useCarga } from './kit'
import type { AjusteApp, FuenteAdmin } from './fuente'

const etiquetaAjuste = (k: string) => {
  const t = k.replace(/_/g, ' ')
  return t.charAt(0).toUpperCase() + t.slice(1)
}

function Seccion({ titulo, children, ancho }: { titulo: string; children: ReactNode; ancho?: boolean }) {
  return (
    <section className="adm-card adm-pad" style={ancho ? { gridColumn: '1 / -1' } : undefined}>
      <h2 className="adm-h2" style={{ marginBottom: 12 }}>{titulo}</h2>
      {children}
    </section>
  )
}

/** Ajustes: interruptores de la app, version minima, terminos, equipo y notificaciones. */
export function Ajustes({ f }: { f: FuenteAdmin }) {
  const aj = useCarga<AjusteApp[]>(() => f.ajustes(), [f])
  const ter = useCarga<string | null>(() => f.versionTerminos(), [f])
  const us = useCarga(() => f.usuarios(), [f])
  const [nueva, setNueva] = useState('')
  const [v, setV] = useState('')
  const [confirmar, setConfirmar] = useState(false)
  const min = (aj.datos ?? []).find((a) => a.key === 'version_minima_android')?.value as string | undefined
  const equipo = (us.datos ?? []).filter((u) => u.role)
  return (
    <>
      <Cabecera kicker="Administración">Ajustes</Cabecera>
      <div className="adm-g2" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))' }}>
        <Seccion titulo="Ajustes de la app">
          <ul style={{ display: 'grid', gap: 14, listStyle: 'none' }}>
            {(aj.datos ?? [])
              .filter((a) => typeof a.value === 'boolean')
              .map((a) => (
                <li key={a.key} className="adm-fila" style={{ justifyContent: 'space-between' }}>
                  <span style={{ fontWeight: 600 }}>{etiquetaAjuste(a.key)}</span>
                  <Interruptor
                    activo={a.value === true}
                    etiqueta={a.key}
                    onCambio={() => void f.guardarAjuste(a.key, !a.value).then(aj.recargar)}
                  />
                </li>
              ))}
          </ul>
        </Seccion>

        <Seccion titulo="Versión mínima de Android">
          <p>
            Versión actual: <strong className="adm-num">{min ?? 'sin definir'}</strong>
          </p>
          <p className="adm-t2" style={{ fontSize: 13, margin: '6px 0 12px' }}>
            Los APK se publican con scripts/publicar-android.sh en el bucket app-releases (android/latest.json). Las apps por
            debajo de la versión mínima piden actualizar.
          </p>
          <div className="adm-fila">
            <input
              aria-label="Nueva versión mínima"
              value={v}
              onChange={(e) => setV(e.target.value)}
              placeholder="1.2.0"
              className="adm-input"
            />
            <Boton
              tipo="pri"
              onClick={() => {
                if (/^\d+\.\d+\.\d+$/.test(v)) void f.guardarAjuste('version_minima_android', v).then(aj.recargar)
              }}
            >
              Guardar
            </Boton>
          </div>
        </Seccion>

        <Seccion titulo="Términos y consentimientos">
          <p data-testid="version-terminos">
            Versión vigente de los Términos: <strong className="adm-num">{ter.datos ?? '...'}</strong>
          </p>
          <p className="adm-t2" style={{ fontSize: 13, margin: '6px 0 12px' }}>
            Versión incluida en esta build: {VERSIONES_LEGALES.terminos}. Al publicar una versión nueva, las apps vuelven a
            pedir la aceptación.
          </p>
          <div className="adm-fila">
            <input
              aria-label="Nueva versión de los Términos"
              value={nueva}
              onChange={(e) => setNueva(e.target.value)}
              placeholder="AAAA-MM-DD"
              className="adm-input"
            />
            <Boton tipo="pri" onClick={() => nueva.trim() && setConfirmar(true)}>
              Publicar
            </Boton>
          </div>
        </Seccion>

        <Seccion titulo="Notificaciones push">
          <div data-testid="push-no-disponible">
            <Chip>No disponible</Chip>
            <p style={{ fontWeight: 700, margin: '10px 0 4px' }}>Envío de notificaciones push</p>
            <p className="adm-t2" style={{ fontSize: 13 }}>
              Runcity todavía no tiene un servicio de envío push (FCM o APNs) conectado al servidor. Cuando exista, desde aquí
              se podrán redactar avisos y elegir el público. Mientras tanto las notificaciones de la app son locales:
              recordatorios de eventos y estado de la carrera.
            </p>
          </div>
        </Seccion>

        <Seccion titulo="Equipo (admin y moderador)" ancho>
          {equipo.length === 0 ? (
            <p className="adm-t2">Sin integrantes con rol todavía.</p>
          ) : (
            <ul style={{ display: 'grid', gap: 10, listStyle: 'none' }}>
              {equipo.map((u) => (
                <li key={u.id} className="adm-fila">
                  <Avatar nombre={u.full_name ?? u.username} />
                  <div className="adm-grow">
                    <p style={{ fontWeight: 700 }}>{u.full_name ?? u.username}</p>
                    <p className="adm-t3" style={{ fontSize: 12 }}>@{u.username}</p>
                  </div>
                  <Chip tono="info">{u.role === 'admin' ? 'Administrador' : 'Moderador'}</Chip>
                </li>
              ))}
            </ul>
          )}
          <p className="adm-t3" style={{ fontSize: 12, marginTop: 10 }}>
            Asigna roles desde Usuarios, abriendo la ficha de cada persona.
          </p>
        </Seccion>
      </div>
      <Confirmar
        abierto={confirmar}
        peligro={false}
        titulo="Publicar nueva versión de los Términos"
        texto={`Todos los usuarios deberán aceptar la versión ${nueva} al abrir la app. Actualiza antes los textos en la app.`}
        confirmar="Publicar"
        onCerrar={() => setConfirmar(false)}
        onConfirmar={() => {
          void f.publicarTerminos(nueva.trim()).then(() => {
            setConfirmar(false)
            ter.recargar()
          })
        }}
      />
    </>
  )
}
