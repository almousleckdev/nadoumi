<script setup lang="ts">
const localePath = useLocalePath()

const faqs = [
  {
    q: 'Do I need to speak Chinese to study in China?',
    a: 'Not necessarily. Many bachelor\'s and most master\'s and PhD programmes have English-taught tracks, especially in engineering, business, medicine and the sciences. Chinese-taught degrees require an HSK certificate (usually HSK 4 to 5). Either way, a semester or two of basic Mandarin is a common graduation requirement, and a one-year language programme is the standard bridge if your Chinese is not yet ready.',
  },
  {
    q: 'How much does it cost?',
    a: 'Public-university tuition is typically ¥18,000 to 40,000 a year for a bachelor\'s (medicine and MBA programmes are higher). On-campus accommodation is around ¥1,000 to 2,500 a month, and a modest living budget outside the largest cities is ¥2,000 to 3,500 a month. A full scholarship removes tuition and accommodation and adds a monthly stipend.',
  },
  {
    q: 'Can I get a full scholarship?',
    a: 'Yes, through the Chinese Government Scholarship, provincial schemes, university awards or the Confucius Institute Scholarship. Apply a full cycle early, to several at once, and meet the language requirement before applying. University scholarships have the latest deadlines and are the most realistic route to full funding for many applicants.',
  },
  {
    q: 'When should I apply?',
    a: 'About a year ahead. Applications generally open November to March for a September start; scholarship deadlines fall earlier, often January to April. Decisions arrive April to July.',
  },
  {
    q: 'What documents do I need?',
    a: 'Passport; notarised and translated diploma and transcripts; a language certificate (HSK or IELTS/TOEFL); a study plan; two recommendation letters; a passport photo; and, for programmes over six months, the Foreigner Physical Examination Form. Applicants under 18 also need a notarised guardianship document.',
  },
  {
    q: 'What visa do I need?',
    a: 'An X1 visa for programmes longer than 180 days, which you convert into a residence permit within 30 days of arriving; an X2 visa for shorter programmes. You apply with your original Admission Notice and the JW202 or JW201 form from the university.',
  },
  {
    q: 'Is it safe, and what is campus life like?',
    a: 'Chinese campuses are walled, self-contained and generally very safe, with dormitories, canteens, clinics and sports facilities on site. Daily life runs through WeChat and Alipay; the international student office and student associations are the fastest way to build a network.',
  },
  {
    q: 'Can I work while studying?',
    a: 'Only with prior permission from both the university and the immigration authorities. Working on a study permit without approval can jeopardise your status. Paid internships tied to your programme are usually possible with the right paperwork.',
  },
  {
    q: 'Can I bring my family?',
    a: 'Spouses and children can apply for an S1/S2 dependant visa once you hold a residence permit, but you must show you can support them; student dormitories do not house families, so you would rent off campus. Policies vary by city, check with the university\'s international office.',
  },
  {
    q: 'What can I do after graduating?',
    a: 'Options include further study in China, a work permit if you find a qualifying job (generally requiring relevant experience or a master\'s), or returning home with a recognised degree and regional network. Some cities run post-graduation job-search or entrepreneurship visas for graduates of top universities.',
  },
  {
    q: 'How does Nadoumi help?',
    a: 'We match you to programmes and scholarships you qualify for, track every deadline, review your documents before submission, and guide you through offers, the JW form, the visa and enrolment.',
  },
]
</script>

<template>
  <GuideLayout slug="faq">
    <p>
      Short answers to the questions students ask most before applying. For detail, follow the links
      to the full guides.
    </p>

    <div class="mt-6 space-y-3">
      <details
        v-for="(f, i) in faqs"
        :key="i"
        class="group rounded-xl border border-slate-200 bg-white p-4 open:shadow-sm"
      >
        <summary class="cursor-pointer list-none font-semibold text-slate-900 marker:content-none">
          <span class="mr-2 text-brand-600 group-open:hidden">+</span>
          <span class="mr-2 hidden text-brand-600 group-open:inline">-</span>
          {{ f.q }}
        </summary>
        <p class="mt-3 leading-7 text-slate-600">{{ f.a }}</p>
      </details>
    </div>

    <p class="mt-8">
      Still unsure? <NuxtLink :to="localePath('/contact')">Contact us</NuxtLink> or
      <NuxtLink :to="localePath('/register')">create a profile</NuxtLink> and we will guide you.
    </p>
  </GuideLayout>
</template>
