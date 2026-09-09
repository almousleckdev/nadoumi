/**
 * Local hero art for the guide pages (the footer "Guides" section). Bundled
 * from app/assets/pages/ so they are served from our own origin and rendered
 * as a plain <img>, unlike the Unsplash-provider imagery elsewhere.
 *
 * Kept out of ./index.ts on purpose: the site footer imports that module on
 * every page and must not pull these images into its bundle.
 */
import welcomeToChina from '~/assets/pages/welcometochina.jpg'
import howToApply from '~/assets/pages/howtoapply.png'
import scholarshipTypes from '~/assets/pages/scholarship-type.jpg'
import studentVisaGuide from '~/assets/pages/student-visa-guide.png'
import livingInChina from '~/assets/pages/living-in-china.jpg'
import cityGuides from '~/assets/pages/cityguides.png'
import faq from '~/assets/pages/faq.jpeg'

export const GUIDE_HEROES: Record<string, string> = {
  'welcome-to-china': welcomeToChina,
  'how-to-apply': howToApply,
  'scholarship-types': scholarshipTypes,
  'student-visa-guide': studentVisaGuide,
  'living-in-china': livingInChina,
  'city-guides': cityGuides,
  faq,
}
