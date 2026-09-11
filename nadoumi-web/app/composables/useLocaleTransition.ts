/**
 * Drives the brief, branded overlay shown while the site switches language
 * (LocaleTransitionOverlay.vue, mounted once in app.vue). `active` is shared
 * app-wide via useState so any locale switcher (site header, dashboard shell)
 * can trigger it and the single overlay instance reacts.
 */
export function useLocaleTransition() {
  const active = useState<boolean>('locale-transition-active', () => false)
  const switchLocalePath = useSwitchLocalePath()

  // Long enough that the branded mark actually reads before the page underneath
  // it changes — a sub-300ms round trip just reads as a flicker.
  const FADE_IN_MS = 420
  const HOLD_MS = 620

  function prefersReducedMotion(): boolean {
    if (!import.meta.client) return false
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches
  }

  async function changeLocale(code: string): Promise<void> {
    // switchLocalePath's generated union type tracks nuxt.config's locales list;
    // callers pass whatever code useI18n().locales reports, so widen here rather
    // than duplicate that list.
    const target = switchLocalePath(code as Parameters<typeof switchLocalePath>[0])
    if (!target) return

    if (prefersReducedMotion()) {
      await navigateTo(target)
      return
    }

    active.value = true
    await new Promise(resolve => setTimeout(resolve, FADE_IN_MS))
    await navigateTo(target)
    await nextTick()
    await new Promise(resolve => setTimeout(resolve, HOLD_MS))
    active.value = false
  }

  return { active, changeLocale }
}
