<script setup lang="ts">
const localePath = useLocalePath()

const coverage = [
  { icon: 'award', term: 'Fully funded', detail: 'Tuition, accommodation and a monthly stipend, plus comprehensive insurance.' },
  { icon: 'home', term: 'Tuition + accommodation', detail: 'No stipend; you cover living costs.' },
  { icon: 'book', term: 'Tuition waiver', detail: 'Only the tuition; accommodation and living costs are yours.' },
  { icon: 'wallet', term: 'Partial', detail: 'A fixed percentage or amount off tuition.' },
] as const
</script>

<template>
  <GuideLayout slug="scholarship-types">
    <p>
      Funding to study in China comes in four broad layers. Many students combine them, for example a
      provincial award for tuition plus a university award for accommodation. Every scholarship listed
      on Nadoumi shows exactly what it covers and who is eligible.
    </p>

    <GuideSection eyebrow="1" title="Chinese Government Scholarship (CSC)">
      <p>
        The national scheme, run by the China Scholarship Council, with several channels: the
        bilateral programme (through your home country's ministry), the Chinese-University Programme
        (apply directly to a designated university), and the Silk Road and Great Wall programmes. A
        full award covers <strong>tuition, on-campus accommodation or an allowance, a monthly
        stipend</strong> (roughly ¥2,500 for a bachelor's, ¥3,000 for a master's, ¥3,500 for a PhD)
        and <strong>comprehensive medical insurance</strong>. It is competitive, with early deadlines
        (often January to March).
      </p>
    </GuideSection>

    <GuideSection eyebrow="2" title="Provincial and municipal scholarships">
      <p>
        Provinces and large cities (Jiangsu, Zhejiang, Shanghai, Guangdong, Beijing and others) run
        their own schemes to attract students to universities in the region. Coverage ranges from a
        tuition waiver to a near-full package, and they are often less competitive than the CSC.
      </p>
    </GuideSection>

    <GuideSection eyebrow="3" title="University scholarships">
      <p>
        Almost every university offers its own awards. These can be full or partial, are frequently
        <strong>renewable each year based on results</strong>, and usually have the latest deadlines.
        For many applicants this is the most realistic route to full funding.
      </p>
    </GuideSection>

    <GuideSection eyebrow="4" title="Confucius Institute and enterprise scholarships">
      <p>
        The <strong>Confucius Institute Scholarship</strong> funds Chinese-language study and degrees
        in Chinese-language teaching, applied through a Confucius Institute or HSK test centre.
        Companies and foundations also sponsor students in fields such as engineering, energy and
        agriculture, often with an internship or job expectation after graduation.
      </p>
    </GuideSection>

    <GuideSection eyebrow="Reference" title="What coverage means">
      <GuideIconList :items="coverage" />
    </GuideSection>

    <GuideSection eyebrow="Improve your odds" title="How to strengthen an application">
      <ul class="list-disc space-y-2 pl-6 marker:text-slate-400">
        <li>Apply a full cycle early and to several scholarships at once.</li>
        <li>Meet or exceed the language requirement <strong>before</strong> you apply.</li>
        <li>Write a specific study plan that names supervisors and research groups.</li>
        <li>Target universities where your profile is strong, not only the top names.</li>
      </ul>
    </GuideSection>

    <PopularScholarships />

    <GuideSection eyebrow="Next" title="Find what you qualify for">
      <p>
        Browse open awards on <NuxtLink :to="localePath('/scholarships')">Scholarships</NuxtLink>, or
        <NuxtLink :to="localePath('/register')">create a profile</NuxtLink> and Nadoumi will match you
        to the ones you are eligible for.
      </p>
    </GuideSection>
  </GuideLayout>
</template>
