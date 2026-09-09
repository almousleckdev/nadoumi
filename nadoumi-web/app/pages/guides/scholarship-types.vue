<script setup lang="ts">
const localePath = useLocalePath()
</script>

<template>
  <GuideLayout slug="scholarship-types">
    <p>
      Funding to study in China comes in four broad layers. Many students combine them, for example
      a provincial award for tuition plus a university award for accommodation. Each scholarship
      listed on Nadoumi shows exactly what it covers and who is eligible.
    </p>

    <h2>1. Chinese Government Scholarship (CSC)</h2>
    <p>
      The national scheme, administered by the China Scholarship Council. It has several channels -
      the bilateral programme (through your home country's ministry), the Chinese-University
      Programme (apply directly to a designated university), and the Silk Road and Great Wall
      programmes. A full award covers <strong>tuition, on-campus accommodation or an
      accommodation allowance, a monthly stipend</strong> (roughly ¥2,500 for a bachelor's, ¥3,000
      for a master's, ¥3,500 for a PhD) and <strong>comprehensive medical insurance</strong>.
      It is competitive and deadlines are early (often January to March).
    </p>

    <h2>2. Provincial and municipal scholarships</h2>
    <p>
      Provinces and large cities, Jiangsu, Zhejiang, Shanghai, Guangdong, Beijing and others, run
      their own schemes to attract students to universities in the region. Coverage ranges from a
      tuition waiver to a near-full package. They are often less competitive than the CSC and worth
      applying for in parallel.
    </p>

    <h2>3. University scholarships</h2>
    <p>
      Almost every university offers its own awards, sometimes called "President's Scholarship" or
      "International Student Scholarship". These can be full or partial, are frequently
      <strong>renewable each year based on results</strong>, and usually have the latest deadlines.
      For many applicants this is the most realistic route to full funding.
    </p>

    <h2>4. Confucius Institute and enterprise scholarships</h2>
    <p>
      The <strong>Confucius Institute Scholarship</strong> funds Chinese-language study and degrees
      in Chinese language teaching, applied through a Confucius Institute or an HSK test centre.
      Separately, companies and foundations sponsor students in fields such as engineering, energy
      and agriculture, often with an internship or employment expectation after graduation.
    </p>

    <hr>

    <h2>What "coverage" means</h2>
    <ul>
      <li><strong>Fully funded</strong>: tuition, accommodation and a monthly stipend, plus insurance.</li>
      <li><strong>Tuition + accommodation</strong>: no stipend; you cover living costs.</li>
      <li><strong>Tuition waiver</strong>: only the tuition; accommodation and living costs are yours.</li>
      <li><strong>Partial</strong>: a fixed percentage or amount off tuition.</li>
    </ul>

    <h2>How to improve your chances</h2>
    <ul>
      <li>Apply a full cycle early and to several scholarships at once.</li>
      <li>Meet or exceed the language requirement before you apply, not after.</li>
      <li>Write a specific study plan that names supervisors and research groups.</li>
      <li>Target universities where your profile is strong rather than only the top names.</li>
    </ul>

    <p>
      Browse open awards on <NuxtLink :to="localePath('/scholarships')">Scholarships</NuxtLink>, or
      <NuxtLink :to="localePath('/register')">create a profile</NuxtLink> and Nadoumi will match you
      to the ones you qualify for.
    </p>
  </GuideLayout>
</template>
