<script setup lang="ts">
const localePath = useLocalePath()

const pay = [
  { icon: 'qr', term: 'WeChat Pay & Alipay', detail: 'QR codes cover almost every purchase, from the canteen to the metro. Link them to a Chinese bank account in your first weeks.' },
  { icon: 'wallet', term: 'A local bank account', detail: 'Bring your passport, residence permit and a university letter. International cards work at hotels and airports, rarely elsewhere.' },
  { icon: 'bag', term: 'A student budget', detail: 'Roughly 2,000 to 3,500 yuan a month for food, transport and essentials outside the biggest cities. Beijing, Shanghai and Shenzhen run higher.' },
] as const

const move = [
  { icon: 'metro', term: 'Metro & bus', detail: 'Pay by QR in every large city. Fares are a few yuan.' },
  { icon: 'train', term: 'High-speed rail', detail: 'Fast, punctual and affordable between cities. Book on the Railway 12306 app or Trip.com.' },
  { icon: 'phone', term: 'Didi', detail: 'The ride-hailing app for taxis and private cars, with an English interface.' },
  { icon: 'bike', term: 'Shared bikes', detail: 'Meituan and HelloRide bikes are everywhere for short hops; unlock by QR.' },
] as const

const daily = [
  { icon: 'food', term: 'Campus canteens', detail: 'The cheapest option, a full meal is 8 to 20 yuan. Halal (清真) canteens are common.' },
  { icon: 'phone', term: 'Meituan & Ele.me', detail: 'Food and groceries delivered in under an hour.' },
  { icon: 'bag', term: 'Taobao & JD', detail: 'Everything else, often next day, to a campus pick-up point.' },
  { icon: 'wifi', term: 'A local SIM', detail: 'From China Mobile, Unicom or Telecom with your passport. Data is cheap and fast.' },
] as const
</script>

<template>
  <GuideLayout slug="living-in-china">
    <p>
      Day-to-day life as an international student in China is comfortable and inexpensive once the
      basics are set up. Almost everything runs through your phone, campuses provide most of what you
      need, and cities are exceptionally well connected. Here is the practical picture.
    </p>

    <GuideSection eyebrow="Money" title="Paying for things">
      <GuideIconList :items="pay" />
      <p>Carry a little cash for small vendors and older markets, but you will reach for your phone first almost everywhere.</p>
    </GuideSection>

    <GuideSection eyebrow="Transport" title="Getting around">
      <GuideIconList :items="move" />
      <GuideCallout title="Always carry your documents" icon="passport" tone="amber">
        Your passport and residence permit are needed to buy train tickets and check into hotels.
      </GuideCallout>
    </GuideSection>

    <GuideSection eyebrow="Home" title="Where you will live">
      <p>
        Most universities offer <strong>on-campus dormitories</strong> for international students,
        single or shared rooms with a bathroom, from around 1,000 to 2,500 yuan a month, booked
        separately from admission and often with a deposit. Renting off campus is cheaper per room in
        smaller cities but means dealing with agents, contracts and your own police registration.
        First-year students almost always start in the dorm.
      </p>
    </GuideSection>

    <GuideSection eyebrow="Everyday" title="Food, delivery and connectivity">
      <GuideIconList :items="daily" />
      <p>
        Some foreign apps and sites are not reachable on Chinese networks; many students use a
        university or commercial VPN for study and to stay in touch with home. Set up
        <strong>WeChat</strong> before you arrive, it is the backbone of student life.
      </p>
    </GuideSection>

    <GuideSection eyebrow="Health" title="Insurance and care">
      <p>
        A <strong>comprehensive medical insurance policy</strong> for international students is
        mandatory, usually arranged through the university (about 600 to 800 yuan a year, included in
        most full scholarships). Campus clinics handle minor issues; larger hospitals have
        international or VIP departments with English-speaking staff. Keep the insurance card and
        policy number on your phone.
      </p>
    </GuideSection>

    <GuideSection eyebrow="Settling in" title="The first month">
      <ul class="list-disc space-y-2 pl-6 marker:text-slate-400">
        <li>Learn survival Mandarin early, numbers, directions and food, even if your degree is in English.</li>
        <li>Join the international student office orientation and the student associations, the fastest way to a network.</li>
        <li>Keep digital and paper copies of your passport, visa, admission letter and JW form.</li>
        <li>Follow the rules on registration and permits, see the <NuxtLink :to="localePath('/guides/student-visa-guide')">Student Visa Guide</NuxtLink>.</li>
      </ul>
    </GuideSection>
  </GuideLayout>
</template>
