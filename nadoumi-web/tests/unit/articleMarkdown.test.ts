import { describe, it, expect } from 'vitest'
import { renderMarkdown } from '~/composables/useMarkdown'
import { embedFromUrl, splitImageSize } from '~/utils/articleDialect'

describe('article body: images', () => {
  it('renders a lone image as a figure with its caption', () => {
    const html = renderMarkdown('![A shop front](https://cdn.example/a.jpg "A local shop")')
    expect(html).toContain('<figure class="nad-fig nad-fig--inline">')
    expect(html).toContain('src="https://cdn.example/a.jpg"')
    expect(html).toContain('alt="A shop front"')
    expect(html).toContain('<figcaption>A local shop</figcaption>')
    expect(html).not.toContain('<p><figure')
  })

  it.each([
    ['wide', 'nad-fig--wide'],
    ['full', 'nad-fig--full'],
  ])('reads the %s width marker and keeps it out of the image url', (marker, cls) => {
    const html = renderMarkdown(`![](https://cdn.example/a.jpg#${marker})`)
    expect(html).toContain(cls)
    expect(html).toContain('src="https://cdn.example/a.jpg"')
    expect(html).not.toContain(`#${marker}`)
  })

  it('leaves out the caption element when there is no caption', () => {
    expect(renderMarkdown('![](https://cdn.example/a.jpg)')).not.toContain('figcaption')
  })

  it('keeps an image inside a sentence as an ordinary inline image', () => {
    const html = renderMarkdown('See ![icon](https://cdn.example/i.png) here')
    expect(html).toContain('<p>See <img')
    expect(html).not.toContain('<figure')
  })

  it('lazy-loads images', () => {
    expect(renderMarkdown('![](https://cdn.example/a.jpg)')).toContain('loading="lazy"')
  })

  it('escapes alt text and captions', () => {
    const html = renderMarkdown('![x"><script>alert(1)</script>](https://cdn.example/a.jpg "<img src=x onerror=alert(1)>")')
    expect(html).not.toMatch(/<script/i)
    expect(html).not.toMatch(/<img src=x/i)
    expect(html).toContain('&lt;img src=x onerror=alert(1)&gt;')
  })

  it('does not render an image with a script url', () => {
    const html = renderMarkdown('![x](javascript:alert(1))')
    expect(html).not.toContain('<img')
    expect(html).not.toContain('<figure')
  })
})

describe('article body: video embeds', () => {
  it('turns a lone youtube autolink into a sandboxed privacy-friendly player', () => {
    const html = renderMarkdown('<https://www.youtube.com/watch?v=aX5DXP9DJ_c>')
    expect(html).toContain('<div class="nad-embed">')
    expect(html).toContain('src="https://www.youtube-nocookie.com/embed/aX5DXP9DJ_c"')
    expect(html).toContain('sandbox="allow-scripts allow-same-origin allow-presentation allow-popups"')
    expect(html).toContain('loading="lazy"')
    expect(html).not.toContain('<a ')
  })

  it('embeds vimeo and short youtube links', () => {
    expect(renderMarkdown('<https://vimeo.com/76979871>')).toContain('src="https://player.vimeo.com/video/76979871"')
    expect(renderMarkdown('<https://youtu.be/aX5DXP9DJ_c>')).toContain('youtube-nocookie.com/embed/aX5DXP9DJ_c')
  })

  it('keeps a video link inside a sentence as a normal link', () => {
    const html = renderMarkdown('Watch <https://youtu.be/aX5DXP9DJ_c> now')
    expect(html).not.toContain('<iframe')
    expect(html).toContain('<a ')
  })

  it('keeps a link with its own text as a normal link', () => {
    const html = renderMarkdown('[the trailer](https://youtu.be/aX5DXP9DJ_c)')
    expect(html).not.toContain('<iframe')
    expect(html).toContain('the trailer')
  })

  it('never embeds a host that is not on the allow-list', () => {
    const html = renderMarkdown('<https://www.youtube.com.evil.example/watch?v=aX5DXP9DJ_c>')
    expect(html).not.toContain('<iframe')
  })
})

describe('article body: the rest of the dialect', () => {
  it('keeps the code language on fenced blocks', () => {
    expect(renderMarkdown('```java\nint x = 1;\n```')).toContain('class="language-java"')
  })

  it('renders a divider', () => {
    expect(renderMarkdown('a\n\n---\n\nb')).toContain('<hr>')
  })

  it('still escapes raw html', () => {
    const html = renderMarkdown('<script>alert(1)</script>')
    expect(html).not.toMatch(/<script/i)
  })

  it('opens links safely in a new tab', () => {
    const html = renderMarkdown('[x](https://example.com)')
    expect(html).toContain('target="_blank"')
    expect(html).toContain('rel="noopener noreferrer nofollow ugc"')
  })
})

describe('article dialect helpers', () => {
  it('splits the width marker from an image url', () => {
    expect(splitImageSize('https://cdn.example/a.jpg#wide')).toEqual({ src: 'https://cdn.example/a.jpg', size: 'wide' })
    expect(splitImageSize('https://cdn.example/a.jpg')).toEqual({ src: 'https://cdn.example/a.jpg', size: 'inline' })
  })

  it('accepts only https links to youtube and vimeo with a well-formed id', () => {
    expect(embedFromUrl('https://youtu.be/aX5DXP9DJ_c')?.provider).toBe('youtube')
    expect(embedFromUrl('http://youtu.be/aX5DXP9DJ_c')).toBeNull()
    expect(embedFromUrl('https://youtu.be/short')).toBeNull()
    expect(embedFromUrl('https://evil.example/aX5DXP9DJ_c')).toBeNull()
    expect(embedFromUrl('https://user:pw@www.youtube.com/watch?v=aX5DXP9DJ_c')).toBeNull()
  })
})
