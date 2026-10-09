import { computed, watch } from 'vue'

/**
 * The signed-in student's profile photo as a short-lived signed URL, shared by the header, the account page and
 * anything else that shows it. The photo is private media, so the URL has to be asked for; `refresh` re-asks after
 * a new photo is uploaded. An applicant with no photo (or a failed lookup) gives an empty string and the avatar
 * falls back to initials.
 */
export function useMyPhoto() {
  const { primary } = useMyApplicant()
  const { photoUrl } = useApplicant()
  const url = useState<string>('nad-my-photo-url', () => '')
  const applicantId = computed(() => primary.value?.id ?? null)

  async function refresh() {
    url.value = applicantId.value === null ? '' : await photoUrl(applicantId.value).then(r => r.url).catch(() => '')
  }

  watch(applicantId, () => { void refresh() }, { immediate: true })
  return { url, refresh }
}
