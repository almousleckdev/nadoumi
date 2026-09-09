/**
 * Registry of the public "Guides" section. One entry per page under
 * `/guides/<slug>`. The footer and the guides hub both render this list, so a
 * new guide only needs a data row + a page file.
 *
 * Hero art for each page is bundled locally and lives in ./heroes.ts, keyed by
 * the same slug (kept separate so the footer bundle stays image-free).
 */
import type { GuideIconName } from "~/components/guides/icons"

export interface GuideMeta {
  slug: string
  title: string
  summary: string
  icon: GuideIconName
}

export const GUIDES: GuideMeta[] = [
  {
    slug: 'welcome-to-china',
    title: 'Welcome to China',
    summary: 'An orientation to studying in China: the system, the language, the pace of life, and what makes it worth it.',
    icon: 'globe',
  },
  {
    slug: 'how-to-apply',
    title: 'How to Apply',
    summary: 'The application journey step by step: choosing a programme, documents, deadlines, offers and enrolment.',
    icon: 'book',
  },
  {
    slug: 'scholarship-types',
    title: 'Scholarship Types',
    summary: 'Government, university, provincial and enterprise scholarships explained: what they cover and who they suit.',
    icon: 'award',
  },
  {
    slug: 'student-visa-guide',
    title: 'Student Visa Guide',
    summary: 'From the JW202/JW201 form and X1/X2 visa to the residence permit you convert it into after arrival.',
    icon: 'passport',
  },
  {
    slug: 'living-in-china',
    title: 'Living in China',
    summary: 'Housing, payments, phones, transport, healthcare and daily life as an international student.',
    icon: 'home',
  },
  {
    slug: 'city-guides',
    title: 'City Guides',
    summary: 'Every province of China: its history, culture, language and festivals, with two cities and their leading universities.',
    icon: 'map',
  },
  {
    slug: 'faq',
    title: 'FAQ',
    summary: 'Straight answers to the questions students ask most before applying.',
    icon: 'chat',
  },
]

export const guideBySlug = (slug: string): GuideMeta | undefined =>
  GUIDES.find(g => g.slug === slug)
