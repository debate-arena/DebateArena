<template>
  <div class="px-4 py-6 sm:px-0">
    <Card class="w-full max-w-2xl mx-auto">
      <CardHeader class="text-center">
        <CardTitle class="text-2xl sm:text-3xl lg:text-4xl font-bold">온라인 음성 토론에 오신 것을 환영합니다!</CardTitle>
        <CardDescription class="text-base sm:text-lg lg:text-xl">
          실시간 음성 토론방을 만들고, 다양한 주제로 자유롭게 토론해보세요.
        </CardDescription>
      </CardHeader>
      <CardContent class="space-y-6">
        <div class="space-y-2">
          <label class="text-sm font-medium text-foreground">토론방 이름</label>
          <Input 
            v-model="roomName" 
            placeholder="토론방 이름을 입력하세요"
            class="w-full"
          />
        </div>
        
        <div class="flex gap-4">
          <Dialog v-model:open="isDialogOpen">
            <DialogTrigger as-child>
              <Button @click="createRoom" class="flex-1 text-sm sm:text-base" :disabled="!roomName.trim()">
                토론방 만들기
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>토론방 생성</DialogTitle>
                <DialogDescription>
                  "{{ roomName }}" 토론방을 생성하시겠습니까?
                </DialogDescription>
              </DialogHeader>
              <div class="flex gap-3 justify-end">
                <Button variant="outline" @click="isDialogOpen = false">취소</Button>
                <Button @click="isDialogOpen = false">생성</Button>
              </div>
            </DialogContent>
          </Dialog>
          <Button variant="secondary" class="flex-1 text-sm sm:text-base">참여하기</Button>
        </div>
        
        <div class="flex gap-2 justify-center">
          <Badge variant="outline">실시간 음성</Badge>
          <Badge variant="outline">다양한 주제</Badge>
          <Badge variant="outline">자유로운 토론</Badge>
        </div>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Badge } from '@/components/ui/badge'

const roomName = ref('')
const isDialogOpen = ref(false)

const createRoom = () => {
  if (roomName.value.trim()) {
    isDialogOpen.value = true
  }
}
</script> 