/**
 * Registry of the public "Guides" section. One entry per page under
 * `/guides/<slug>`. The footer and the guides hub both render this list, so a
 * new guide only needs a data row + a page file.
 *
 * `hero` is an Unsplash path served through the `unsplash` @nuxt/image provider
 * (same set as app/data/imagery.ts).
 */
import type { GuideIconName } from "~/components/guides/icons"

export interface GuideMeta {
  slug: string
  title: string
  summary: string
  icon: GuideIconName
  hero: string
}

export const GUIDES: GuideMeta[] = [
  {
    slug: 'welcome-to-china',
    title: 'Welcome to China',
    summary: 'An orientation to studying in China: the system, the language, the pace of life, and what makes it worth it.',
    icon: 'globe',
    hero: '/photo-1523987355523-c7b5b0dd90a7',
  },
  {
    slug: 'how-to-apply',
    title: 'How to Apply',
    summary: 'The application journey step by step: choosing a programme, documents, deadlines, offers and enrolment.',
    icon: 'book',
    hero: '/photo-1523240795612-9a054b0db644',
  },
  {
    slug: 'scholarship-types',
    title: 'Scholarship Types',
    summary: 'Government, university, provincial and enterprise scholarships explained: what they cover and who they suit.',
    icon: 'award',
    hero: '/photo-1627556704302-624286467c65',
  },
  {
    slug: 'student-visa-guide',
    title: 'Student Visa Guide',
    summary: 'From the JW202/JW201 form and X1/X2 visa to the residence permit you convert it into after arrival.',
    icon: 'passport',
    hero: '/photo-1573497019940-1c28c88b4f3e',
  },
  {
    slug: 'living-in-china',
    title: 'Living in China',
    summary: 'Housing, payments, phones, transport, healthcare and daily life as an international student.',
    icon: 'home',
    hero: '/photo-1523580846011-d3a5bc25702b',
  },
  {
    slug: 'city-guides',
    title: 'City Guides',
    summary: 'Every province of China: its history, culture, language and festivals, with two cities and their leading universities.',
    icon: 'map',
    hero: '/photo-1607013251379-e6eecfffe234',
  },
  {
    slug: 'faq',
    title: 'FAQ',
    summary: 'Straight answers to the questions students ask most before applying.',
    icon: 'chat',
    hero: '/photo-1522202176988-66273c2fd55f',
  },
]

export const guideBySlug = (slug: string): GuideMeta | undefined =>
  GUIDES.find(g => g.slug === slug)
