<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Badge } from '@/components/ui/badge'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import LoginModal from '@/components/auth/LoginModal.vue'
import NicknameModal from '@/components/auth/NicknameModal.vue'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const authStore = useAuthStore()
const onlineUsers = ref(42)
const activeRooms = ref(8)
const isLoginModalOpen = ref(false)
const isNicknameModalOpen = ref(false)

const isDark = ref(false)

const toggleDarkMode = () => {
  isDark.value = !isDark.value
}

// DebateRoom 페이지인지 확인
const isDebateRoom = computed(() => route.name === 'DebateRoom')

const openLoginModal = () => {
  isLoginModalOpen.value = true
}

const handleLogout = async () => {
  await authStore.logout()
}

// 페이지 로드 시 인증 상태 확인
onMounted(async () => {
  console.log('🔍 App.vue 마운트 - 인증 상태 확인 시작')
  
  // 로그아웃 중이면 인증 상태 확인 건너뛰기
  if (authStore.isLoggingOut) {
    console.log('🔍 로그아웃 중 - 인증 상태 확인 건너뜀')
    return
  }
  
  await authStore.checkAuthStatus()
  console.log('🔍 인증 상태 확인 완료:', {
    isLoggedIn: authStore.isLoggedIn,
    hasNickname: authStore.hasNickname,
    userNickname: authStore.userNickname
  })
  
  // 닉네임 필수 체크
  checkNicknameRequired()
})

// 닉네임 설정 성공 처리
const handleNicknameSuccess = () => {
  console.log('닉네임 설정 완료')
  // 닉네임 설정 완료 후 모달 닫기
  isNicknameModalOpen.value = false
}

// 닉네임 필수 체크 함수
const checkNicknameRequired = () => {
  if (authStore.isLoggedIn && !authStore.hasNickname) {
    console.log('🔍 닉네임 필수 - 온보딩 모달 표시')
    isNicknameModalOpen.value = true
  }
}

// 닉네임 모달 변경 핸들러
const handleNicknameModalChange = (isOpen: boolean) => {
  // 모달이 닫힐 때 닉네임 필수 체크
  if (!isOpen) {
    console.log('🔍 닉네임 모달 닫힘 - 재체크')
    // 약간의 지연 후 재체크 (상태 업데이트 대기)
    setTimeout(() => {
      checkNicknameRequired()
    }, 200)
  }
}
</script>

<template>
  <div class="min-h-screen bg-background min-w-[1200px]" :class="{ 'dark': isDark }">
    <!-- 헤더 -->
    <header class="border-b bg-card">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <div class="flex items-center gap-4">
            <img src="/src/assets/images/icons/colosseum_icon.png" alt="Logo" class="h-8 w-8" />
            <span class="text-lg sm:text-xl lg:text-2xl font-semibold text-foreground">Debate Arena</span>
            <Badge variant="secondary" class="text-sm">{{ onlineUsers }}명 온라인</Badge>
          </div>
          <nav class="flex gap-2">
            <Button variant="ghost" size="sm" @click="toggleDarkMode">테마 변경</Button>
            <Button variant="ghost" size="sm">홈</Button>
            <Button variant="ghost" size="sm" @click="$router.push('/matching')">
              매칭
            </Button>
            <Button variant="ghost" size="sm">
              토론방
              <Badge variant="destructive" class="ml-2">{{ activeRooms }}</Badge>
            </Button>
            
            <!-- 로그인 상태에 따른 헤더 변경 -->
            <template v-if="authStore.isLoggedIn">
              <!-- 로그인된 상태: 사용자 프로필 -->
              <DropdownMenu>
                <DropdownMenuTrigger as-child>
                  <Button variant="ghost" size="sm" class="flex items-center gap-2">
                    <Avatar class="h-6 w-6">
                      <AvatarImage src="" />
                      <AvatarFallback>{{ authStore.userNickname.charAt(0) || 'U' }}</AvatarFallback>
                    </Avatar>
                    <span class="hidden sm:inline">{{ authStore.userNickname || '사용자' }}</span>
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent>
                  <DropdownMenuItem>프로필 설정</DropdownMenuItem>
                  <DropdownMenuItem>토론 기록</DropdownMenuItem>
                  <DropdownMenuItem>설정</DropdownMenuItem>
                  <DropdownMenuItem @click="handleLogout" class="text-destructive">
                    로그아웃
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </template>
            
            <template v-else>
              <!-- 로그인되지 않은 상태: 로그인 버튼 -->
              <Button 
                variant="outline" 
                size="sm" 
                @click="openLoginModal"
                class="ml-2"
              >
                로그인
              </Button>
            </template>
          </nav>
        </div>
      </div>
    </header>

    <!-- 메인 컨텐츠 -->
    <main class="">
      <div :class="isDebateRoom ? '' : 'px-4 py-6 sm:px-0'">
        <!-- 라우터 뷰 -->
        <router-view />
      </div>
    </main>

    <!-- 로그인 모달 -->
    <LoginModal v-model:open="isLoginModalOpen" />
    
    <!-- 닉네임 온보딩 모달 -->
    <NicknameModal 
      v-model:open="isNicknameModalOpen" 
      @success="handleNicknameSuccess"
      @update:open="handleNicknameModalChange"
    />
  </div>
</template>

<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}
</style>
