<script setup lang="ts">
const localePath = useLocalePath()

const steps = [
  { n: 1, title: 'Choose programmes', body: 'Decide on level, language of instruction, field and budget. Shortlist three to six so you have options. On Nadoumi you can filter by country, funding, language and level and compare requirements side by side.' },
  { n: 2, title: 'Prepare your documents', body: 'See the checklist below. Notarise and translate anything not already in Chinese or English, and start the medical exam early.' },
  { n: 3, title: 'Submit the application', body: "Through the university's own portal and, for the Chinese Government Scholarship, also the CSC system. Pay the application fee (usually ¥400 to 800). Apply for the scholarship and the admission at the same time; one does not guarantee the other." },
  { n: 4, title: 'Interviews and decisions', body: 'Some departments hold a short online interview. Decisions arrive from April to July: an Admission Notice and, if awarded, a separate Scholarship Certificate.' },
  { n: 5, title: 'Accept and get your visa form', body: 'Confirm your place; the university issues a JW202 (self-funded / university scholarship) or JW201 (government scholarship) with the admission letter. Continue to the Student Visa Guide.' },
  { n: 6, title: 'Before you fly', body: 'Book accommodation, arrange the first months’ funds (stipends are paid after registration), bring every original, and note the registration deadline: missing it can void the offer.' },
]

const docs = [
  { icon: 'passport', term: 'Passport', detail: 'Valid for at least the length of your study, plus six months.' },
  { icon: 'book', term: 'Diploma & transcripts', detail: 'Highest qualification and full records, notarised and translated.' },
  { icon: 'globe', term: 'Language certificate', detail: 'HSK for Chinese-taught, IELTS or TOEFL for English-taught (or an English-medium letter).' },
  { icon: 'check', term: 'Study plan', detail: 'A personal statement, usually 800 to 1,500 words, naming supervisors or research groups.' },
  { icon: 'chat', term: 'Two recommendation letters', detail: 'From academic referees who taught or supervised you.' },
  { icon: 'hospital', term: 'Physical Examination Form', detail: 'The Foreigner Physical Examination Form for programmes over six months, plus a white-background photo.' },
] as const
</script>

<template>
  <GuideLayout slug="how-to-apply">
    <p>
      Applying to a Chinese university is straightforward once your documents are in order. Most
      institutions open applications between <strong>November and March</strong> for a September
      start; scholarship deadlines fall earlier, often <strong>January to April</strong>. Start about
      a year ahead.
    </p>

    <GuideSection eyebrow="The journey" title="Six steps to enrolment">
      <ol class="space-y-4">
        <li v-for="s in steps" :key="s.n" class="flex gap-4 rounded-xl border border-slate-200 bg-white p-4">
          <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-600 text-sm font-bold text-white">{{ s.n }}</span>
          <span>
            <span class="block font-display font-semibold text-slate-900">{{ s.title }}</span>
            <span class="mt-1 block text-sm leading-6 text-slate-600">{{ s.body }}</span>
          </span>
        </li>
      </ol>
    </GuideSection>

    <GuideSection eyebrow="Checklist" title="Documents you will need">
      <GuideIconList :items="docs" />
      <GuideCallout title="Under 18?" icon="shield" tone="amber">
        You also need a notarised guardianship document naming a guardian resident in China.
      </GuideCallout>
    </GuideSection>

    <GuideSection eyebrow="Support" title="How Nadoumi helps">
      <p>
        We track every step, flag deadlines, and review your documents before you submit.
        <NuxtLink :to="localePath('/register')">Create a profile</NuxtLink> to begin, or read the
        <NuxtLink :to="localePath('/guides/student-visa-guide')">Student Visa Guide</NuxtLink> for
        what comes after the offer.
      </p>
    </GuideSection>
  </GuideLayout>
</template>
