<script setup lang="ts">
import { ref } from 'vue'
import { errorMessage, login } from '../api'
import TextField from './TextField.vue'

const emit = defineEmits<{ loggedIn: [] }>()

const userId = ref('')
const password = ref('')
const error = ref('')
const busy = ref(false)

async function submit() {
  busy.value = true
  error.value = ''
  try {
    await login(userId.value.trim(), password.value)
    emit('loggedIn')
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <form class="login" @submit.prevent="submit">
    <header>
      <h2>登入</h2>
      <p class="subtitle">登入後管理你的金融商品喜好紀錄</p>
    </header>
    <TextField v-model="userId" label="使用者 ID" autocomplete="username" required />
    <TextField
      v-model="password"
      label="密碼"
      type="password"
      autocomplete="current-password"
      required
    />
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <button class="primary" :disabled="busy">{{ busy ? '登入中…' : '登入' }}</button>
  </form>
</template>

<style scoped>
.login {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-width: 22rem;
  padding: 1.5rem;
  margin: 4rem auto;
  background: var(--surface);
  border: 1px solid var(--border);
  border-top: 3px solid var(--accent-bright);
  border-radius: 8px;
  box-shadow: var(--shadow);
}

h2 {
  margin: 0;
}
</style>
