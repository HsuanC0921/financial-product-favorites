<script setup lang="ts">
import { shiftDecimal, withCommas } from '../amount'
import type { Favorite } from '../types'

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
            <strong>{{ item.productName }}</strong>
            <small v-if="item.note">{{ item.note }}</small>
            <small>{{ formatTime(item.createdAt) }}</small>
          </td>
          <td class="num qty" data-label="價格 × 數量">
            {{ withCommas(item.price) }} × {{ withCommas(String(item.purchaseQuantity)) }}
            <small>費率 {{ shiftDecimal(item.feeRate, 2) }}%</small>
          </td>
          <td class="account" data-label="扣款帳號">
            {{ item.account }}
            <small v-if="accountNames.get(item.account)">{{
              accountNames.get(item.account)
            }}</small>
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
  font-weight: 600;
  color: var(--muted);
  background: var(--bg);
}

.num {
  font-variant-numeric: tabular-nums;
  text-align: right;
}

small {
  display: block;
  color: var(--muted);
}

.product strong {
  display: block;
}

.total {
  font-weight: 600;
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
    display: grid;
    grid-template-areas:
      'product total'
      'qty fee'
      'account actions';
    grid-template-columns: 1fr auto;
    gap: 0.75rem 1rem;
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
    padding: 0;
    white-space: normal;
    border: 0;
  }

  /* 標籤改成數值上方的小字，不佔一整列 */
  td::before {
    display: block;
    font-size: 0.8125rem;
    color: var(--muted);
    content: attr(data-label);
  }

  .product {
    grid-area: product;
  }

  .product::before {
    content: none;
  }

  .total {
    grid-area: total;
    font-size: 1.125rem;
  }

  .qty {
    grid-area: qty;
    text-align: left;
  }

  .fee {
    grid-area: fee;
  }

  .account {
    grid-area: account;
  }

  .action-cell {
    grid-area: actions;
    align-self: end;
  }

  td:not(.product) small {
    display: inline;
  }

  td.qty small {
    display: block;
  }

  .actions {
    justify-content: flex-end;
  }

  .actions button {
    min-height: 2.25rem;
  }
}
</style>
