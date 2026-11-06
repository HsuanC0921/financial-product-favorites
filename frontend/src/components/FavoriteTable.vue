<script setup lang="ts">
import { ref, useId } from 'vue'
import { shiftDecimal, withCommas } from '../amount'
import type { Favorite } from '../types'

const popupId = useId()
const popupText = ref<string>()

defineProps<{
  items: Favorite[]
  accountNames: Map<string, string>
}>()
defineEmits<{ edit: [favorite: Favorite]; remove: [favorite: Favorite] }>()

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString('zh-TW', { hour12: false })
}
</script>

<template>
  <div class="table-wrap">
    <table>
      <thead>
        <tr>
          <th>產品</th>
          <th class="num">價格 × 數量</th>
          <th>扣款帳號</th>
          <th class="num">總手續費</th>
          <th class="num">預計扣款總金額</th>
          <th><span class="visually-hidden">操作</span></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.sn">
          <td class="product" data-label="產品">
            <div class="product-heading">
              <button
                class="link product-name"
                :popovertarget="popupId"
                popovertargetaction="show"
                @click="popupText = item.productName"
              >
                {{ item.productName }}
              </button>
              <button
                v-if="item.note"
                class="link text-preview"
                :popovertarget="popupId"
                popovertargetaction="show"
                :aria-label="`備註：${item.note}`"
                @click="popupText = item.note"
              >
                <img src="/notes.svg" alt="" width="14" height="14" />
                <span>{{ item.note }}</span>
              </button>
            </div>
            <small>{{ formatTime(item.createdAt) }}</small>
          </td>
          <td class="num qty" data-label="價格 × 數量">
            {{ withCommas(item.price) }} × {{ withCommas(String(item.purchaseQuantity)) }}
            <small>費率 {{ shiftDecimal(item.feeRate, 2) }}%</small>
          </td>
          <td class="account" data-label="扣款帳號">
            {{ item.account }}
            <button
              v-if="accountNames.get(item.account)"
              class="link text-preview"
              :popovertarget="popupId"
              popovertargetaction="show"
              :aria-label="`帳號暱稱：${accountNames.get(item.account)}`"
              @click="popupText = accountNames.get(item.account)"
            >
              <span>{{ accountNames.get(item.account) }}</span>
            </button>
          </td>
          <td class="num fee" data-label="總手續費">{{ withCommas(item.totalFee) }}</td>
          <td class="num total" data-label="預計扣款總金額">{{ withCommas(item.totalAmount) }}</td>
          <td class="action-cell">
            <div class="actions">
              <button @click="$emit('edit', item)">編輯</button>
              <button class="danger" @click="$emit('remove', item)">刪除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div :id="popupId" class="text-popup" popover role="note">{{ popupText }}</div>
  </div>
</template>

<style scoped>
/* 外層要有定位，裡面的隱藏文字才不會把頁面撐寬 */
.table-wrap {
  position: relative;
  overflow-x: auto;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 8px;
  box-shadow: var(--shadow);
}

table {
  width: 100%;
  font-variant-numeric: tabular-nums;
  border-collapse: collapse;
}

th,
td {
  padding: 0.5rem 0.75rem;
  text-align: left;
  white-space: nowrap;
  border-bottom: 1px solid var(--border);
}

tbody tr:last-child td {
  border-bottom: 0;
}

tbody tr:hover {
  background: var(--accent-soft);
}

th {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--muted);
  background: var(--bg);
}

.num {
  text-align: right;
}

small {
  display: block;
  color: var(--muted);
}

.product-heading {
  display: grid;
  grid-template-columns: minmax(8rem, max-content) minmax(3rem, 1fr);
  gap: 0.5rem;
  align-items: center;
}

.product-name {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
  -webkit-line-clamp: 2;
  font-weight: 700;
  text-align: left;
  overflow-wrap: anywhere;
  white-space: normal;
  text-decoration: none;
}

.text-preview {
  display: flex;
  gap: 0.25rem;
  align-items: center;
  max-width: 12rem;
  font-size: 0.875rem;
  color: var(--muted);
  text-align: left;
  text-decoration: none;
}

.text-preview img {
  flex-shrink: 0;
}

.text-preview span {
  overflow: hidden;
  text-overflow: ellipsis;
}

.text-popup {
  width: min(24rem, calc(100vw - 2rem));
  max-height: calc(100dvh - 2rem);
  padding: 1rem;
  overflow: auto;
  color: var(--text);
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 6px;
  box-shadow: var(--shadow);
}

.total {
  font-weight: 600;
}

.action-cell {
  width: 1%;
}

.actions {
  display: flex;
  gap: 0.5rem;
}

/* 表格最窄約 830px，再窄就改成一筆一張卡片，欄位名稱由 data-label 補上 */
@media (width <= 880px) {
  .table-wrap {
    overflow: visible;
    background: none;
    border: 0;
  }

  table,
  tbody,
  tr,
  td {
    display: block;
  }

  thead {
    display: none;
  }

  tr {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem 1rem;
    align-items: start;
    padding: 0.875rem 1rem;
    margin-bottom: 0.75rem;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 8px;
    box-shadow: var(--shadow);
  }

  tbody tr:hover {
    background: var(--surface);
  }

  td {
    flex-basis: 100%;
    min-width: 0;
    padding: 0;
    overflow-wrap: anywhere;
    white-space: normal;
    border: 0;
  }

  td::before {
    display: block;
    font-size: 0.875rem;
    color: var(--muted);
    content: attr(data-label);
  }

  .product {
    display: grid;
    grid-template-areas:
      'name name'
      'time note';
    grid-template-columns: max-content minmax(0, 1fr);
    column-gap: 0.5rem;
    align-items: center;
  }

  .product-heading {
    display: contents;
  }

  .product-name {
    grid-area: name;
  }

  .product > small {
    grid-area: time;
    white-space: nowrap;
  }

  .product .text-preview {
    grid-area: note;
    justify-self: end;
    max-width: 8rem;
  }

  .product::before {
    content: none;
  }

  .num {
    display: flex;
    flex-wrap: wrap;
    gap: 0 1rem;
    align-items: baseline;
    justify-content: flex-end;
    order: 1;
    white-space: nowrap;
  }

  .total {
    gap: 0.25rem 1rem;
    order: 0;
    font-size: 1.125rem;
  }

  .account {
    flex: 1 1 auto;
    order: 2;
    font-size: 0.875rem;
  }

  .action-cell {
    flex-basis: auto;
    align-self: flex-end;
    order: 2;
    width: auto;
    margin-left: auto;
  }

  .num::before {
    margin-right: auto;
  }

  td.qty small {
    flex-basis: 100%;
    text-align: right;
  }

  .actions {
    justify-content: flex-end;
  }

  .actions button {
    min-height: 2.75rem;
    padding-inline: 0.65rem;
  }
}
</style>
