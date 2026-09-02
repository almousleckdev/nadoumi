// app/composables/useSession.ts  — replaced in Task 10
export function useSession() {
  return { status: ref<'unknown' | 'guest' | 'authed'>('guest'), user: ref<null | { nickName: string }>(null) }
}
