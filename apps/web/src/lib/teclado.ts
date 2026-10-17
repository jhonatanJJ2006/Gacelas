/** En pantallas táctiles, tocar fuera de un campo de texto quita el foco y cierra el teclado. */
export function cerrarTecladoAlTocarFuera(doc: Document = document) {
  const alTocar = (e: PointerEvent) => {
    if (e.pointerType !== 'touch') return
    const activo = doc.activeElement as HTMLElement | null
    if (!activo || !activo.matches('input, textarea, [contenteditable="true"]')) return
    const destino = e.target as HTMLElement | null
    if (destino?.closest('input, textarea, select, label, button, [contenteditable="true"]')) return
    activo.blur()
  }
  doc.addEventListener('pointerdown', alTocar, { passive: true })
  return () => doc.removeEventListener('pointerdown', alTocar)
}
