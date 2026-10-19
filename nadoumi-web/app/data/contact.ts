/**
 * Single source of truth for Nadoumi's public contact details.
 * Referenced by the About page, the Contact page and the site footer so the
 * address / hours / emails / phones are never duplicated.
 */
export const CONTACT = {
  /** Registered office, as printed locally. */
  officeCn: '四川省绵阳市涪城区涪西路2号九洲北郡6栋1单元8层02号',
  /** Same address, romanised for international readers. */
  officeEn:
    'Room 02, Floor 8, Unit 1, Building 6, Jiuzhou Beijun, No. 2 Fuxi Road, '
    + 'Fucheng District, Mianyang, Sichuan, China',
  city: 'Mianyang, Sichuan · China',
  hours: 'Monday to Friday, 09:00 to 18:00 (China Standard Time, UTC+8)',
  emails: [
    'support@nadoumi.com',
    'team@nadoumiconsulting.com',
    'nadoumiedu@gmail.com',
  ],
  phones: ['+86 159 0823 7607', '+86 155 2057 6024'],
} as const

/** `tel:` href, strips spaces. */
export const telHref = (phone: string): string => `tel:${phone.replace(/\s+/g, '')}`

/** Number behind the WhatsApp chat links (footer + floating button). */
export const WHATSAPP_PHONE = '+86 155 2057 6024'

/** `wa.me` needs digits only, no `+` or spaces. */
export const whatsappHref = (phone: string = WHATSAPP_PHONE): string =>
  `https://wa.me/${phone.replace(/\D/g, '')}`

/** Social profiles shown in the footer. */
export const SOCIAL = [
  { id: 'facebook', label: 'Facebook', href: 'https://www.facebook.com/profile.php?id=61583472043300' },
  { id: 'instagram', label: 'Instagram', href: 'https://www.instagram.com/nadoumi_consulting?srtk=NWtwYXI4cmIweDc3' },
  { id: 'tiktok', label: 'TikTok', href: 'https://www.tiktok.com/@nadoumi.consultin?_r=1&_t=ZS-9AgSPhAMKak' },
] as const
