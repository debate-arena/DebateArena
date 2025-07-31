<template>
  <Dialog v-model:open="isOpen" @update:open="handleClose" :escape-key-down="false">
    <DialogContent class="sm:max-w-md no-backdrop" :close-button="false" :close-on-overlay-click="false">
      <DialogHeader>
        <DialogTitle class="text-center">닉네임 설정</DialogTitle>
        <DialogDescription class="text-center">
          토론에서 사용할 닉네임을 설정해주세요
        </DialogDescription>
      </DialogHeader>
      
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <!-- 닉네임 입력 -->
        <div class="space-y-2">
          <Label for="nickname">닉네임</Label>
          <Input
            id="nickname"
            v-model="nickname"
            type="text"
            placeholder="2-16자 사이로 입력해주세요"
            :disabled="loading"
            @input="handleNicknameChange"
            class="w-full"
          />
          
          <!-- 유효성 검사 메시지 -->
          <div v-if="validationMessage" class="text-sm" :class="validationType === 'error' ? 'text-destructive' : 'text-muted-foreground'">
            {{ validationMessage }}
          </div>
          
          <!-- 중복 확인 결과 -->
          <div v-if="checkResult" class="text-sm" :class="checkResult.available ? 'text-green-600' : 'text-destructive'">
            {{ checkResult.message }}
          </div>
        </div>
        
        <!-- 버튼 -->
        <div class="flex gap-2">
          <Button
            type="submit"
            class="flex-1"
            :disabled="loading || !isValid || !isAvailable"
          >
            <div v-if="loading" class="animate-spin rounded-full h-4 w-4 border-2 border-current border-t-transparent mr-2"></div>
            {{ loading ? '처리 중...' : '설정 완료' }}
          </Button>
        </div>
        
        <!-- 에러 메시지 -->
        <div v-if="error" class="text-sm text-destructive text-center">
          {{ error }}
        </div>
      </form>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { useAuthStore } from '@/store/auth'

interface Props {
  open: boolean
  isOnboarding?: boolean // 온보딩 모달인지 여부
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const authStore = useAuthStore()
const nickname = ref('')
const loading = ref(false)
const error = ref('')
const checkResult = ref<{ available: boolean; message: string } | null>(null)

const isOpen = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value)
})

// 유효성 검사
const isValid = computed(() => {
  const value = nickname.value.trim()
  if (value.length < 2) return false
  if (value.length > 16) return false
  // 특수문자 제한 (한글, 영문, 숫자만 허용)
  const validPattern = /^[가-힣a-zA-Z0-9]+$/
  return validPattern.test(value)
})

const isAvailable = computed(() => {
  return checkResult.value?.available === true
})

const validationMessage = computed(() => {
  const value = nickname.value.trim()
  
  if (value.length === 0) return ''
  if (value.length < 2) return '닉네임은 2자 이상이어야 합니다.'
  if (value.length > 16) return '닉네임은 16자 이하여야 합니다.'
  
  // 특수문자 검사
  const validPattern = /^[가-힣a-zA-Z0-9]+$/
  if (!validPattern.test(value)) {
    return '한글, 영문, 숫자만 사용 가능합니다.'
  }
  
  return ''
})

const validationType = computed(() => {
  return validationMessage.value ? 'error' : 'info'
})

// 닉네임 변경 시 중복 확인
let checkTimeout: number | null = null

const handleNicknameChange = () => {
  // 이전 타이머 취소
  if (checkTimeout) {
    clearTimeout(checkTimeout)
  }
  
  // 500ms 후 중복 확인
  checkTimeout = window.setTimeout(async () => {
    await checkNickname()
  }, 500)
}

const checkNickname = async () => {
  const value = nickname.value.trim()
  
  if (!isValid.value || value.length === 0) {
    checkResult.value = null
    return
  }
  
  try {
    const available = await authStore.checkNickname(value)
    checkResult.value = {
      available,
      message: available ? '사용 가능한 닉네임입니다.' : '이미 사용 중인 닉네임입니다.'
    }
  } catch (err) {
    console.error('닉네임 중복 확인 오류:', err)
    checkResult.value = {
      available: false,
      message: '중복 확인 중 오류가 발생했습니다.'
    }
  }
}

// 폼 제출
const handleSubmit = async () => {
  if (!isValid.value || !isAvailable.value) return
  
  loading.value = true
  error.value = ''
  
  try {
    const success = await authStore.saveNickname(nickname.value.trim())
    
    if (success) {
      // 성공 시 모달 닫기
      isOpen.value = false
      emit('success')
    } else {
      error.value = '닉네임 설정에 실패했습니다.'
    }
  } catch (err) {
    console.error('닉네임 저장 오류:', err)
    error.value = '닉네임 설정 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

const handleClose = (value: boolean) => {
  // 모달이 열릴 때 정리
  if (value) {
    nickname.value = ''
    error.value = ''
    checkResult.value = null
    loading.value = false
    
    if (checkTimeout) {
      clearTimeout(checkTimeout)
      checkTimeout = null
    }
  }
  
  // 모달이 닫히려고 할 때
  if (!value) {
    console.log('🔍 닉네임 모달 닫기 시도')
    
    // 온보딩 모달이고 닉네임이 없는 경우에만 다시 열기
    if (props.isOnboarding && !authStore.hasNickname) {
      console.log('🔍 온보딩 모달 - 닉네임 없음, 다시 열기')
      setTimeout(() => {
        emit('update:open', true)
      }, 100)
      return
    }
    
    // 그 외의 경우는 그냥 닫기
    console.log('🔍 모달 그냥 닫기')
    emit('update:open', value)
    return
  }
  
  emit('update:open', value)
}

// 컴포넌트 언마운트 시 정리
watch(() => props.open, (newValue) => {
  if (!newValue && checkTimeout) {
    clearTimeout(checkTimeout)
    checkTimeout = null
  }
})
</script> 