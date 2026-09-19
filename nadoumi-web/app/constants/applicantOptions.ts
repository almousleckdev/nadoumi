/**
 * Code lists for every applicant choice field. The codes are what the API stores (they mirror the
 * backend enums); the labels are `options.<group>.<code>` in the locale files, so no copy lives here.
 */
export const OPTION_GROUPS = {
  educationLevel: ['HIGH_SCHOOL', 'FOUNDATION', 'DIPLOMA', 'BACHELOR', 'MASTER', 'DOCTORATE', 'OTHER'],
  studyLevel: ['LANGUAGE', 'FOUNDATION', 'BACHELOR', 'MASTER', 'DOCTORATE'],
  scholarshipInterest: ['REQUIRED', 'PREFERRED', 'NOT_NEEDED'],
  intakeTerm: ['SPRING', 'SUMMER', 'FALL', 'WINTER'],
  chinaVisaType: ['X1', 'X2', 'Z', 'F', 'L', 'S1', 'S2', 'OTHER'],
  employmentType: ['FULL_TIME', 'PART_TIME', 'INTERNSHIP', 'FREELANCE'],
  contactRelation: ['GUARDIAN', 'EMERGENCY', 'OTHER'],
  fieldOfStudy: [
    'BUSINESS', 'ECONOMICS', 'LAW', 'ENGINEERING', 'COMPUTER_SCIENCE', 'MEDICINE', 'NURSING', 'SCIENCE',
    'EDUCATION', 'ARTS_DESIGN', 'LANGUAGES', 'SOCIAL_SCIENCES', 'AGRICULTURE', 'ARCHITECTURE', 'TOURISM',
  ],
  chinaCity: [
    'Beijing', 'Shanghai', 'Guangzhou', 'Shenzhen', 'Hangzhou', 'Nanjing', 'Wuhan', 'Chengdu', 'Chongqing',
    'Xian', 'Tianjin', 'Suzhou', 'Qingdao', 'Dalian', 'Xiamen', 'Changsha', 'Zhengzhou', 'Harbin', 'Jinan',
    'Hefei', 'Kunming', 'Shenyang', 'Ningbo', 'Fuzhou',
  ],
} as const

export type OptionGroup = keyof typeof OPTION_GROUPS

/** ISO code of China, the only country with the extra location and work-visa questions. */
export const CHINA_COUNTRY = 'CN'
export const MAX_INTEREST_FIELDS = 12
export const MAX_INTEREST_CITIES = 10
export const INTAKE_YEAR_SPAN = 4
export const CONTACT_RELATIONS_REACHABLE = ['GUARDIAN', 'EMERGENCY'] as const
