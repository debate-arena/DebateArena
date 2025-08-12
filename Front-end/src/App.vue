<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Badge } from '@/components/ui/badge'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import NicknameModal from '@/components/auth/NicknameModal.vue'
import { useAuthStore } from '@/store/auth'
import { useThemeStore } from '@/store/theme'
import { authAPI } from '@/api/auth'
import { config } from '@/config/env'

const route = useRoute()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const onlineUsers = ref(42)
const activeRooms = ref(8)
const isNicknameModalOpen = ref(false)
const isNicknameChangeModalOpen = ref(false) // 닉네임 변경용 모달

// DebateRoom 페이지인지 확인
const isDebateRoom = computed(() => route.name === 'DebateRoom')

// Google 로그인 바로 이동
const handleGoogleLogin = () => {
  window.location.href = authAPI.getOAuthUrl('google')
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
  console.log("base url>>>>>>>>>>", config.MAIN_API_URL);
  
  // 닉네임 필수 체크
  checkNicknameRequired()
})

// 닉네임 설정 성공 처리 (온보딩용)
const handleNicknameSuccess = () => {
  console.log('닉네임 설정 완료 (온보딩)')
  // 온보딩 모달 닫기
  isNicknameModalOpen.value = false
}

// 닉네임 변경 성공 처리 (변경용)
const handleNicknameChangeSuccess = () => {
  console.log('닉네임 변경 완료')
  // 변경 모달은 자동으로 닫힘 (별도 처리 불필요)
}

// 닉네임 필수 체크 함수
const checkNicknameRequired = () => {
  console.log('🔍 checkNicknameRequired 호출됨')
  console.log('🔍 로그인 상태:', authStore.isLoggedIn)
  console.log('🔍 닉네임 보유 상태:', authStore.hasNickname)
  
  if (authStore.isLoggedIn && !authStore.hasNickname) {
    console.log('🔍 닉네임 필수 - 온보딩 모달 표시')
    isNicknameModalOpen.value = true
  } else {
    console.log('🔍 닉네임 있음 또는 로그인 안됨 - 모달 열지 않음')
  }
}

// 닉네임 모달 변경 핸들러 (온보딩용)
const handleNicknameModalChange = (isOpen: boolean) => {
  // 모달이 닫힐 때 닉네임 필수 체크 (온보딩 모달만)
  if (!isOpen) {
    console.log('🔍 온보딩 닉네임 모달 닫힘 - 재체크')
    // 약간의 지연 후 재체크 (상태 업데이트 대기)
    setTimeout(() => {
      checkNicknameRequired()
    }, 200)
  }
}

// 닉네임 변경 모달 핸들러 (변경용 - 취소 가능)
const handleNicknameChangeModalChange = (isOpen: boolean) => {
  // 닉네임 변경 모달은 취소 가능하므로 강제 재열림 없음
  console.log('🔍 닉네임 변경 모달 상태 변경:', isOpen)
  console.log('🔍 현재 닉네임 상태:', authStore.hasNickname)
  // 변경 모달에서는 checkNicknameRequired 호출하지 않음
  // 모달이 닫히면 그냥 닫힘 (취소 가능)
}

// 닉네임 변경 모달 열기
const openNicknameChangeModal = () => {
  isNicknameChangeModalOpen.value = true
}

// Google 아이콘 동적 선택
const googleIcon = computed(() => {
  return themeStore.isDark 
    ? '/src/assets/images/icons/goggle-dark.png'
    : '/src/assets/images/icons/goggle-light.png'
})
</script>

