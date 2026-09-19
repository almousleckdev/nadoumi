<script setup lang="ts">
import type { ContactBody } from '~/composables/useApplicant'
import type { ContactDto } from '~/types/catalog'
import { contactBody, contactForm } from '~/utils/sectionMappers'
import { validateContact } from '~/utils/sectionRules'

const props = defineProps<{ record: ContactDto | null, busy: boolean }>()
const emit = defineEmits<{ submit: [body: ContactBody], cancel: [] }>()
const { t } = useI18n()
const { options: relations } = useEnumOptions('contactRelation')

const form = reactive(contactForm(props.record))
const { errors, submit } = useValidatedForm(() => validateContact(form))
</script>

<template>
  <form class="grid gap-4 sm:grid-cols-2" novalidate @submit.prevent="submit(() => emit('submit', contactBody(form)))">
    <FormSelectField id="contact-relation" v-model="form.relation" :label="t('contact.relation')" :options="relations" :error="errors.relation" required />
    <FormTextField id="contact-name" v-model="form.name" :label="t('contact.name')" :error="errors.name" autocomplete="off" :maxlength="150" required />
    <FormTextField id="contact-phone" v-model="form.phone" type="tel" :label="t('contact.phone')" :hint="t('contact.phoneHint')" :error="errors.phone" autocomplete="off" :maxlength="32" :required="form.relation !== 'OTHER'" />
    <FormTextField id="contact-email" v-model="form.email" type="email" :label="t('contact.email')" :error="errors.email" autocomplete="off" :maxlength="120" />
    <FormActions :busy="busy" cancellable @cancel="emit('cancel')" />
  </form>
</template>
