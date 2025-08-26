<template>
  <Dialog v-model:open="isOpen" @update:open="$emit('close')">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle class="text-center">
          ⏰ 주제 변경으로 인한 매칭 취소
        </DialogTitle>
        <DialogDescription class="text-center">
          주제가 변경되어 매칭이 자동으로 취소되었습니다.
        </DialogDescription>
      </DialogHeader>
      
      <div class="space-y-4">
        <div class="text-center space-y-2">
          <p class="text-sm text-muted-foreground">
            새로운 주제로 다시 매칭하시겠습니까?
          </p>
        </div>
        
        <div class="space-y-2">
          <Button 
            variant="outline" 
            size="lg" 
            class="w-full" 
            @click="$emit('close')"
          >
            취소
          </Button>
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

