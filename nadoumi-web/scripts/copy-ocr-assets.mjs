// Copies the OCR engine files the passport reader needs into public/vendor/ocr so they are
// served from our own origin. Tesseract.js otherwise fetches its worker and core from a public
// CDN; self-hosting keeps every passport-related request on our origin. Run after install.
import { copyFileSync, existsSync, mkdirSync, readdirSync } from 'node:fs'
import { createRequire } from 'node:module'
import { dirname, join } from 'node:path'

const require = createRequire(import.meta.url)
const target = join(process.cwd(), 'public', 'vendor', 'ocr')

const tesseractDir = dirname(require.resolve('tesseract.js/package.json'))
const coreDir = dirname(createRequire(join(tesseractDir, 'package.json')).resolve('tesseract.js-core/package.json'))
const langDir = join(dirname(require.resolve('@tesseract.js-data/eng/package.json')), '4.0.0_best_int')

// LSTM-only core variants: the reader uses the LSTM engine; the browser picks the fastest it supports.
const coreFiles = readdirSync(coreDir).filter(f => /^tesseract-core(-relaxedsimd|-simd)?-lstm\.wasm\.js$/.test(f))
const files = [
  [join(tesseractDir, 'dist', 'worker.min.js'), 'worker.min.js'],
  [join(langDir, 'eng.traineddata.gz'), 'eng.traineddata.gz'],
  ...coreFiles.map(f => [join(coreDir, f), f]),
]

mkdirSync(target, { recursive: true })
for (const [from, name] of files) {
  if (!existsSync(from)) throw new Error(`OCR asset missing: ${from}`)
  copyFileSync(from, join(target, name))
}
console.log(`OCR assets copied to public/vendor/ocr (${files.length} files)`)
