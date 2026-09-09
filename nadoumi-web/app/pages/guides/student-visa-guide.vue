<script setup lang="ts">
const localePath = useLocalePath()

const documents = [
  { icon: 'book', term: 'Admission Notice', detail: 'The original admission letter from your university.' },
  { icon: 'passport', term: 'JW202 or JW201', detail: 'The Visa Application for Study in China form. JW202 for self-funded or university-scholarship students; JW201 for Chinese Government Scholarship students.' },
] as const

const arrive = [
  { n: 1, title: 'Register your address within 24 hours', body: 'At the local police station. If you live in a university dormitory the university normally does this; if you rent, you must do it yourself, and again after every trip abroad.' },
  { n: 2, title: 'Complete the medical check', body: 'If you did the Foreigner Physical Examination at home, bring the report and X-ray to be verified at the local International Travel Healthcare Centre; otherwise you take the exam in China.' },
  { n: 3, title: 'Apply for the residence permit within 30 days', body: 'At the Exit-Entry Administration Bureau, with your passport and X1 visa, JW202 or JW201, Admission Notice, verified medical report, accommodation registration and photos. The permit replaces the visa and is issued for the academic year, renewable annually.' },
]
</script>

<template>
  <GuideLayout slug="student-visa-guide">
    <p>
      Studying in China for longer than 180 days means an <strong>X1</strong> visa, converted into a
      <strong>residence permit</strong> after you arrive. Shorter programmes use an <strong>X2</strong>
      visa. This is the standard path; always check your nearest Chinese embassy or consulate, as
      details vary by country.
    </p>

    <GuideSection eyebrow="Before you apply" title="The two key documents">
      <GuideIconList :items="documents" />
      <p>The university sends both after you accept your place. Keep the originals safe; the embassy and, later, the immigration office both need them.</p>
    </GuideSection>

    <GuideSection eyebrow="At the embassy" title="Applying for the visa">
      <ol class="list-decimal space-y-2 pl-6 marker:text-slate-400">
        <li>Complete the online visa application form (COVA) and print it.</li>
        <li>Gather: passport valid six months or more with a blank page, a recent photo, the Admission Notice and JW form, and any documents the consulate lists for your nationality.</li>
        <li>Book an appointment at the Chinese Visa Application Service Centre or embassy, submit in person, and pay the fee.</li>
        <li>Collect the visa in about four working days. An X1 is usually single-entry, valid 90 days from issue, so time your travel accordingly.</li>
      </ol>
    </GuideSection>

    <GuideSection eyebrow="After you land" title="Within your first month">
      <ol class="space-y-4">
        <li v-for="s in arrive" :key="s.n" class="flex gap-4 rounded-xl border border-slate-200 bg-white p-4">
          <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-600 text-sm font-bold text-white">{{ s.n }}</span>
          <span>
            <span class="block font-display font-semibold text-slate-900">{{ s.title }}</span>
            <span class="mt-1 block text-sm leading-6 text-slate-600">{{ s.body }}</span>
          </span>
        </li>
      </ol>
    </GuideSection>

    <GuideSection eyebrow="While you study" title="Keep your status valid">
      <ul class="list-disc space-y-2 pl-6 marker:text-slate-400">
        <li>Renew the residence permit at least <strong>one month before it expires</strong>.</li>
        <li>Re-register your address with the police after every re-entry to China.</li>
        <li>Part-time work and internships need prior permission from both the university and immigration.</li>
        <li>If you change university or leave, your permit must be updated or cancelled promptly.</li>
      </ul>
      <GuideCallout title="Before all this" icon="check" tone="brand">
        The offer and JW form come first, read <NuxtLink :to="localePath('/guides/how-to-apply')">How to Apply</NuxtLink>.
      </GuideCallout>
    </GuideSection>
  </GuideLayout>
</template>
