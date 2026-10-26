import type { Ruta } from '../../../mocks/data'

export interface RutasRepository {
  guardadas(): Promise<string[]>
  alternarGuardada(id: string): Promise<string[]>
}

export interface ResenaRuta {
  id: string
  usuarioId: string
  nombre: string
  estrellas: number
  comentario: string
  fecha: string
}

export interface NuevaRuta {
  nombre: string
  descripcion: string
  dificultad: Ruta['dificultad']
  etiquetas: string[]
  puntos: { lat: number; lng: number }[]
  elevacionM: number
  actividadId?: string
  perfilElevacion?: number[]
  origen?: 'actividad' | 'dibujada'
}

export interface FotoRuta {
  id: string
  url: string
  usuarioId: string
}

export interface CorredorRuta {
  usuarioId: string
  nombre: string
  usuario: string
  veces: number
  ultima: string
}

export interface ExplorarRepository {
  listar(): Promise<Ruta[]>
  /** id de ruta -> km desde el punto (rutas a menos de 50 km). */
  cercanas(lat: number, lng: number): Promise<Record<string, number>>
  crear(r: NuevaRuta): Promise<Ruta>
  meGusta(): Promise<string[]>
  alternarMeGusta(id: string, activo: boolean): Promise<void>
  resenas(id: string): Promise<ResenaRuta[]>
  resenar(id: string, estrellas: number, comentario: string): Promise<void>
  fotos(id: string): Promise<FotoRuta[]>
  subirFoto(id: string, archivo: Blob): Promise<void>
  /** Corredores visibles y total de personas que la han corrido. */
  corredores(id: string): Promise<{ lista: CorredorRuta[]; total: number }>
  registrarCorrida(rutaId: string, actividadId: string): Promise<void>
}
