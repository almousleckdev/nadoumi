<script setup lang="ts">
const localePath = useLocalePath()
</script>

<template>
  <GuideLayout slug="welcome-to-china">
    <p>
      China hosts one of the largest international student communities in the world, spread across
      more than a thousand universities from the tropics of Hainan to the grasslands of the north-east.
      Degrees are taught in Chinese and, increasingly, in English; tuition is modest by global
      standards; and generous government and university scholarships are available to strong
      applicants. This guide is a short orientation before you dive into the details.
    </p>

    <h2>Why study in China</h2>
    <ul>
      <li><strong>Cost.</strong> Public-university tuition is typically a fraction of comparable programmes in Europe or North America, and living costs outside the biggest cities are low.</li>
      <li><strong>Scholarships.</strong> The Chinese Government Scholarship, provincial schemes and university awards can cover tuition, accommodation and a monthly stipend. See <NuxtLink :to="localePath('/guides/scholarship-types')">Scholarship Types</NuxtLink>.</li>
      <li><strong>Range.</strong> Engineering, medicine (MBBS), business, Chinese language, international relations and the sciences are all well represented, many with English-taught tracks.</li>
      <li><strong>Language and network.</strong> Even a year of Mandarin opens doors across Asia; graduates leave with a genuine regional network.</li>
    </ul>

    <h2>How the system is structured</h2>
    <p>
      A bachelor's degree usually takes four years (five for medicine and architecture); a master's
      takes two to three; a PhD three to four. The academic year runs from September to July with a
      long winter break around Spring Festival (late January or February). Universities are ranked
      informally through national projects, you will hear "Double First-Class", and the older "985"
      and "211" labels, which mark the research-intensive institutions.
    </p>

    <h2>Language of instruction</h2>
    <p>
      Chinese-taught degrees normally require an <strong>HSK</strong> certificate (HSK 4 for many
      programmes, HSK 5 for humanities and medicine). English-taught degrees require
      <strong>IELTS/TOEFL</strong> or proof that your previous education was in English, plus, for
      most, a semester or two of basic Mandarin as a graduation requirement. If your Chinese is not
      yet ready, a one-year language programme is the standard bridge, and many scholarships include it.
    </p>

    <h2>Daily life in brief</h2>
    <p>
      Campuses are walled, self-contained and safe, with on-site dormitories, canteens, clinics and
      sports facilities. Life runs through the phone: <strong>WeChat</strong> and
      <strong>Alipay</strong> handle messaging, payments, transport and deliveries. Cities are
      connected by a vast high-speed rail network, and the cost of eating out, local transport and
      data is very low. For the practical set-up, read <NuxtLink :to="localePath('/guides/living-in-china')">Living in China</NuxtLink>.
    </p>

    <h2>Your next steps</h2>
    <ol>
      <li>Shortlist programmes and universities, start from <NuxtLink :to="localePath('/scholarships')">Scholarships</NuxtLink> and <NuxtLink :to="localePath('/universities')">Universities</NuxtLink>.</li>
      <li>Check the language and academic requirements for each.</li>
      <li>Prepare your documents early (see <NuxtLink :to="localePath('/guides/how-to-apply')">How to Apply</NuxtLink>).</li>
      <li>Apply, accept your offer, and get your <strong>JW202/JW201</strong> form for the visa (see <NuxtLink :to="localePath('/guides/student-visa-guide')">Student Visa Guide</NuxtLink>).</li>
    </ol>
    <p>
      Create a profile on Nadoumi and we will match you to programmes, track your deadlines and
      guide each step to enrolment.
    </p>
  </GuideLayout>
</template>
