#!/usr/bin/env node

/**
 * Script para verificar que los archivos Markdown no contengan emojis.
 * Parte de las reglas de documentación de Gacelas.
 */

const fs = require('fs')
const path = require('path')
const { execSync } = require('child_process')

// Patrón para detectar emojis y símbolos Unicode decorativos
// Incluye rangos comunes de emojis Unicode
const emojiPattern =
  /[\u{1F300}-\u{1F9FF}\u{2600}-\u{26FF}\u{2700}-\u{27BF}\u{231A}-\u{23FF}\u{2190}-\u{21FF}\u{FE00}-\u{FE0F}\u{200D}]/gu

// Obtener todos los archivos .md excluyendo node_modules
let mdFiles = []
try {
  const output = execSync('find . -name "*.md" -type f | grep -v node_modules', {
    encoding: 'utf-8',
  })
  mdFiles = output.trim().split('\n').filter(Boolean)
} catch (error) {
  console.error('Error al buscar archivos markdown')
  process.exit(1)
}

let hasErrors = false
const errors = []

mdFiles.forEach((file) => {
  const content = fs.readFileSync(file, 'utf-8')
  const lines = content.split('\n')

  lines.forEach((line, index) => {
    const match = line.match(emojiPattern)
    if (match) {
      hasErrors = true
      errors.push({
        file,
        line: index + 1,
        content: line.trim(),
        emoji: match[0],
      })
    }
  })
})

if (hasErrors) {
  console.error('\n❌ Error: Se encontraron emojis en la documentación\n')
  console.error('Los archivos de documentación no deben contener emojis.')
  console.error('Ver AGENTS.md y docs/CONTRIBUTING.md para más información.\n')

  errors.forEach(({ file, line, content, emoji }) => {
    console.error(`${file}:${line}`)
    console.error(`  Emoji encontrado: "${emoji}"`)
    console.error(`  Línea: ${content.substring(0, 100)}${content.length > 100 ? '...' : ''}`)
    console.error()
  })

  console.error(`Total de problemas encontrados: ${errors.length}\n`)
  process.exit(1)
}

console.log('✓ Verificación de emojis pasó: No se encontraron emojis en la documentación')
process.exit(0)
