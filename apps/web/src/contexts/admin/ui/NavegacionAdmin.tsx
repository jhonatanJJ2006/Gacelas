import { NavColapsable, type ItemNav } from '../../../shared/ui/navegacion/NavColapsable'

/*
 * Navegacion del panel: adaptador sobre el navbar B colapsable compartido. Solo traduce los
 * destinos del panel a items; el componente no conoce pantallas ni datos.
 */

export interface Destino {
  /** Ruta relativa a /admin ('' es el Resumen). */
  ruta: string
  etiqueta: string
  icono: string
  /** Otras rutas que mantienen este destino activo. */
  tambien?: string[]
  /** Cuenta real (por ejemplo casos pendientes); se omite si no hay numero. */
  insignia?: number
  /** Texto del globo del riel colapsado. */
  globo?: string
  grupo?: string
}

export interface UsuarioNav {
  nombre: string
  rol: string
}

export function NavegacionAdmin({
  destinos,
  usuario,
  sufijo,
}: {
  destinos: Destino[]
  usuario: UsuarioNav
  /** Query a conservar en los enlaces (por ejemplo ?demo). */
  sufijo: string
}) {
  const items: ItemNav[] = destinos.map((d) => ({
    id: d.ruta || 'resumen',
    etiqueta: d.etiqueta,
    a: `/admin/${d.ruta}${sufijo}`,
    icono: d.icono,
    insignia: d.insignia,
    globo: d.globo,
    grupo: d.grupo,
    activoEn: (p) => {
      const actual = p.replace(/^\/admin\/?/, '').split('/')[0]
      return actual === d.ruta || (d.tambien ?? []).includes(actual)
    },
  }))
  return (
    <NavColapsable
      items={items}
      usuario={{ nombre: usuario.nombre, detalle: usuario.rol }}
      etiqueta="Secciones del panel"
      claveAlmacen="runcity.admin.nav.colapsado"
      className="adm-nav"
    />
  )
}
