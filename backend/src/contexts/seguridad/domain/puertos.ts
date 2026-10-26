import type { IncidentePublico, SeguridadZona } from './indice'

export interface IndiceRepositorio {
  zonaDeRuta(rutaId: string): Promise<SeguridadZona | null>
}

export interface IncidenteRepositorio {
  recientes(limite: number): Promise<IncidentePublico[]>
}
