<template>
  <Dialog v-model:open="isOpen" @update:open="$emit('close')">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle class="text-center">
          ⚠️ 주제 변경 예정
        </DialogTitle>
        <DialogDescription class="text-center">
          주제가 곧 변경됩니다.
        </DialogDescription>
      </DialogHeader>
      
      <div class="space-y-4">
        <div class="text-center space-y-2">
          <p class="text-sm text-muted-foreground">
            현재 주제로 매칭을 시작하시겠습니까?
          </p>
        </div>
        
        <div class="space-y-2">
          <Button size="lg" class="w-full" @click="$emit('confirm')">계속 진행</Button>
          <Button variant="outline" size="lg" class="w-full" @click="$emit('close')">취소</Button>
        </div>
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
  'confirm': []
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const isOpen = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value)
})
</script>

