/** Every field-validation code the onboarding forms can report, and the copy for it, in one place. */
export const FORM_ERROR_KEYS = {
  required: 'validation.required',
  // profile
  nameInvalid: 'profileForm.nameInvalid',
  dobFuture: 'profileForm.dobFuture',
  dobUnderage: 'profileForm.dobUnderage',
  contactHandleRequired: 'profileForm.contactHandleRequired',
  verifyBeforeSave: 'profileForm.verifyBeforeSave',
  // passport
  numberInvalid: 'passport.errors.numberInvalid',
  issueFuture: 'passport.errors.issueFuture',
  expiryBeforeIssue: 'passport.errors.expiryBeforeIssue',
  expiryTooSoon: 'passport.errors.expiryTooSoon',
  // education, work, location
  startFuture: 'formErrors.startFuture',
  endBeforeStart: 'formErrors.endBeforeStart',
  endFuture: 'formErrors.endFuture',
  endRequired: 'formErrors.endRequired',
  visaExpired: 'formErrors.visaExpired',
  needOne: 'formErrors.needOne',
  emailInvalid: 'validation.email',
} as const

export type FormErrorCode = keyof typeof FORM_ERROR_KEYS

/** Field name to error code, as returned by the pure validators. */
export type FieldErrors = Partial<Record<string, FormErrorCode>>
