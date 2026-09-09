/**
 * Line-icon path data for the guides (Lucide-style, 24x24, stroke).
 * Kept as a plain module so both `GuideIcon.vue` and typed data files can
 * reference the icon-name union without importing a `.vue` file.
 */
export const ICON_PATHS = {
  train: 'M4 15.5V6a3 3 0 0 1 3-3h10a3 3 0 0 1 3 3v9.5a2.5 2.5 0 0 1-2.5 2.5h-11A2.5 2.5 0 0 1 4 15.5ZM4 11h16M8 3v8M16 3v8M8.5 18l-2 3M15.5 18l2 3M8 15h.01M16 15h.01',
  metro: 'M8 3h8a4 4 0 0 1 4 4v7a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V7a4 4 0 0 1 4-4ZM4 10h16M9 17l-2 4M15 17l2 4M8.5 13.5h.01M15.5 13.5h.01',
  bus: 'M8 6v6M15 6v6M2 12h19.6M18 18h.01M14 18H10M6 18h.01M6 3h12a3 3 0 0 1 3 3v10a2 2 0 0 1-2 2h-1a2 2 0 0 1-4 0h-2a2 2 0 0 1-4 0H4a2 2 0 0 1-2-2V6a3 3 0 0 1 3-3Z',
  bike: 'M18.5 20a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7ZM5.5 20a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7ZM15 6a1 1 0 1 0 0-2 1 1 0 0 0 0 2ZM12 17.5V14l-3-3 4-3 2 3h2',
  plane: 'M17.8 19.8 16 14l-6-2-4 4-2-1 3-4-6-2 2-2 8 1 4-4a2 2 0 0 1 3 3l-4 4 1 8-2 2Z',
  wallet: 'M19 7V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-2M21 12a2 2 0 0 0-2-2h-4a2 2 0 0 0 0 4h4a2 2 0 0 0 2-2Z',
  qr: 'M4 4h6v6H4V4ZM14 4h6v6h-6V4ZM4 14h6v6H4v-6ZM14 14h2v2h-2v-2ZM18 14h2v2h-2v-2ZM14 18h2v2h-2v-2ZM18 18h2v2h-2v-2Z',
  chat: 'M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5Z',
  phone: 'M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2ZM11 18h2',
  wifi: 'M5 12.55a11 11 0 0 1 14 0M8.5 16.11a6 6 0 0 1 7 0M2 8.82a15 15 0 0 1 20 0M12 20h.01',
  food: 'M3 2v7c0 1.1.9 2 2 2h1M6 2v9M9 2v9M5 11v11M15 2a4 4 0 0 0-4 4v6h4V2ZM19 2v20',
  bag: 'M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4H6ZM3 6h18M16 10a4 4 0 0 1-8 0',
  hospital: 'M12 6v4M8 8h8M8 22v-5h8v5M4 22V6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v16H4Z',
  home: 'M3 9.5 12 3l9 6.5M5 10v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V10M9 21v-6h6v6',
  passport: 'M6 2h12a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1ZM12 12a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM8 17h8',
  calendar: 'M8 2v4M16 2v4M3 8h18M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z',
  shield: 'M12 2 4 5v6c0 5 3.4 9.4 8 11 4.6-1.6 8-6 8-11V5l-8-3Z',
  book: 'M4 19.5A2.5 2.5 0 0 1 6.5 17H20M4 19.5A2.5 2.5 0 0 0 6.5 22H20V2H6.5A2.5 2.5 0 0 0 4 4.5v15Z',
  award: 'M12 15a6 6 0 1 0 0-12 6 6 0 0 0 0 12ZM8.2 13.5 7 22l5-3 5 3-1.2-8.5',
  globe: 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20ZM2 12h20M12 2a15 15 0 0 1 0 20 15 15 0 0 1 0-20Z',
  map: 'M9 3 3 5v16l6-2 6 2 6-2V3l-6 2-6-2ZM9 3v16M15 5v16',
  check: 'M20 6 9 17l-5-5',
  search: 'M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16ZM21 21l-4.3-4.3',
} as const

export type GuideIconName = keyof typeof ICON_PATHS
