import sharp from 'sharp'
import { readdir } from 'fs/promises'
import { join } from 'path'

async function createOverview() {
  const screensDir = '/opt/cursor/artifacts/screens'
  const files = (await readdir(screensDir)).filter((f) => f.endsWith('.png')).sort()

  console.log(`Creando collage con ${files.length} pantallas...`)

  const cols = 6
  const rows = Math.ceil(files.length / cols)
  const thumbWidth = 195
  const thumbHeight = 422
  const padding = 10

  const canvasWidth = (thumbWidth + padding) * cols
  const canvasHeight = (thumbHeight + padding) * rows + padding

  const bg = await sharp({
    create: {
      width: canvasWidth,
      height: canvasHeight,
      channels: 4,
      background: { r: 12, g: 19, b: 34, alpha: 1 },
    },
  }).png()

  const composites = []

  for (let i = 0; i < files.length; i++) {
    const col = i % cols
    const row = Math.floor(i / cols)
    const x = col * (thumbWidth + padding) + padding
    const y = row * (thumbHeight + padding) + padding

    const resized = await sharp(join(screensDir, files[i]))
      .resize(thumbWidth, thumbHeight, {
        fit: 'contain',
        background: { r: 12, g: 19, b: 34, alpha: 1 },
      })
      .toBuffer()

    composites.push({
      input: resized,
      left: x,
      top: y,
    })
  }

  await bg.composite(composites).toFile('/opt/cursor/artifacts/overview.png')

  console.log('✓ Collage creado: /opt/cursor/artifacts/overview.png')
}

createOverview()
