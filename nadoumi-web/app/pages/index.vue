<script setup lang="ts">
import { imagery } from '~/data/imagery'
import profileImage from '~/assets/images/image1.png'
import discoverImage from '~/assets/images/image.png'

const { t } = useI18n()
const localePath = useLocalePath()
useSeo(t('home.hero.title'), t('home.hero.subtitle'))

const { featured, recommended, partners } = useHomeFeeds()

const JOURNEY_STEPS = 8
const journey = computed(() => Array.from({ length: JOURNEY_STEPS }, (_, i) => ({
  title: t(`home.journey.s${i + 1}t`),
  body: t(`home.journey.s${i + 1}b`),
})))
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
        :pending="featured.pending.value"
        :error="featured.error.value ? t('errors.loadSection') : ''"
        :empty="!featured.items.value.length"
        :empty-text="t('home.featuredUnis.empty')"
      >
        <UniversityCard
          v-for="u in featured.items.value"
          :key="u.id"
          :university="u"
          variant="carousel"
        />
      </DiscoverySection>
    </NContainer>

    <NContainer>
      <StorySplit
        :image="{ src: discoverImage, alt: t('home.story1.title') }"
        image-local
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
      <CategoryShowcase kind="scholarship" />
    </NContainer>

    <NContainer>
      <StorySplit
        :image="{ src: profileImage, alt: t('home.story2.eyebrow') }"
        image-local
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
      <CategoryShowcase kind="program" />
    </NContainer>

    <NContainer>
      <DiscoverySection
        :eyebrow="t('home.recommendedUnis.eyebrow')"
        :title="t('home.recommendedUnis.title')"
        :description="t('home.recommendedUnis.description')"
        :carousel-label="t('home.recommendedUnis.title')"
        :view-all-to="localePath('/universities')"
        :view-all-label="t('catalog.viewAllUniversities')"
        :pending="recommended.pending.value"
        :error="recommended.error.value ? t('errors.loadSection') : ''"
        :empty="!recommended.items.value.length"
        :empty-text="t('home.recommendedUnis.empty')"
      >
        <UniversityCard
          v-for="u in recommended.items.value"
          :key="u.id"
          :university="u"
          variant="carousel"
        />
      </DiscoverySection>
    </NContainer>

    <NContainer>
      <DiscoverySection
        v-if="partners.pending.value || partners.items.value.length"
        :eyebrow="t('home.partners.eyebrow')"
        :title="t('home.partners.title')"
        :description="t('home.partners.description')"
        :carousel-label="t('home.partners.title')"
        :pending="partners.pending.value"
        :error="partners.error.value ? t('errors.loadSection') : ''"
        :empty="!partners.items.value.length"
      >
        <PartnerLogos :universities="partners.items.value" />
      </DiscoverySection>
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

    <StudentTestimonials />

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
