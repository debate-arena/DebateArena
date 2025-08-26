<template>
  <div class="min-h-screen flex items-center justify-center bg-background">
    <div class="text-center space-y-4">
      <div class="animate-spin rounded-full h-8 w-8 border-2 border-primary border-t-transparent mx-auto"></div>
      <p class="text-sm text-muted-foreground">로그인 처리 중...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()

onMounted(async () => {
  try {
    // URL 파라미터에서 로그인 상태 확인
    const urlParams = new URLSearchParams(window.location.search)
    const status = urlParams.get('status')
    const nextPath = urlParams.get('next')
    
    if (status === 'success') {
      // 로그인 성공 처리
      await authStore.handleLoginSuccess()
      
      // 원래 페이지로 리다이렉트
      const targetPath = nextPath ? decodeURIComponent(nextPath) : '/'
      router.replace(targetPath)
    } else {
      // 로그인 실패 처리
      authStore.handleLoginError('로그인에 실패했습니다.')
      router.replace('/')
    }
  } catch (err) {
    console.error('로그인 처리 중 오류:', err)
    authStore.handleLoginError('로그인 처리 중 오류가 발생했습니다.')
    router.replace('/')
  }
})
</script> 