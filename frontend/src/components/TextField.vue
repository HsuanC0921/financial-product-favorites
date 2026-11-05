<script setup lang="ts">
import { useId } from 'vue'

defineProps<{
  label: string
  error?: string
  type?: 'text' | 'password'
  inputmode?: 'decimal' | 'numeric'
  autocomplete?: string
  maxlength?: number
  required?: boolean
}>()

const model = defineModel<string>({ required: true })
const id = useId()
</script>

<template>
  <div class="field">
    <label :for="id">{{ label }}</label>
    <input
      :id="id"
      v-model="model"
      :type="type ?? 'text'"
      :inputmode="inputmode"
      :autocomplete="autocomplete"
      :maxlength="maxlength"
      :required="required"
      :aria-invalid="error ? true : undefined"
      :aria-describedby="error ? `${id}-error` : undefined"
    />
    <small v-if="error" :id="`${id}-error`" class="error">{{ error }}</small>
  </div>
</template>
