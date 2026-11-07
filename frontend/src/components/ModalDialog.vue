<script setup lang="ts">
import { onBeforeUnmount, onMounted, useId, useTemplateRef } from 'vue'

defineProps<{ title: string; busy?: boolean }>()
const emit = defineEmits<{ close: [] }>()

const dialog = useTemplateRef('dialog')
const titleId = useId()

// 用原生 dialog：自帶焦點管理、Esc 關閉與背景遮罩
onMounted(() => dialog.value?.showModal())
onBeforeUnmount(() => dialog.value?.close())
</script>

<template>
  <dialog
    ref="dialog"
    :aria-labelledby="titleId"
    :aria-busy="busy || undefined"
    @cancel="busy && $event.preventDefault()"
    @close="emit('close')"
  >
    <h2 :id="titleId">{{ title }}</h2>
    <slot />
  </dialog>
</template>

<style scoped>
dialog {
  width: min(36rem, calc(100vw - 2rem));
  padding: 1.25rem;
  overflow-wrap: anywhere;
  border: 0;
  border-radius: 10px;
  box-shadow: 0 12px 32px rgb(16 24 40 / 20%);
}

dialog::backdrop {
  background: var(--backdrop);
}

h2 {
  margin: 0 0 1rem;
  font-size: 1.1rem;
}
</style>
