/** Exportacion sin dependencias: CSV (Excel en español) y un PDF de texto con barras simples. */

export type Celda = string | number | null | undefined

/** CSV con separador ";" y BOM para que Excel en español abra bien acentos y decimales. */
export function aCsv(encabezado: string[], filas: Celda[][]): string {
  const esc = (v: Celda) => {
    const s = v == null ? '' : typeof v === 'number' ? String(v).replace('.', ',') : v
    return /[;"\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
  }
  return '\uFEFF' + [encabezado, ...filas].map((f) => f.map(esc).join(';')).join('\r\n') + '\r\n'
}

export interface BloquePdf {
  titulo?: string
  lineas?: string[]
  /** Barras horizontales: etiqueta y valor. */
  barras?: { etiqueta: string; valor: number }[]
}

/** Texto PDF en WinAnsi: los acentos del español entran directo; lo demas se reemplaza. */
function textoPdf(s: string): string {
  let r = ''
  for (const ch of s) {
    const c = ch.codePointAt(0)!
    const b = c < 256 ? ch : '?'
    r += b === '\\' || b === '(' || b === ')' ? '\\' + b : b
  }
  return r
}

/**
 * Genera un PDF A4 (varias paginas si hace falta) con titulo, bloques de texto y barras.
 * Devuelve los bytes; usar con `new Blob([bytes], { type: 'application/pdf' })`.
 */
export function aPdf(titulo: string, bloques: BloquePdf[]): Uint8Array {
  const ancho = 595
  const alto = 842
  const margen = 50
  const paginas: string[] = []
  let ops: string[] = []
  let y = alto - margen
  const nuevaPagina = () => {
    paginas.push(ops.join('\n'))
    ops = []
    y = alto - margen
  }
  const espacio = (h: number) => {
    if (y - h < margen) nuevaPagina()
  }
  const texto = (s: string, tam: number, negrita = false, x = margen) => {
    ops.push(
      `BT /${negrita ? 'F2' : 'F1'} ${tam} Tf ${x} ${y.toFixed(1)} Td (${textoPdf(s)}) Tj ET`
    )
  }
  ops.push('0.42 0.12 0.64 rg')
  ops.push(`${margen} ${alto - margen - 6} ${ancho - 2 * margen} 3 re f`)
  ops.push('0 0 0 rg')
  y -= 26
  texto(titulo, 18, true)
  y -= 22
  for (const b of bloques) {
    if (b.titulo) {
      espacio(30)
      y -= 8
      texto(b.titulo, 13, true)
      y -= 18
    }
    for (const l of b.lineas ?? []) {
      espacio(14)
      texto(l, 10)
      y -= 14
    }
    if (b.barras?.length) {
      const max = Math.max(1, ...b.barras.map((x) => x.valor))
      const anchoBarra = ancho - 2 * margen - 170
      for (const x of b.barras) {
        espacio(16)
        texto(x.etiqueta.slice(0, 26), 9)
        const w = Math.max(1, (x.valor / max) * anchoBarra)
        ops.push(
          `0.66 0.33 0.97 rg ${margen + 130} ${(y - 2).toFixed(1)} ${w.toFixed(1)} 10 re f 0 0 0 rg`
        )
        texto(String(x.valor), 9, false, margen + 136 + w)
        y -= 16
      }
    }
  }
  paginas.push(ops.join('\n'))

  const objetos: string[] = []
  const agregar = (s: string) => objetos.push(s) // numero = indice + 1
  agregar('<< /Type /Catalog /Pages 2 0 R >>')
  agregar('') // Pages, se completa al final
  agregar('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>')
  agregar('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>')
  const hijos: number[] = []
  paginas.forEach((contenido, i) => {
    const pie = `BT /F1 8 Tf ${margen} 30 Td (${textoPdf(`Gacelas - pagina ${i + 1} de ${paginas.length}`)}) Tj ET`
    const flujo = `${contenido}\n${pie}`
    agregar(`<< /Length ${flujo.length} >>\nstream\n${flujo}\nendstream`)
    const nContenido = objetos.length
    agregar(
      `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${ancho} ${alto}] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents ${nContenido} 0 R >>`
    )
    hijos.push(objetos.length)
  })
  objetos[1] = `<< /Type /Pages /Kids [${hijos.map((h) => `${h} 0 R`).join(' ')}] /Count ${hijos.length} >>`

  let pdf = '%PDF-1.4\n'
  const offsets: number[] = []
  objetos.forEach((o, i) => {
    offsets.push(pdf.length)
    pdf += `${i + 1} 0 obj\n${o}\nendobj\n`
  })
  const xref = pdf.length
  pdf += `xref\n0 ${objetos.length + 1}\n0000000000 65535 f \n`
  pdf += offsets.map((o) => `${String(o).padStart(10, '0')} 00000 n \n`).join('')
  pdf += `trailer\n<< /Size ${objetos.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`
  // Latin-1: un byte por caracter, asi los offsets coinciden.
  const bytes = new Uint8Array(pdf.length)
  for (let i = 0; i < pdf.length; i++) bytes[i] = pdf.charCodeAt(i) & 0xff
  return bytes
}

/** Descarga un archivo generado en el navegador. */
export function descargar(nombre: string, datos: string | Uint8Array, tipo: string) {
  const blob = new Blob([datos as BlobPart], { type: tipo })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = nombre
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
