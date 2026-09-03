<script setup lang="ts">
import type { Page, ScholarshipCard, UniversitySummary } from '~/types/catalog'
import { imagery } from '~/data/imagery'

const { t } = useI18n()
const localePath = useLocalePath()
useSeo(t('home.hero.title'), t('home.hero.subtitle'))

const { publicGet } = useApi()

const { data: featured, pending: featuredPending, error: featuredError } = useLazyAsyncData(
  'home-featured-universities',
  () => publicGet<Page<UniversitySummary>>('universities', { featured: true, size: 12 }),
  { default: () => null },
)
const { data: recommended, pending: recommendedPending, error: recommendedError } = useLazyAsyncData(
  'home-recommended-universities',
  () => publicGet<Page<UniversitySummary>>('universities', { recommended: true, size: 12 }),
  { default: () => null },
)

const { data: newSch, pending: newSchPending, error: newSchError } = useLazyAsyncData(
  'home-new-scholarships',
  () => publicGet<Page<ScholarshipCard>>('scholarships', { sort: 'newest', size: 12 }),
  { default: () => null },
)
const { data: fundedSch, pending: fundedSchPending, error: fundedSchError } = useLazyAsyncData(
  'home-funded-scholarships',
  () => publicGet<Page<ScholarshipCard>>('scholarships', { funding: 'FULLY', size: 12 }),
  { default: () => null },
)

const featuredUnis = computed(() => featured.value?.content ?? [])
const recommendedUnis = computed(() => recommended.value?.content ?? [])
const newScholarships = computed(() => newSch.value?.content ?? [])
const fundedScholarships = computed(() => fundedSch.value?.content ?? [])

const journey = computed(() => [
  { title: t('home.journey.s1t'), body: t('home.journey.s1b') },
  { title: t('home.journey.s2t'), body: t('home.journey.s2b') },
  { title: t('home.journey.s3t'), body: t('home.journey.s3b') },
  { title: t('home.journey.s4t'), body: t('home.journey.s4b') },
  { title: t('home.journey.s5t'), body: t('home.journey.s5b') },
  { title: t('home.journey.s6t'), body: t('home.journey.s6b') },
  { title: t('home.journey.s7t'), body: t('home.journey.s7b') },
  { title: t('home.journey.s8t'), body: t('home.journey.s8b') },
])
</script>

