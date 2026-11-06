<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, errorMessage, fetchMe, logout, setSessionExpiredHandler } from './api'
import FavoritesView from './components/FavoritesView.vue'
import LoginForm from './components/LoginForm.vue'
import type { Me } from './types'

const me = ref<Me | null>(null)
const ready = ref(false)
const notice = ref('')
const error = ref('')

setSessionExpiredHandler(() => {
  if (me.value) {
    me.value = null
    error.value = ''
    notice.value = '登入已逾時，請重新登入'
  }
})

async function refreshMe() {
  me.value = await fetchMe()
}

async function loadMe() {
  try {
    await refreshMe()
    notice.value = ''
    error.value = ''
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      me.value = null
      error.value = ''
    } else {
      error.value = errorMessage(e)
    }
  } finally {
    ready.value = true
  }
}

async function signOut() {
  try {
    await logout()
    me.value = null
    notice.value = ''
    error.value = ''
    await loadMe()
  } catch (e) {
    error.value = `尚未完成登出。${errorMessage(e)}`
  }
}

onMounted(loadMe)
</script>

<template>
  <header class="topbar">
    <h1>
      <img src="/favicon.svg" alt="" width="28" height="28" />
      金融商品喜好紀錄系統
    </h1>
    <div v-if="me" class="user">
      <span class="who">
        <strong>{{ me.userName }}</strong>
        <small>{{ me.email }}</small>
      </span>
      <button @click="signOut">登出</button>
    </div>
  </header>

  <main :class="{ guest: ready && !me }">
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="!ready" role="status">載入中…</p>
    <FavoritesView v-else-if="me" :me="me" :refresh-me="refreshMe" />
    <p v-else-if="error"><button @click="loadMe">重新載入</button></p>
    <template v-else>
      <p v-if="notice" class="notice" role="status">{{ notice }}</p>
      <LoginForm @logged-in="loadMe" />
    </template>
  </main>
</template>

<style scoped>
.topbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem max(1rem, calc((100% - 1200px) / 2 + 1rem));
  background: var(--surface);
  border-bottom: 3px solid var(--accent-bright);
  box-shadow: var(--shadow);
}

h1 {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  margin: 0;
  font-size: 1.15rem;
  color: var(--accent);
}

.user {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  color: var(--muted);
}

.who {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.35;
}

/* 窄螢幕：標題與登出同一列，姓名與 Email 獨立一行放在下面 */
@media (width <= 560px) {
  .topbar {
    display: grid;
    grid-template-areas:
      'title logout'
      'who who';
    grid-template-columns: 1fr auto;
    gap: 0.25rem 0.75rem;
  }

  h1 {
    grid-area: title;
    font-size: 1.05rem;
  }

  .user {
    display: contents;
  }

  .user button {
    grid-area: logout;
  }

  .who {
    flex-direction: row;
    grid-area: who;
    gap: 0.5rem;
    align-items: baseline;
  }
}

main {
  max-width: 1200px;
  padding: 1.5rem 1rem;
  margin: 0 auto;
}

/* 登入頁整片鋪上淡淡的等高線圖樣，圖樣來源見 README 的第三方素材 */
main.guest {
  max-width: none;
  min-height: calc(100vh - 3.75rem);
  background: url('/topography.svg') center / 600px;
}

main.guest .notice {
  max-width: 22rem;
  margin: 0 auto;
}
</style>
