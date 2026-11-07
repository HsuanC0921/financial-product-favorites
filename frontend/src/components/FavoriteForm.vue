<script setup lang="ts">
import { computed, nextTick, ref, useId } from 'vue'
import {
  ApiError,
  addAccount,
  createFavorite,
  errorMessage,
  getFavorite,
  updateFavorite,
} from '../api'
import { estimate, isPercent, isPrice, isQuantity, shiftDecimal, withCommas } from '../amount'
import type { Account, Favorite, FavoriteInput, Me } from '../types'
import ModalDialog from './ModalDialog.vue'
import TextField from './TextField.vue'

const props = defineProps<{
  me: Me
  favorite: Favorite | null
  refreshMe: () => Promise<void>
}>()
const emit = defineEmits<{ saved: []; close: [] }>()

// 新增與編輯共用同一份表單：有傳入 favorite 就是編輯
// 欄位初始值只在開啟時讀一次，之後以使用者輸入為準
const editing = props.favorite !== null
const accountId = useId()
const noteId = useId()

const productName = ref(props.favorite?.productName ?? '')
const price = ref(props.favorite?.price ?? '')
// 畫面上的費率用百分比（0.1425 代表 0.1425%），送出時才換成後端用的比例
const percent = ref(props.favorite ? shiftDecimal(props.favorite.feeRate, 2) : '')
const quantity = ref(props.favorite ? String(props.favorite.purchaseQuantity) : '')
const account = ref(props.favorite?.account ?? props.me.defaultAccount)
const note = ref(props.favorite?.note ?? '')

// 修改時帶回讀取當下的版本，發生衝突後取得最新版本會更新這個值
const version = ref(props.favorite?.version ?? 0)
const submitted = ref(false)
const busy = ref(false)
const error = ref('')
const conflict = ref(false)
const latestSummary = ref('')

const errors = computed(() => ({
  productName: productName.value.trim() ? '' : '請輸入產品名稱',
  price: isPrice(price.value) ? '' : '產品價格須介於 0.01 到 9,999,999,999.99，最多兩位小數',
  percent: isPercent(percent.value) ? '' : '請輸入 0 到 100 的費率，最多六位小數',
  quantity: isQuantity(quantity.value) ? '' : '請輸入 1 到 1,000,000 的整數',
}))
const hasErrors = computed(() => Object.values(errors.value).some(Boolean))
// 欄位錯誤等到第一次按儲存才顯示，避免使用者還沒輸入就被提醒
const shown = computed(() => (submitted.value ? errors.value : null))

const preview = computed(() =>
  errors.value.percent
    ? null
    : estimate(price.value, shiftDecimal(percent.value, -2), Number(quantity.value)),
)

function toInput(): FavoriteInput {
  return {
    productName: productName.value.trim(),
    price: price.value,
    feeRate: shiftDecimal(percent.value, -2),
    purchaseQuantity: Number(quantity.value),
    account: account.value,
    note: note.value,
  }
}

async function submit(event: SubmitEvent) {
  submitted.value = true
  if (hasErrors.value) {
    const form = event.currentTarget as HTMLFormElement
    await nextTick()
    form.querySelector<HTMLInputElement>('[aria-invalid="true"]')!.focus()
    return
  }
  busy.value = true
  error.value = ''
  try {
    if (props.favorite) await updateFavorite(props.favorite.sn, version.value, toInput())
    else await createFavorite(toInput())
    emit('saved')
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      // 紀錄已經不存在，回到清單讓使用者看到最新狀態
      emit('saved')
      return
    }
    conflict.value = e instanceof ApiError && e.status === 412
    if (conflict.value) latestSummary.value = ''
    error.value = errorMessage(e)
  } finally {
    busy.value = false
  }
}

// 版本衝突時只更新版本號，使用者已經輸入的內容原封不動
async function reloadVersion() {
  if (!props.favorite) return
  busy.value = true
  try {
    const latest = await getFavorite(props.favorite.sn)
    version.value = latest.version
    latestSummary.value =
      `目前資料庫裡是：${latest.productName}，產品價格 ${latest.price}，` +
      `費率 ${shiftDecimal(latest.feeRate, 2)}%，數量 ${latest.purchaseQuantity}，` +
      `扣款帳號 ${latest.account}，備註 ${latest.note || '未填寫'}`
    conflict.value = false
    error.value = ''
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) emit('saved')
    else error.value = errorMessage(e)
  } finally {
    busy.value = false
  }
}

const addingAccount = ref(false)
const newAccount = ref('')
const newAccountName = ref('')
const accountError = ref('')
const accountBusy = ref(false)
const createdAccount = ref<Account | null>(null)

async function submitAccount() {
  accountError.value = ''
  if (!createdAccount.value && !/^\d{1,32}$/.test(newAccount.value)) {
    accountError.value = '帳號只能是 1 到 32 位數字'
    return
  }
  if (!createdAccount.value && !newAccountName.value.trim()) {
    accountError.value = '請輸入帳號暱稱'
    return
  }
  accountBusy.value = true
  try {
    if (!createdAccount.value) {
      createdAccount.value = await addAccount(newAccount.value, newAccountName.value.trim())
    }
    await props.refreshMe()
    account.value = createdAccount.value.account
    addingAccount.value = false
    createdAccount.value = null
    newAccount.value = ''
    newAccountName.value = ''
  } catch (e) {
    accountError.value = createdAccount.value
      ? `帳號已建立，但帳號清單尚未更新。${errorMessage(e)}`
      : errorMessage(e)
  } finally {
    accountBusy.value = false
  }
}
</script>

