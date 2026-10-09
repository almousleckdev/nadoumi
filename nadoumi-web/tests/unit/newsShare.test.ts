import { describe, it, expect } from 'vitest'
import { shareTargets, isShareableUrl } from '~/utils/share'

const URL_ = 'https://www.nadoumi.com/en/news/studying-in-chengdu'
const TITLE = 'Studying in Chengdu & beyond'

describe('shareTargets', () => {
  it('builds a link for each network with the page url and title encoded', () => {
    const targets = Object.fromEntries(shareTargets(URL_, TITLE).map(t => [t.key, t.href]))

    expect(targets.x).toBe(
      `https://twitter.com/intent/tweet?url=${encodeURIComponent(URL_)}&text=${encodeURIComponent(TITLE)}`)
    expect(targets.facebook).toBe(`https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(URL_)}`)
    expect(targets.linkedin).toBe(`https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(URL_)}`)
    expect(targets.whatsapp).toBe(`https://wa.me/?text=${encodeURIComponent(`${TITLE} ${URL_}`)}`)
  })

  it('never lets the title break out of the query string', () => {
    const [x] = shareTargets(URL_, 'a&url=https://evil.example#')

    expect(new URL(x!.href).searchParams.get('url')).toBe(URL_)
  })

  it('offers only the networks the site supports, in a stable order', () => {
    expect(shareTargets(URL_, TITLE).map(t => t.key)).toEqual(['x', 'facebook', 'linkedin', 'whatsapp'])
  })

  it('returns no targets for a url that is not a web page', () => {
    expect(shareTargets('javascript:alert(1)', TITLE)).toEqual([])
    expect(shareTargets('', TITLE)).toEqual([])
  })
})

describe('isShareableUrl', () => {
  it.each([
    ['https://www.nadoumi.com/en/news/a', true],
    ['http://localhost:3000/en/news/a', true],
    ['javascript:alert(1)', false],
    ['data:text/html,hi', false],
    ['/en/news/a', false],
    ['', false],
  ])('%s -> %s', (url, expected) => {
    expect(isShareableUrl(url)).toBe(expected)
  })
})
