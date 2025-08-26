import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  // 로컬 스토리지에서 테마 상태 복원
  const savedTheme = localStorage.getItem('theme')
  const isDark = ref(savedTheme === 'dark' || false)

  const toggleDarkMode = () => {
    isDark.value = !isDark.value
    // 로컬 스토리지에 저장
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  }

  const setDarkMode = (dark: boolean) => {
    isDark.value = dark
    // 로컬 스토리지에 저장
    localStorage.setItem('theme', dark ? 'dark' : 'light')
  }

  return { isDark, toggleDarkMode, setDarkMode }
}) 