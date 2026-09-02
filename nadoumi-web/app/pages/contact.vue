<script setup lang="ts">
useSeo('Contact', 'Get in touch with the Nadoumi team.')

const form = reactive({ name: '', email: '', message: '' })
const sent = ref(false)

function submit() {
  // Wiring to /api/public/contact (or a support inbox) lands with the Content slice.
  sent.value = true
}
</script>

<template>
  <div>
    <PageHero title="Contact" subtitle="We usually reply within two business days." />
    <form v-if="!sent" class="nad-form" @submit.prevent="submit">
      <label>Name<input v-model="form.name" type="text" required></label>
      <label>Email<input v-model="form.email" type="email" required></label>
      <label>Message<textarea v-model="form.message" rows="5" required /></label>
      <button type="submit">Send</button>
    </form>
    <p v-else>Thanks — we'll be in touch.</p>
  </div>
</template>

<style scoped>
.nad-form { display: grid; gap: 1rem; max-width: 32rem; }
.nad-form label { display: grid; gap: 0.35rem; font-size: 0.9rem; color: var(--nad-muted); }
.nad-form input, .nad-form textarea { padding: 0.6rem; border: 1px solid var(--nad-border); border-radius: 0.5rem; font: inherit; }
.nad-form button { justify-self: start; padding: 0.6rem 1.4rem; border: 0; border-radius: 0.5rem; background: var(--nad-accent); color: #fff; font: inherit; cursor: pointer; }
</style>
