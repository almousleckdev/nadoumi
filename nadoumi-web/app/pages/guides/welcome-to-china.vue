<script setup lang="ts">
const localePath = useLocalePath()

const why = [
  { icon: 'wallet', term: 'Low cost', detail: 'Public-university tuition is a fraction of comparable programmes in Europe or North America, and living costs outside the biggest cities are low.' },
  { icon: 'award', term: 'Real scholarships', detail: 'Government, provincial and university awards can cover tuition, accommodation and a monthly stipend.' },
  { icon: 'book', term: 'Wide choice', detail: 'Engineering, medicine, business, sciences, Chinese language and international relations, many with English-taught tracks.' },
  { icon: 'globe', term: 'A regional network', detail: 'Even a year of Mandarin opens doors across Asia; graduates leave with a genuine network.' },
] as const

const life = [
  { icon: 'qr', term: 'Everything by phone', detail: 'WeChat and Alipay handle messaging, payments, transport and deliveries.' },
  { icon: 'train', term: 'High-speed rail', detail: 'A vast network links every major city, fast and affordable.' },
  { icon: 'home', term: 'Campus living', detail: 'Walled, self-contained campuses with dorms, canteens, clinics and sports on site.' },
  { icon: 'shield', term: 'Safe day to day', detail: 'Low street crime; the international student office helps you settle in.' },
] as const
</script>

<template>
  <GuideLayout slug="welcome-to-china">
    <p>
      China hosts one of the largest international student communities in the world, spread across
      more than a thousand universities from the tropics of Hainan to the grasslands of the north.
      Degrees are taught in Chinese and, increasingly, in English; tuition is modest; and generous
      scholarships are available to strong applicants. This is a short orientation before you dive in.
    </p>

    <GuideSection eyebrow="The case" title="Why study in China">
      <GuideIconList :items="why" />
    </GuideSection>

    <GuideSection eyebrow="Structure" title="How the system works">
      <p>
        A bachelor's degree usually takes four years (five for medicine and architecture); a master's
        two to three; a PhD three to four. The academic year runs from September to July with a long
        winter break around Spring Festival. You will hear universities described as
        <strong>"Double First-Class"</strong>, and the older <strong>"985"</strong> and
        <strong>"211"</strong> labels, which mark the research-intensive institutions.
      </p>
    </GuideSection>

    <GuideSection eyebrow="Language" title="What language will I study in?">
      <p>
        Chinese-taught degrees normally require an <strong>HSK</strong> certificate (HSK 4 for many
        programmes, HSK 5 for humanities and medicine). English-taught degrees require
        <strong>IELTS or TOEFL</strong>, or proof your previous education was in English, plus a
        semester or two of basic Mandarin as a graduation requirement.
      </p>
      <GuideCallout title="Not ready in Chinese?" icon="check" tone="brand">
        A one-year language programme is the standard bridge, and many scholarships include it.
      </GuideCallout>
    </GuideSection>

    <GuideSection eyebrow="Daily life" title="Life in brief">
      <GuideIconList :items="life" />
      <p>For the full practical set-up, read <NuxtLink :to="localePath('/guides/living-in-china')">Living in China</NuxtLink>.</p>
    </GuideSection>

    <GuideSection eyebrow="Next" title="Your next steps">
      <ol class="list-decimal space-y-2 pl-6 marker:text-slate-400">
        <li>Shortlist programmes: start from <NuxtLink :to="localePath('/scholarships')">Scholarships</NuxtLink> and <NuxtLink :to="localePath('/universities')">Universities</NuxtLink>.</li>
        <li>Check the language and academic requirements for each.</li>
        <li>Prepare documents early: see <NuxtLink :to="localePath('/guides/how-to-apply')">How to Apply</NuxtLink>.</li>
        <li>Apply, accept your offer, and get the <strong>JW202/JW201</strong> form for the visa: see the <NuxtLink :to="localePath('/guides/student-visa-guide')">Student Visa Guide</NuxtLink>.</li>
      </ol>
      <p>
        <NuxtLink :to="localePath('/register')">Create a profile</NuxtLink> and Nadoumi will match you
        to programmes, track your deadlines and guide each step to enrolment.
      </p>
    </GuideSection>
  </GuideLayout>
</template>