<template>
  <div>
    <HomeHero />

    <NContainer>
      <DiscoverySection
        :eyebrow="t('home.featuredUnis.eyebrow')"
        :title="t('home.featuredUnis.title')"
        :description="t('home.featuredUnis.description')"
        :carousel-label="t('home.featuredUnis.title')"
        :view-all-to="localePath('/universities')"
        :view-all-label="t('catalog.viewAllUniversities')"
        :pending="featuredPending"
        :error="featuredError ? t('errors.loadSection') : ''"
        :empty="!featuredUnis.length"
        :empty-text="t('home.featuredUnis.empty')"
      >
        <UniversityCard
          v-for="u in featuredUnis"
          :key="u.id"
          :university="u"
          variant="carousel"
        />
      </DiscoverySection>
    </NContainer>

    <NContainer>
      <StorySplit
        :image="imagery.storyOutdoors"
        :eyebrow="t('home.story1.eyebrow')"
        :title="t('home.story1.title')"
        :body="t('home.story1.body')"
      >
        <template #actions>
          <NButton :to="localePath('/universities')" variant="secondary">
            {{ t('home.hero.ctaUniversities') }}
          </NButton>
        </template>
      </StorySplit>
    </NContainer>

    <NContainer>
      <DiscoverySection
        :eyebrow="t('home.newScholarships.eyebrow')"
        :title="t('home.newScholarships.title')"
        :description="t('home.newScholarships.description')"
        :carousel-label="t('home.newScholarships.title')"
        :view-all-to="localePath('/scholarships')"
        :view-all-label="t('scholarships.viewAll')"
        :pending="newSchPending"
        :error="newSchError ? t('errors.loadSection') : ''"
        :empty="!newScholarships.length"
        :empty-text="t('home.newScholarships.empty')"
      >
        <ScholarshipCard
          v-for="sch in newScholarships"
          :key="sch.id"
          :scholarship="sch"
          variant="carousel"
        />
      </DiscoverySection>
    </NContainer>

    <NContainer>
      <StorySplit
        :image="imagery.storyStudy"
        :eyebrow="t('home.story2.eyebrow')"
        :title="t('home.story2.title')"
        :body="t('home.story2.body')"
        reverse
      >
        <template #actions>
          <NButton :to="localePath('/register')">{{ t('home.hero.ctaScholarships') }}</NButton>
        </template>
      </StorySplit>
    </NContainer>

    <NContainer>
      <DiscoverySection
        :eyebrow="t('home.fundedScholarships.eyebrow')"
        :title="t('home.fundedScholarships.title')"
        :description="t('home.fundedScholarships.description')"
        :carousel-label="t('home.fundedScholarships.title')"
        :view-all-to="localePath('/scholarships?funding=FULLY')"
        :view-all-label="t('scholarships.viewAll')"
        :pending="fundedSchPending"
        :error="fundedSchError ? t('errors.loadSection') : ''"
        :empty="!fundedScholarships.length"
        :empty-text="t('home.fundedScholarships.empty')"
      >
        <ScholarshipCard
          v-for="sch in fundedScholarships"
          :key="sch.id"
          :scholarship="sch"
          variant="carousel"
        />
      </DiscoverySection>
    </NContainer>

    <NContainer>
      <SectionPlaceholder
        :eyebrow="t('home.programDiscovery.eyebrow')"
        :title="t('home.programDiscovery.title')"
        :description="t('home.programDiscovery.description')"
        :arriving-with="t('home.arriving.programs')"
      />
    </NContainer>

    <NContainer>
      <DiscoverySection
        :eyebrow="t('home.recommendedUnis.eyebrow')"
        :title="t('home.recommendedUnis.title')"
        :description="t('home.recommendedUnis.description')"
        :carousel-label="t('home.recommendedUnis.title')"
        :view-all-to="localePath('/universities')"
        :view-all-label="t('catalog.viewAllUniversities')"
        :pending="recommendedPending"
        :error="recommendedError ? t('errors.loadSection') : ''"
        :empty="!recommendedUnis.length"
        :empty-text="t('home.recommendedUnis.empty')"
      >
        <UniversityCard
          v-for="u in recommendedUnis"
          :key="u.id"
          :university="u"
          variant="carousel"
        />
      </DiscoverySection>
    </NContainer>

    <NContainer>
      <SectionPlaceholder
        :eyebrow="t('home.partners.eyebrow')"
        :title="t('home.partners.title')"
        :description="t('home.partners.description')"
        :arriving-with="t('home.arriving.partnerships')"
      />
    </NContainer>

    <section class="border-t border-slate-200 bg-slate-50">
      <NContainer>
        <div class="py-16 sm:py-20">
          <SectionHeading
            :eyebrow="t('home.journey.eyebrow')"
            :title="t('home.journey.title')"
            :description="t('home.journey.description')"
          />
          <div class="mt-10">
            <JourneyTimeline :steps="journey" />
          </div>
        </div>
      </NContainer>
    </section>

    <CtaBand
      :image="imagery.ctaClassroom"
      :eyebrow="t('home.cta.eyebrow')"
      :title="t('home.cta.title')"
      :body="t('home.cta.body')"
    >
      <template #actions>
        <NButton :to="localePath('/register')" size="lg">{{ t('home.cta.apply') }}</NButton>
        <NButton :to="localePath('/contact')" variant="secondary" size="lg">{{ t('home.cta.contact') }}</NButton>
      </template>
    </CtaBand>
  </div>
</template>
