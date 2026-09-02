export default defineNuxtPlugin(async () => {
  await useSession().refresh().catch(() => undefined)
})
