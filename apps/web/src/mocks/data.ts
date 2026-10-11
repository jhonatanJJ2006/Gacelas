/** Datos de demostracion compartidos con la app Android (data/mock/MockData.kt). */

/** Identificador del usuario local para rutas propias. */
export const ID_USUARIO = '0'

export interface Usuario {
  id: string
  nombre: string
  username: string
}

export interface Actividad {
  id: string
  usuarioId: string
  titulo: string
  distanciaKm: number
  duracionSegundos: number
  ritmo: string
  fecha: string
  likes: number
  comentarios: number
}

export type Dificultad = 'Fácil' | 'Moderado' | 'Difícil'

export interface Ruta {
  id: string
  nombre: string
  distanciaKm: number
  dificultad: Dificultad
  elevacionM: number
  descripcion: string
  autorId: string
  /** Etiquetas (ver ETIQUETAS_RUTA). */
  etiquetas?: string[]
  /** Trazo de la ruta. */
  puntos?: { lat: number; lng: number }[]
  meGusta?: number
  /** Promedio de estrellas (1-5) o null sin reseñas. */
  valoracion?: number | null
  resenas?: number
  autorNombre?: string
  /** La creo la cuenta actual. */
  propia?: boolean
  /** Alturas (m) a lo largo del trazo. */
  perfilElevacion?: number[]
  origen?: 'actividad' | 'dibujada' | 'oficial'
}

export interface Evento {
  id: string
  titulo: string
  descripcion: string
  fecha: string
  hora: string
  ubicacion: string
  participantes: number
  distanciaKm: number
  pasado: boolean
  creadoPorUsuario: boolean
}

export interface RutaSugerida {
  id: string
  nombre: string
  distanciaKm: number
  minutos: number
  elevacionM: number
  principal: boolean
}

export type TipoNotificacion = 'like' | 'comment' | 'follow' | 'event' | 'live' | 'streak'

export interface Notificacion {
  id: string
  tipo: TipoNotificacion
  usuario: string
  mensaje: string
  tiempo: string
  leida: boolean
}

export const usuariosMock: Usuario[] = [
  { id: '1', nombre: 'María López', username: 'maria_runs' },
  { id: '2', nombre: 'Carlos Ruiz', username: 'carlos_runner' },
  { id: '3', nombre: 'Ana Martínez', username: 'ana_trails' },
]

export const usuarioActualMock: Usuario = {
  id: ID_USUARIO,
  nombre: 'Jhonatan Runner',
  username: 'jhonatan_runs',
}

export const actividadesMock: Actividad[] = [
  {
    id: '1',
    usuarioId: '1',
    titulo: 'Carrera matutina',
    distanciaKm: 5.2,
    duracionSegundos: 1714,
    ritmo: '5:30',
    fecha: 'hace 2h',
    likes: 23,
    comentarios: 5,
  },
  {
    id: '2',
    usuarioId: '2',
    titulo: 'Entrenamiento de intervalos',
    distanciaKm: 8.5,
    duracionSegundos: 2720,
    ritmo: '5:20',
    fecha: 'hace 4h',
    likes: 18,
    comentarios: 3,
  },
]

export const rutasMock: Ruta[] = [
  {
    id: '1',
    nombre: 'Parque Jipiro',
    distanciaKm: 5.2,
    dificultad: 'Fácil',
    elevacionM: 45,
    descripcion: 'Ruta panorámica alrededor del Parque Jipiro, ideal para principiantes.',
    autorId: 'gacelas',
    etiquetas: ['parque', 'plana', 'urbana'],
  },
  {
    id: '2',
    nombre: 'Río Zamora',
    distanciaKm: 8.5,
    dificultad: 'Moderado',
    elevacionM: 120,
    descripcion: 'Ruta junto al río con algunas subidas moderadas.',
    autorId: 'gacelas',
  },
  {
    id: '3',
    nombre: 'Cerro Villonaco',
    distanciaKm: 12.3,
    dificultad: 'Difícil',
    elevacionM: 380,
    descripcion: 'Ruta de montaña desafiante con vistas espectaculares de Loja.',
    autorId: 'gacelas',
  },
  {
    id: '4',
    nombre: 'Campus UTPL - San Cayetano',
    distanciaKm: 6.1,
    dificultad: 'Moderado',
    elevacionM: 150,
    descripcion: 'Mi ruta de entrenamiento entre el campus y San Cayetano.',
    autorId: ID_USUARIO,
  },
]

