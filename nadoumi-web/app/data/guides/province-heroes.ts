/**
 * Local hero photos for the City Guides province cards. Drop a file into
 * app/assets/provinces/ named after the province slug (lower-case, letters
 * only) and it is picked up automatically, e.g. `beijing.jpg`,
 * `innermongolia.webp`. Provinces without a file fall back to the designed
 * graphic card.
 */
const modules = import.meta.glob('../../assets/provinces/*.{jpg,jpeg,png,webp,avif}', {
  eager: true,
  import: 'default',
}) as Record<string, string>

export const PROVINCE_HEROES: Record<string, string> = Object.fromEntries(
  Object.entries(modules).map(([path, url]) => {
    const slug = path.split('/').pop()!.replace(/\.[^.]+$/, '')
    return [slug, url]
  }),
)

/** Province display name -> lookup slug (lower-case, letters only). */
export const provinceSlug = (name: string): string => name.toLowerCase().replace(/[^a-z]/g, '')

export const provinceHero = (name: string): string | undefined => PROVINCE_HEROES[provinceSlug(name)]
