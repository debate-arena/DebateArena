<template>
  <Dialog v-model:open="isOpen" @update:open="$emit('close')">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle class="text-center">
          🔐 로그인이 필요한 서비스입니다
        </DialogTitle>
        <DialogDescription class="text-center">
          매칭을 시작하려면 로그인이 필요합니다.
        </DialogDescription>
      </DialogHeader>
      
      <div class="space-y-4">
        <div class="text-center space-y-2">
          <p class="text-sm text-muted-foreground">
            Google 계정으로 로그인하여 매칭을 시작하세요.
          </p>
        </div>
        
        <div class="space-y-2">
          <Button size="lg" class="w-full" @click="$emit('close')">확인</Button>
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
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const isOpen = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value)
})
</script>

