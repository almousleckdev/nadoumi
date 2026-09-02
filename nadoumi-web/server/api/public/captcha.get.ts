// Passthrough of the backend captcha image endpoint. No credentials attached.
// The explicit `$fetch` generic both documents the shape and keeps `nuxt typecheck`
// from recursing into the typed-route table for the returned value.
export default defineEventHandler(async (event) => {
  return $fetch<{ captchaEnabled: boolean; uuid?: string; img?: string }>(
    `${backendBaseUrl(event)}/captchaImage`,
  )
})