export const eventosMock: Evento[] = [
  {
    id: '1',
    titulo: 'Carrera Parque Jipiro',
    descripcion: 'Carrera grupal de 10km por el Parque Jipiro. Todos los niveles bienvenidos.',
    fecha: 'Sábado 11 de Octubre',
    hora: '6:00 AM',
    ubicacion: 'Parque Jipiro, Loja',
    participantes: 15,
    distanciaKm: 10,
    pasado: false,
    creadoPorUsuario: false,
  },
  {
    id: '2',
    titulo: 'Trail Running Podocarpus',
    descripcion: 'Aventura de trail running en el Parque Nacional Podocarpus.',
    fecha: 'Domingo 19 de Octubre',
    hora: '7:00 AM',
    ubicacion: 'Entrada Cajanuma',
    participantes: 8,
    distanciaKm: 15,
    pasado: false,
    creadoPorUsuario: false,
  },
  {
    id: '3',
    titulo: 'Entrenamiento Intervalos Villonaco',
    descripcion: 'Series en la base del Cerro Villonaco.',
    fecha: 'Miércoles 15 de Octubre',
    hora: '6:30 AM',
    ubicacion: 'Base Cerro Villonaco',
    participantes: 12,
    distanciaKm: 8,
    pasado: false,
    creadoPorUsuario: false,
  },
  {
    id: '4',
    titulo: 'Fondo dominical Zamora',
    descripcion: 'Fondo suave de 12 km junto al río Zamora.',
    fecha: 'Domingo 28 de Septiembre',
    hora: '6:30 AM',
    ubicacion: 'Parque Lineal La Tebaida',
    participantes: 21,
    distanciaKm: 12,
    pasado: true,
    creadoPorUsuario: false,
  },
]

export const rutasSugeridasMock: RutaSugerida[] = [
  {
    id: 'primary',
    nombre: 'Jipiro → Puerta → Zamora Huayco',
    distanciaKm: 8.5,
    minutos: 45,
    elevacionM: 120,
    principal: true,
  },
  {
    id: 'alt',
    nombre: 'Ruta Universitaria Directa',
    distanciaKm: 6.8,
    minutos: 36,
    elevacionM: 85,
    principal: false,
  },
]

export const notificacionesMock: Notificacion[] = [
  {
    id: '1',
    tipo: 'live',
    usuario: 'María López',
    mensaje: 'está corriendo ahora',
    tiempo: 'hace 5 min',
    leida: false,
  },
  {
    id: '2',
    tipo: 'like',
    usuario: 'Carlos Ruiz',
    mensaje: 'le dio like a tu carrera matutina',
    tiempo: 'hace 1h',
    leida: false,
  },
  {
    id: '3',
    tipo: 'comment',
    usuario: 'Ana Martínez',
    mensaje: 'comentó en tu actividad: "Excelente ritmo"',
    tiempo: 'hace 2h',
    leida: true,
  },
  {
    id: '4',
    tipo: 'follow',
    usuario: 'Pedro Sánchez',
    mensaje: 'comenzó a seguirte',
    tiempo: 'hace 3h',
    leida: true,
  },
  {
    id: '5',
    tipo: 'event',
    usuario: 'Gacelas Loja',
    mensaje: 'Nuevo evento: Carrera Parque Jipiro',
    tiempo: 'hace 1d',
    leida: true,
  },
]

export const usuarioPorId = (id: string) => usuariosMock.find((u) => u.id === id)
export const eventoPorId = (id: string) => eventosMock.find((e) => e.id === id)
