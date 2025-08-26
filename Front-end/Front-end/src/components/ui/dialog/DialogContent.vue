<script setup lang="ts">
import { computed } from 'vue'
import type { HTMLAttributes } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { DialogContent, type DialogContentProps } from 'reka-ui'
import { cn } from '@/lib/utils'
import DialogOverlay from './DialogOverlay.vue'

const props = defineProps<DialogContentProps & { class?: HTMLAttributes['class'] }>()

const delegatedProps = reactiveOmit(props, 'class')

// no-backdrop 클래스가 있는지 확인
const hasNoBackdrop = computed(() => {
  return props.class && typeof props.class === 'string' && props.class.includes('no-backdrop')
})
</script>

<template>
  <DialogContent
    data-slot="dialog-content"
    v-bind="delegatedProps"
    :class="cn('fixed left-[50%] top-[50%] z-50 grid w-full max-w-lg translate-x-[-50%] translate-y-[-50%] gap-4 border bg-background p-6 shadow-lg duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[state=closed]:slide-out-to-left-1/2 data-[state=closed]:slide-out-to-top-[48%] data-[state=open]:slide-in-from-left-1/2 data-[state=open]:slide-in-from-top-[48%] sm:rounded-lg', props.class)"
  >
    <slot />
  </DialogContent>
  
  <!-- backdrop 제거: no-backdrop 클래스가 있을 때는 backdrop을 렌더링하지 않음 -->
  <DialogOverlay v-if="!hasNoBackdrop" />
</template>
