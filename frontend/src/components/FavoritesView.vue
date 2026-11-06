<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ApiError, deleteFavorite, errorMessage, getFavorite, listFavorites } from '../api'
import type { Favorite, Me } from '../types'
import FavoriteForm from './FavoriteForm.vue'
import FavoriteTable from './FavoriteTable.vue'
import ModalDialog from './ModalDialog.vue'

const props = defineProps<{
  me: Me
  refreshMe: () => Promise<void>
}>()

// 清單一次載入 100 筆，「載入更多」接在後面，新增、修改、刪除後整份重新載入
const items = ref<Favorite[]>([])
const nextAfterSn = ref<number | null>(null)
const status = ref<'loading' | 'ready' | 'failed'>('loading')
const loadingMore = ref(false)
const error = ref('')
const keyword = ref('')

const formOpen = ref(false)
const editing = ref<Favorite | null>(null)
const deleting = ref<Favorite | null>(null)
const deleteBusy = ref(false)
const deleteError = ref('')

// 名稱篩選只在已載入的資料裡搜尋
const visibleItems = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return word ? items.value.filter((i) => i.productName.toLowerCase().includes(word)) : items.value
})

const accountNames = computed(
  () => new Map(props.me.accounts.map((a) => [a.account, a.accountName])),
)

// 每次重新載入就換一個編號，較早送出、較晚回來的清單請求直接丟掉
let loadId = 0

async function reload() {
  const id = ++loadId
  try {
    const page = await listFavorites()
    if (id !== loadId) return
    if (page.user.userId !== props.me.userId) {
      // 跨分頁切換帳號時重載整頁，清除舊身分的畫面狀態
      window.location.reload()
      return
    }
    items.value = page.items
    nextAfterSn.value = page.nextAfterSn
    status.value = 'ready'
    error.value = ''
  } catch (e) {
    if (id !== loadId) return
    error.value = errorMessage(e)
    status.value = 'failed'
  }
}

async function loadMore() {
  if (nextAfterSn.value === null) return
  const id = loadId
  loadingMore.value = true
  try {
    const page = await listFavorites(nextAfterSn.value)
    if (id !== loadId) return
    if (page.user.userId !== props.me.userId) {
      window.location.reload()
      return
    }
    items.value = [...items.value, ...page.items]
    nextAfterSn.value = page.nextAfterSn
    error.value = ''
  } catch (e) {
    if (id !== loadId) return
    error.value = errorMessage(e)
  } finally {
    loadingMore.value = false
  }
}

function openForm(favorite: Favorite | null) {
  editing.value = favorite
  formOpen.value = true
}

async function onSaved() {
  formOpen.value = false
  await reload()
}

function askDelete(favorite: Favorite) {
  deleteError.value = ''
  deleting.value = favorite
}

async function confirmDelete() {
  const target = deleting.value
  if (!target) return
  deleteBusy.value = true
  deleteError.value = ''
  try {
    await deleteFavorite(target.sn, target.version)
    deleting.value = null
    await reload()
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      // 已經被刪掉了，結果和使用者要的一樣
      deleting.value = null
      await reload()
    } else if (e instanceof ApiError && e.status === 412) {
      // 資料剛被改過：換成最新版本，讓使用者確認後再按一次
      try {
        const latest = await getFavorite(target.sn)
        items.value = items.value.map((i) => (i.sn === latest.sn ? latest : i))
        deleting.value = latest
        deleteError.value = '這筆資料剛被修改過，已更新為最新內容，請確認後再刪除'
      } catch (e2) {
        if (e2 instanceof ApiError && e2.status === 404) {
          // 取得最新內容時發現已經被刪掉了，同樣視為完成
          deleting.value = null
          await reload()
        } else {
          deleteError.value = errorMessage(e2)
        }
      }
    } else {
      deleteError.value = errorMessage(e)
    }
  } finally {
    deleteBusy.value = false
  }
}

onMounted(reload)
</script>

<template>
  <p v-if="status === 'loading'" role="status">載入中…</p>

  <template v-else>
    <p v-if="error" class="error" role="alert">{{ error }}</p>

    <div class="page-header">
      <div>
        <h2>我的喜好清單</h2>
        <p class="subtitle">
          共 {{ items.length }} 筆{{ nextAfterSn !== null ? '以上' : '' }}，金額單位為新台幣
        </p>
      </div>
      <button class="primary" @click="openForm(null)">新增喜好</button>
    </div>

    <div class="toolbar">
      <input
        v-model="keyword"
        type="search"
        placeholder="依產品名稱篩選"
        aria-label="依產品名稱篩選"
      />
    </div>

    <p v-if="status === 'failed'">
      <button @click="reload">重新載入</button>
    </p>
    <div v-else-if="items.length === 0" class="empty">
      <img src="/empty-list.svg" alt="" width="56" height="56" />
      <p>還沒有任何喜好紀錄，按「新增喜好」開始吧</p>
    </div>
    <p v-else-if="visibleItems.length === 0" class="empty">沒有符合的產品</p>

    <FavoriteTable
      v-else
      :items="visibleItems"
      :account-names="accountNames"
      @edit="openForm"
      @remove="askDelete"
    />

    <p v-if="nextAfterSn !== null" class="more">
      <button :disabled="loadingMore" @click="loadMore">
        {{ loadingMore ? '載入中…' : '載入更多' }}
      </button>
      <small>篩選只會搜尋已載入的資料</small>
    </p>
  </template>

  <FavoriteForm
    v-if="formOpen"
    :me="me"
    :favorite="editing"
    :refresh-me="refreshMe"
    @saved="onSaved"
    @close="formOpen = false"
  />

  <ModalDialog v-if="deleting" title="刪除喜好" :busy="deleteBusy" @close="deleting = null">
    <p>確定要刪除「{{ deleting.productName }}」嗎？刪除後無法復原。</p>
    <p v-if="deleteError" class="error" role="alert">{{ deleteError }}</p>
    <div class="dialog-actions">
      <button :disabled="deleteBusy" @click="deleting = null">取消</button>
      <button class="danger" :disabled="deleteBusy" @click="confirmDelete">確定刪除</button>
    </div>
  </ModalDialog>
</template>

<style scoped>
.page-header {
  display: flex;
  gap: 1rem;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

h2 {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--accent);
}

.toolbar {
  margin-bottom: 1rem;
}

.toolbar input {
  max-width: 24rem;
}

.empty {
  padding: 2rem 0;
  color: var(--muted);
  text-align: center;
}

.empty img {
  display: block;
  margin: 0 auto 0.5rem;
}

.empty p {
  margin: 0;
}

.more {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  margin-top: 1rem;
  color: var(--muted);
}
</style>
