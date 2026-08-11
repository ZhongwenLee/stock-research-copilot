import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const currentCompany = ref('')

  function setCurrentCompany(value: string) {
    currentCompany.value = value
  }

  return {
    currentCompany,
    setCurrentCompany,
  }
})