<template>
  <div class="min-h-screen bg-background min-w-[1200px]" :class="{ 'dark': themeStore.isDark }">
    <!-- 헤더 -->
    <header class="border-b bg-card">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <div class="flex items-center gap-4">
            <img 
              src="/src/assets/images/icons/colosseum_icon.png" 
              alt="Logo" 
              class="h-8 w-8 cursor-pointer hover:opacity-80 transition-all duration-300"
              :class="{ 'brightness-0 invert': themeStore.isDark }"
              @click="$router.push('/')"
            />
            <span 
              class="text-lg sm:text-xl lg:text-2xl font-semibold text-foreground cursor-pointer hover:opacity-80 transition-opacity"
              @click="$router.push('/')"
            >
              Debate Arena
            </span>
          </div>
          <nav class="flex gap-2">
            <!-- 테마 변경 버튼 -->
            <Button variant="ghost" size="sm" @click="themeStore.toggleDarkMode" class="theme-toggle-btn">
              <span v-if="themeStore.isDark" class="dark-emoji">🌙</span>
              <span v-else class="light-emoji">☀️</span>
              <span v-if="themeStore.isDark">다크모드</span>
              <span v-else>라이트모드</span>
            </Button>
            
            <Button variant="ghost" size="sm" @click="$router.push('/')">홈</Button>
            <Button variant="ghost" size="sm" @click="$router.push('/matching')">
              매칭
            </Button>
            <Button variant="ghost" size="sm" @click="$router.push('/debate-room')">
              토론방
            </Button>
            
            <!-- 로그인 상태에 따른 헤더 변경 -->
            <template v-if="authStore.isLoggedIn">
              <!-- 로그인된 상태: 사용자 프로필 -->
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="sm" class="relative">
                    <span class="text-sm font-medium">{{ authStore.userNickname || '사용자' }}</span>
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" class="w-56 bg-popover text-popover-foreground border border-border">
                  <div class="flex items-center justify-start gap-2 p-2">
                    <div class="flex flex-col space-y-1">
                      <p class="text-sm font-medium leading-none" :class="themeStore.isDark ? 'text-white' : 'text-gray-900'">{{ authStore.userNickname || '사용자' }}</p>
                      <p class="text-xs leading-none" :class="themeStore.isDark ? 'text-gray-300' : 'text-gray-500'">{{ authStore.user?.email }}</p>
                    </div>
                  </div>
                  <DropdownMenuItem @click="openNicknameChangeModal" :class="themeStore.isDark ? 'text-white hover:bg-accent hover:text-accent-foreground' : 'text-gray-900 hover:bg-accent hover:text-accent-foreground'">
                    닉네임 변경
                  </DropdownMenuItem>
                  <DropdownMenuItem @click="handleLogout" :class="themeStore.isDark ? 'text-white hover:bg-accent hover:text-accent-foreground' : 'text-gray-900 hover:bg-accent hover:text-accent-foreground'">
                    로그아웃
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </template>
            
            <!-- 로그아웃된 상태: 로그인 버튼 -->
            <template v-else>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="outline" size="sm">
                    로그인
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" class="w-48 bg-popover text-popover-foreground border border-border">
                  <DropdownMenuItem @click="handleGoogleLogin" class="flex items-center gap-2" :class="themeStore.isDark ? 'text-white hover:bg-accent hover:text-accent-foreground' : 'text-gray-900 hover:bg-accent hover:text-accent-foreground'">
                    <img :src="googleIcon" class="w-4 h-4" alt="Google" />
                    Google로 로그인
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </template>
          </nav>
        </div>
      </div>
    </header>

    <!-- 메인 콘텐츠 -->
    <main class="flex-1">
      <RouterView />
    </main>

    <!-- 모달들 -->
    <NicknameModal 
      v-model:open="isNicknameModalOpen" 
      :is-onboarding="true"
      @success="handleNicknameSuccess"
      @update:open="handleNicknameModalChange"
    />
    <NicknameModal 
      v-model:open="isNicknameChangeModalOpen" 
      :is-onboarding="false"
      @success="handleNicknameChangeSuccess"
      @update:open="handleNicknameChangeModalChange"
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

/* 테마 변경 버튼 이모지 색상 조정 */
.theme-toggle-btn .dark-emoji {
  filter: brightness(0) invert(1); /* 흰색으로 변경 */
}

.theme-toggle-btn .light-emoji {
  filter: brightness(0); /* 검은색으로 변경 */
}
</style>
