import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'
import { useAuthStore } from '@/stores/auth'

import './assets/main.css'

const app = createApp(App)

// Pinia
const pinia = createPinia()
app.use(pinia)

// Router
app.use(router)

// Element Plus with Chinese locale
app.use(ElementPlus, { locale: zhCn })

// Init auth state from localStorage
const authStore = useAuthStore()
authStore.initFromStorage()

app.mount('#app')
