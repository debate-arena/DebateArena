<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Badge } from '@/components/ui/badge'

const onlineUsers = ref(42)
const activeRooms = ref(8)
</script>

<template>
  <div class="min-h-screen bg-background min-w-[1200px]">
    <!-- 헤더 -->
    <header class="border-b bg-card">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <div class="flex items-center gap-4">
            <img src="/vite.svg" alt="Logo" class="h-8 w-8" />
            <span class="text-lg sm:text-xl lg:text-2xl font-semibold text-foreground">Debate Arena</span>
            <Badge variant="secondary" class="text-sm">{{ onlineUsers }}명 온라인</Badge>
          </div>
          <nav class="flex gap-2">
            <Button variant="ghost" size="sm">홈</Button>
            <Button variant="ghost" size="sm" @click="$router.push('/matching')">
              매칭
            </Button>
            <Button variant="ghost" size="sm">
              토론방
              <Badge variant="destructive" class="ml-2">{{ activeRooms }}</Badge>
            </Button>
            <DropdownMenu>
              <DropdownMenuTrigger as-child>
                <Button variant="ghost" size="sm">마이페이지</Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent>
                <DropdownMenuItem>프로필 설정</DropdownMenuItem>
                <DropdownMenuItem>토론 기록</DropdownMenuItem>
                <DropdownMenuItem>설정</DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </nav>
        </div>
      </div>
    </header>

    <!-- 메인 컨텐츠 -->
    <main class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
      <div class="px-4 py-6 sm:px-0">
        <!-- 라우터 뷰 -->
        <router-view />
      </div>
    </main>
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
