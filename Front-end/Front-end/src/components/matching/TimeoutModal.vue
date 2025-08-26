<template>
  <Dialog v-model:open="isOpen" @update:open="$emit('close')">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle class="text-center">⏰ 매칭 타임아웃</DialogTitle>
        <DialogDescription class="text-center">
          매칭 시간이 초과되었습니다. 다시 시도해주세요.
        </DialogDescription>
      </DialogHeader>
      <div class="flex gap-2">
        <Button @click="$emit('close')" class="flex-1">
          확인
        </Button>
      </div>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'

interface Props {
  open: boolean
}

interface Emits {
  'update:open': [value: boolean]
  'close': []
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const isOpen = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value)
})
</script>
