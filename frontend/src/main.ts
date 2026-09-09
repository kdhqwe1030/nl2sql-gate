import { createPinia } from 'pinia'
import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import { useAuthStore } from './stores/auth'
import './style.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)

// 토큰이 남아있으면 /api/auth/me로 유저 정보를 복구한 뒤에 라우팅을 시작한다
// (이게 끝나기 전에 마운트하면 최초 렌더에서 로그인 여부를 잘못 판단할 수 있다).
useAuthStore()
  .restoreSession()
  .finally(() => app.mount('#app'))