<template>
  <ModalDialog
    :title="editing ? '編輯喜好' : '新增喜好'"
    :busy="busy || accountBusy"
    @close="emit('close')"
  >
    <form class="favorite-form" novalidate @submit.prevent="submit">
      <TextField
        v-model="productName"
        class="wide"
        label="產品名稱"
        :maxlength="100"
        :error="shown?.productName"
        :disabled="busy || accountBusy"
      />
      <TextField
        v-model="price"
        label="產品價格"
        inputmode="decimal"
        :error="shown?.price"
        :disabled="busy || accountBusy"
      />
      <TextField
        v-model="percent"
        label="手續費率（%）"
        inputmode="decimal"
        :error="shown?.percent"
        :disabled="busy || accountBusy"
      />
      <TextField
        v-model="quantity"
        class="wide-narrow"
        label="購買數量"
        inputmode="numeric"
        :error="shown?.quantity"
        :disabled="busy || accountBusy"
      />

      <div class="field wide-narrow">
        <div class="field-head">
          <label :for="accountId">扣款帳號</label>
          <button
            v-if="!addingAccount"
            type="button"
            class="link"
            :disabled="busy || accountBusy"
            @click="addingAccount = true"
          >
            新增帳號
          </button>
        </div>
        <select :id="accountId" v-model="account" :disabled="busy || accountBusy">
          <option v-for="a in me.accounts" :key="a.account" :value="a.account">
            {{ a.account }}（{{ a.accountName }}）
          </option>
        </select>
      </div>

      <fieldset v-if="addingAccount" class="new-account" :disabled="busy || accountBusy">
        <legend>新增扣款帳號</legend>
        <TextField
          v-model="newAccount"
          label="帳號"
          inputmode="numeric"
          :maxlength="32"
          :disabled="createdAccount !== null"
        />
        <TextField
          v-model="newAccountName"
          label="暱稱"
          :maxlength="50"
          :disabled="createdAccount !== null"
        />
        <p v-if="accountError" class="error" role="alert">{{ accountError }}</p>
        <div class="dialog-actions">
          <button type="button" @click="addingAccount = false">
            {{ createdAccount ? '關閉' : '取消' }}
          </button>
          <button type="button" class="primary" @click="submitAccount">
            {{ accountBusy ? '處理中…' : createdAccount ? '重新載入帳號' : '加入帳號' }}
          </button>
        </div>
      </fieldset>

      <div class="field wide">
        <label :for="noteId">備註（選填）</label>
        <textarea
          :id="noteId"
          v-model="note"
          maxlength="200"
          rows="2"
          :disabled="busy || accountBusy"
        />
      </div>

      <p v-if="preview" class="preview" aria-live="polite">
        <span class="subtitle">預估總手續費用</span>
        <span class="preview-value">{{ withCommas(preview.fee) }}</span>
        <span class="subtitle">預計扣款總金額</span>
        <strong class="preview-value">{{ withCommas(preview.total) }}</strong>
      </p>

      <p v-if="error" class="error" role="alert">
        {{ error }}
        <button v-if="conflict" type="button" class="link" :disabled="busy" @click="reloadVersion">
          取得最新版本
        </button>
      </p>
      <p v-if="latestSummary" class="notice">
        已取得最新版本，你輸入的內容都還在，確認後再按儲存。{{ latestSummary }}
      </p>

      <div class="dialog-actions">
        <span class="subtitle currency"><span aria-hidden="true">ⓘ</span> 新台幣計價</span>
        <button type="button" :disabled="busy || accountBusy" @click="emit('close')">取消</button>
        <button class="primary" :disabled="busy || accountBusy || addingAccount">
          {{ busy ? '儲存中…' : '儲存' }}
        </button>
      </div>
    </form>
  </ModalDialog>
</template>

<style scoped>
.favorite-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem 1rem;
}

/* 跨滿兩欄的項目 */
.favorite-form > .wide,
.favorite-form > p,
.favorite-form > fieldset,
.favorite-form > .dialog-actions {
  grid-column: 1 / -1;
}

.field-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.new-account {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  border: 1px solid var(--border);
  border-radius: 6px;
}

.currency {
  align-self: center;
  margin-right: auto;
}

.preview {
  display: grid;
  grid-template-columns: max-content minmax(0, 1fr);
  gap: 0.25rem 0.75rem;
  align-items: baseline;
  padding: 0.5rem 0.75rem;
  margin: 0;
  font-variant-numeric: tabular-nums;
  background: var(--accent-soft);
  border-radius: 6px;
}

.preview-value {
  text-align: right;
  overflow-wrap: anywhere;
}

/* 窄螢幕：價格與費率仍並排，數量與扣款帳號（下拉文字較長）各占一列 */
@media (width <= 560px) {
  .preview {
    grid-template-columns: minmax(0, 1fr);
  }

  .favorite-form > .wide-narrow {
    grid-column: 1 / -1;
  }
}
</style>
