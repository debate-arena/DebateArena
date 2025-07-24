<script setup lang="ts">
import { Button } from '@/components/ui/button'
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-slate-100 to-slate-200 flex flex-col">
    <!-- 상단 네비게이션 -->
    <header class="w-full h-16 flex items-center justify-between px-8 bg-white/80 shadow-sm sticky top-0 z-10">
      <div class="flex items-center gap-2">
        <img src="/vite.svg" alt="Logo" class="h-8 w-8" />
        <span class="font-bold text-xl tracking-tight text-slate-800">Debate Arena</span>
      </div>
      <nav class="flex gap-4">
        <Button variant="ghost">홈</Button>
        <Button variant="ghost">토론방</Button>
        <Button variant="ghost">마이페이지</Button>
      </nav>
    </header>

    <!-- 메인 컨텐츠 -->
    <main class="flex-1 flex flex-col items-center justify-center py-16">
      <div class="bg-white rounded-2xl shadow-xl p-10 w-full max-w-md flex flex-col items-center gap-6">
        <h1 class="text-3xl font-bold text-slate-800 mb-2">온라인 음성 토론에 오신 것을 환영합니다!</h1>
        <p class="text-slate-600 text-center mb-4">실시간 음성 토론방을 만들고, 다양한 주제로 자유롭게 토론해보세요.</p>
        <div class="flex gap-4 w-full">
          <Button class="flex-1">토론방 만들기</Button>
          <Button variant="secondary" class="flex-1">참여하기</Button>
        </div>
      </div>
    </main>

    <!-- 푸터 -->
    <footer class="h-16 flex items-center justify-center text-slate-400 text-sm bg-transparent">
      © 2024 Debate Arena. Powered by Vue, shadcn-vue, TailwindCSS.
    </footer>
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
