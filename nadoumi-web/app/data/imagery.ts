/**
 * Curated imagery for the public site. Served from Unsplash's CDN via the
 * `unsplash` provider in `nuxt.config.ts` (`<NuxtImg src="/photo-…">`).
 *
 * Every id below was checked to resolve (HTTP 200). Alt text is intentionally
 * broad ("students on a university campus") so it stays accurate if a photo is
 * later swapped for a more on-brand shot. Replacing these ids with a
 * hand-curated, licence-cleared set is a design follow-up — the keys stay stable.
 */
export interface CuratedImage {
  /** Unsplash path after the CDN base, e.g. `/photo-1541339907198-e08756dedf3f`. */
  src: string
  alt: string
}

export const imagery = {
  heroPrimary: {
    src: '/photo-1523240795612-9a054b0db644',
    alt: 'International students walking together across a university campus',
  },
  heroSecondary: {
    src: '/photo-1541339907198-e08756dedf3f',
    alt: 'University campus building on a bright day',
  },
  storyCollaboration: {
    src: '/photo-1522202176988-66273c2fd55f',
    alt: 'A group of students working together at a shared table',
  },
  storyStudy: {
    src: '/photo-1517245386807-bb43f82c33c4',
    alt: 'A student studying on a laptop',
  },
  storyLecture: {
    src: '/photo-1607013251379-e6eecfffe234',
    alt: 'Students seated in a university lecture hall',
  },
  storyClassroom: {
    src: '/photo-1509062522246-3755977927d7',
    alt: 'A classroom during a lesson',
  },
  storyOutdoors: {
    src: '/photo-1523580846011-d3a5bc25702b',
    alt: 'Students talking outdoors on campus',
  },
  storyLibrary: {
    src: '/photo-1562774053-701939374585',
    alt: 'A university library reading room',
  },
  ctaClassroom: {
    src: '/photo-1498243691581-b145c3f54a5a',
    alt: 'International students collaborating in a classroom',
  },
  graduation: {
    src: '/photo-1627556704302-624286467c65',
    alt: 'Graduates in caps and gowns at a commencement ceremony',
  },
  campusLife: {
    src: '/photo-1523987355523-c7b5b0dd90a7',
    alt: 'Students crossing a university quad',
  },
  advising: {
    src: '/photo-1573497019940-1c28c88b4f3e',
    alt: 'A student meeting with an adviser',
  },
} as const satisfies Record<string, CuratedImage>

export type ImageryKey = keyof typeof imagery
