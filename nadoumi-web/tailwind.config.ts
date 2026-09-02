import type { Config } from 'tailwindcss'

export default <Partial<Config>>{
  content: [
    './app/**/*.{vue,ts}',
    './app/components/**/*.{vue,ts}',
    './app/pages/**/*.vue',
    './app/layouts/**/*.vue',
  ],
  theme: {
    extend: {
      colors: {
        // brand === Tailwind orange; aliased so intent reads in markup
        brand: {
          50: '#fff7ed', 100: '#ffedd5', 200: '#fed7aa', 300: '#fdba74',
          400: '#fb923c', 500: '#f97316', 600: '#ea580c', 700: '#c2410c',
          800: '#9a3412', 900: '#7c2d12',
        },
      },
      fontFamily: {
        display: ['"Plus Jakarta Sans"', 'Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        arabic: ['"Noto Sans Arabic"', 'Inter', 'ui-sans-serif', 'sans-serif'],
      },
      borderRadius: { sm: '6px', md: '10px', lg: '16px' },
      boxShadow: {
        xs: '0 1px 2px 0 rgb(15 23 42 / 0.05)',
        sm: '0 1px 3px 0 rgb(15 23 42 / 0.08), 0 1px 2px -1px rgb(15 23 42 / 0.08)',
        md: '0 6px 16px -4px rgb(15 23 42 / 0.10)',
      },
      maxWidth: { marketing: '72rem', app: '64rem' },
    },
  },
}
