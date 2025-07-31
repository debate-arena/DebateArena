import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(false)

  const toggleDarkMode = () => {
    isDark.value = !isDark.value
  }

  const setDarkMode = (dark: boolean) => {
    isDark.value = dark
  }

  return {
    isDark,
    toggleDarkMode,
    setDarkMode
  }
}) 