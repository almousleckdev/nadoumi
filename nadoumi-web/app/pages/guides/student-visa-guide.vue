<script setup lang="ts">
const localePath = useLocalePath()
</script>

<template>
  <GuideLayout slug="student-visa-guide">
    <p>
      Studying in China for longer than 180 days means an <strong>X1</strong> visa, converted into a
      <strong>residence permit</strong> after you arrive. Shorter programmes use an <strong>X2</strong>
      visa. This is the standard path; always check the requirements of your nearest Chinese embassy
      or consulate, as details vary by country.
    </p>

    <h2>Before you apply, the two key documents</h2>
    <ul>
      <li><strong>Admission Notice</strong>: the original letter from your university.</li>
      <li><strong>JW202</strong> (self-funded or university-scholarship students) or <strong>JW201</strong> (Chinese Government Scholarship students), the "Visa Application for Study in China" form, issued by the university.</li>
    </ul>
    <p>The university sends both after you accept your place. Keep the originals safe, the embassy and, later, the immigration office both need them.</p>

    <h2>Applying for the visa</h2>
    <ol>
      <li>Complete the online visa application form (COVA) and print it.</li>
      <li>Gather: passport (valid 6+ months, with a blank page), a recent passport photo, the Admission Notice and JW202/JW201, and any documents the consulate lists for your nationality.</li>
      <li>Book an appointment at the Chinese Visa Application Service Centre or embassy, submit in person, and pay the fee (varies by country and processing speed).</li>
      <li>Collect the visa in about 4 working days (standard), an X1 is usually issued for a single entry, valid 90 days from issue, so time your travel accordingly.</li>
    </ol>

    <h2>After you arrive</h2>
    <ol>
      <li><strong>Register your address within 24 hours</strong> at the local police station. If you live in a university dormitory, the university normally does this for you; if you rent, you must do it yourself and repeat it after any trip abroad.</li>
      <li><strong>Complete the medical check.</strong> If you did the Foreigner Physical Examination in your home country, bring the report and X-ray to be verified at the local International Travel Healthcare Centre; otherwise you take the exam in China.</li>
      <li><strong>Apply for the residence permit within 30 days</strong> of entry, through the Exit-Entry Administration Bureau. You will need: passport with the X1 visa, JW202/JW201, Admission Notice, the verified medical report, the accommodation registration form, passport photos and the fee. The permit replaces the visa and is normally issued for the length of the academic year, renewable annually.</li>
    </ol>

    <h2>While you study</h2>
    <ul>
      <li>Renew the residence permit at least <strong>one month before it expires</strong>.</li>
      <li>Re-register your address with the police after every re-entry to China.</li>
      <li>Part-time work and internships require prior permission from both the university and immigration, do not work on a study permit without it.</li>
      <li>If you change university or drop out, your permit must be updated or cancelled promptly.</li>
    </ul>

    <p>
      Nadoumi walks scholarship and self-funded students through the JW form, the visa file and the
      post-arrival steps. <NuxtLink :to="localePath('/guides/how-to-apply')">See How to Apply</NuxtLink>
      for what comes before this.
    </p>
  </GuideLayout>
</template>
