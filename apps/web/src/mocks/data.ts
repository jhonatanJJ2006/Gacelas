export interface Usuario {
  id: string
  nombre: string
  username: string
  avatar?: string
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

export interface Ruta {
  id: string
  nombre: string
  distanciaKm: number
  dificultad: 'Fácil' | 'Moderado' | 'Difícil'
  elevacionM: number
  descripcion: string
}

export interface Evento {
  id: string
  titulo: string
  descripcion: string
  fecha: string
  hora: string
  ubicacion: string
  participantes: number
}

export const usuariosMock: Usuario[] = [
  { id: '1', nombre: 'María López', username: 'maria_runs' },
  { id: '2', nombre: 'Carlos Ruiz', username: 'carlos_runner' },
  { id: '3', nombre: 'Ana Martínez', username: 'ana_trails' },
]

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
  },
  {
    id: '2',
    nombre: 'Río Zamora',
    distanciaKm: 8.5,
    dificultad: 'Moderado',
    elevacionM: 120,
    descripcion: 'Ruta junto al río con algunas subidas moderadas.',
  },
  {
    id: '3',
    nombre: 'Cerro Villonaco',
    distanciaKm: 12.3,
    dificultad: 'Difícil',
    elevacionM: 380,
    descripcion: 'Ruta de montaña desafiante con vistas espectaculares de Loja.',
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
  },
  {
    id: '2',
    titulo: 'Trail Running Podocarpus',
    descripcion: 'Aventura de trail running en el Parque Nacional Podocarpus.',
    fecha: 'Domingo 19 de Octubre',
    hora: '7:00 AM',
    ubicacion: 'Entrada Cajanuma',
    participantes: 8,
  },
]
