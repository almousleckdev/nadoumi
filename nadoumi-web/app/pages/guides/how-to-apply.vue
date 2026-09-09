<script setup lang="ts">
const localePath = useLocalePath()
</script>

<template>
  <GuideLayout slug="how-to-apply">
    <p>
      Applying to a Chinese university is straightforward once your documents are in order. Most
      institutions open applications between <strong>November and March</strong> for a September
      start; scholarship deadlines fall earlier, often in <strong>January to April</strong>. Start
      about a year ahead.
    </p>

    <h2>Step 1: Choose programmes</h2>
    <p>
      Decide on the degree level, the language of instruction, the field and a budget. Shortlist
      three to six programmes so you have options if one is competitive. On Nadoumi you can filter by
      country, funding, language and level, then compare requirements side by side.
    </p>

    <h2>Step 2: Prepare your documents</h2>
    <ul>
      <li>Passport (valid for at least the length of your study, plus six months).</li>
      <li>Highest diploma and full academic transcripts, notarised and translated into Chinese or English.</li>
      <li>Language certificate: <strong>HSK</strong> for Chinese-taught, <strong>IELTS/TOEFL</strong> for English-taught (or an English-medium-of-instruction letter).</li>
      <li>A study plan or personal statement (usually 800 to 1,500 words).</li>
      <li>Two academic recommendation letters.</li>
      <li>A passport photo on a white background, and a completed <strong>Foreigner Physical Examination Form</strong> for programmes longer than six months.</li>
      <li>For applicants under 18, a notarised guardianship document.</li>
    </ul>

    <h2>Step 3: Submit the application</h2>
    <p>
      Applications go through the university's own portal and, for the Chinese Government Scholarship,
      also through the <strong>CSC</strong> system (agency number required). Pay the application fee
      (typically ¥400 to 800). Apply to your scholarship and your admission at the same time, one does
      not guarantee the other.
    </p>

    <h2>Step 4: Interviews and decisions</h2>
    <p>
      Some departments hold a short online interview. Admission decisions arrive from
      <strong>April to July</strong>. You will receive an <strong>Admission Notice</strong> and, if
      awarded, a separate <strong>Scholarship Certificate</strong>.
    </p>

    <h2>Step 5: Accept and get your visa form</h2>
    <p>
      Confirm your place and the university will issue a <strong>JW202</strong> (self-funded /
      university scholarship) or <strong>JW201</strong> (government scholarship) form together with
      the admission letter. These two documents are what you take to the visa application.
      Continue to the <NuxtLink :to="localePath('/guides/student-visa-guide')">Student Visa Guide</NuxtLink>.
    </p>

    <h2>Step 6: Before you fly</h2>
    <ul>
      <li>Book accommodation (on-campus dorms usually need a separate reservation and deposit).</li>
      <li>Arrange the first months' funds; scholarship stipends are paid after registration.</li>
      <li>Bring originals of every document, the university checks them at registration.</li>
      <li>Note your registration deadline on the admission letter; missing it can void the offer.</li>
    </ul>

    <p>
      Nadoumi tracks each of these steps for you, flags deadlines, and reviews your documents before
      you submit. <NuxtLink :to="localePath('/register')">Create a profile</NuxtLink> to begin.
    </p>
  </GuideLayout>
</template>
