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
