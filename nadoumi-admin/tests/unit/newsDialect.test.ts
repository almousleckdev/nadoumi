import { describe, it, expect } from 'vitest'
import {
  embedFromUrl, imageSrcWithSize, normalizeLink, splitImageSize, wordCount,
} from '@/views/news/editor/dialect'

describe('splitImageSize', () => {
  it('reads the wide marker and strips it from the url', () => {
    expect(splitImageSize('https://cdn.x/a.jpg#wide')).toEqual({ src: 'https://cdn.x/a.jpg', size: 'wide' })
  })

  it('reads the full marker', () => {
    expect(splitImageSize('https://cdn.x/a.jpg#full')).toEqual({ src: 'https://cdn.x/a.jpg', size: 'full' })
  })

  it('defaults to inline when there is no marker', () => {
    expect(splitImageSize('https://cdn.x/a.jpg')).toEqual({ src: 'https://cdn.x/a.jpg', size: 'inline' })
  })

  it('leaves an unrelated fragment alone', () => {
    expect(splitImageSize('https://cdn.x/a.jpg#section')).toEqual({ src: 'https://cdn.x/a.jpg#section', size: 'inline' })
  })
})

describe('imageSrcWithSize', () => {
  it('appends the marker for wide and full only', () => {
    expect(imageSrcWithSize('https://cdn.x/a.jpg', 'wide')).toBe('https://cdn.x/a.jpg#wide')
    expect(imageSrcWithSize('https://cdn.x/a.jpg', 'full')).toBe('https://cdn.x/a.jpg#full')
    expect(imageSrcWithSize('https://cdn.x/a.jpg', 'inline')).toBe('https://cdn.x/a.jpg')
  })

  it('replaces a previous marker instead of stacking them', () => {
    expect(imageSrcWithSize('https://cdn.x/a.jpg#wide', 'full')).toBe('https://cdn.x/a.jpg#full')
    expect(imageSrcWithSize('https://cdn.x/a.jpg#wide', 'inline')).toBe('https://cdn.x/a.jpg')
  })
})

describe('embedFromUrl', () => {
  it('accepts the common youtube url shapes and normalises them', () => {
    const expected = {
      provider: 'youtube',
      url: 'https://www.youtube.com/watch?v=aX5DXP9DJ_c',
      src: 'https://www.youtube-nocookie.com/embed/aX5DXP9DJ_c',
    }
    expect(embedFromUrl('https://www.youtube.com/watch?v=aX5DXP9DJ_c')).toEqual(expected)
    expect(embedFromUrl('https://youtu.be/aX5DXP9DJ_c')).toEqual(expected)
    expect(embedFromUrl('https://m.youtube.com/watch?v=aX5DXP9DJ_c&t=30s')).toEqual(expected)
    expect(embedFromUrl('https://www.youtube.com/embed/aX5DXP9DJ_c')).toEqual(expected)
  })

  it('accepts vimeo urls', () => {
    const expected = { provider: 'vimeo', url: 'https://vimeo.com/76979871', src: 'https://player.vimeo.com/video/76979871' }
    expect(embedFromUrl('https://vimeo.com/76979871')).toEqual(expected)
    expect(embedFromUrl('https://player.vimeo.com/video/76979871')).toEqual(expected)
  })

  it('trims surrounding whitespace', () => {
    expect(embedFromUrl('  https://youtu.be/aX5DXP9DJ_c \n')?.provider).toBe('youtube')
  })

  it.each([
    ['plain http', 'http://www.youtube.com/watch?v=aX5DXP9DJ_c'],
    ['unknown host', 'https://evil.example/watch?v=aX5DXP9DJ_c'],
    ['lookalike host', 'https://www.youtube.com.evil.example/watch?v=aX5DXP9DJ_c'],
    ['credentials in url', 'https://user:pw@www.youtube.com/watch?v=aX5DXP9DJ_c'],
    ['bad video id', 'https://www.youtube.com/watch?v=short'],
    ['id with injection', 'https://www.youtube.com/watch?v="onload=alert(1)'],
    ['javascript scheme', 'javascript:alert(1)'],
    ['not a url', 'hello world'],
    ['non numeric vimeo id', 'https://vimeo.com/abcdef'],
    ['empty', ''],
  ])('rejects %s', (_label, url) => {
    expect(embedFromUrl(url)).toBeNull()
  })
})

describe('wordCount', () => {
  it('counts words across scripts and ignores extra whitespace', () => {
    expect(wordCount('  Tokyo is   a city.\n\nIt rocks ')).toBe(6)
    expect(wordCount('')).toBe(0)
    expect(wordCount('   ')).toBe(0)
  })
})

describe('normalizeLink', () => {
  it('keeps http, https and mailto links', () => {
    expect(normalizeLink('https://example.com/a?b=1')).toBe('https://example.com/a?b=1')
    expect(normalizeLink('http://example.com')).toBe('http://example.com')
    expect(normalizeLink('mailto:hi@example.com')).toBe('mailto:hi@example.com')
  })

  it('adds https to a bare domain and mailto to a bare address', () => {
    expect(normalizeLink('example.com/path')).toBe('https://example.com/path')
    expect(normalizeLink('www.example.com')).toBe('https://www.example.com')
    expect(normalizeLink('hi@example.com')).toBe('mailto:hi@example.com')
  })

  it('trims whitespace', () => {
    expect(normalizeLink('  https://example.com \n')).toBe('https://example.com')
  })

  it.each([
    ['javascript scheme', 'javascript:alert(1)'],
    ['data scheme', 'data:text/html;base64,AAAA'],
    ['relative path', '/news/1'],
    ['plain words', 'not a link'],
    ['empty', '   '],
  ])('rejects %s', (_label, input) => {
    expect(normalizeLink(input)).toBeNull()
  })
})
