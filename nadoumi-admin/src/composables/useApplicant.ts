import { ref } from 'vue'
import {
  getApplicant, listEducation, listTestScores, listContacts,
  type Applicant, type Education, type TestScore, type Contact,
} from '@/api/applicant'

/**
 * Loads one applicant and its sub-resources for the detail screen. The profile
 * loads eagerly; education / test scores / contacts load on first access to
 * their tab (`loadEducation()` etc.), so opening the page is one request.
 */
export function useApplicant(id: number | string) {
  const applicant = ref<Applicant | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const education = ref<Education[] | null>(null)
  const testScores = ref<TestScore[] | null>(null)
  const contacts = ref<Contact[] | null>(null)

  async function load() {
    loading.value = true
    error.value = null
    try {
      applicant.value = await getApplicant(id)
    }
    catch (e) {
      error.value = (e as Error)?.message || 'Could not load the applicant'
    }
    finally {
      loading.value = false
    }
  }

  function once<T>(store: { value: T[] | null }, fetcher: () => Promise<T[]>) {
    return async (force = false) => {
      if (store.value !== null && !force) return
      store.value = await fetcher()
    }
  }

  const loadEducation = once(education, () => listEducation(id))
  const loadTestScores = once(testScores, () => listTestScores(id))
  const loadContacts = once(contacts, () => listContacts(id))

  return {
    applicant, loading, error, load,
    education, testScores, contacts,
    loadEducation, loadTestScores, loadContacts,
  }
}
