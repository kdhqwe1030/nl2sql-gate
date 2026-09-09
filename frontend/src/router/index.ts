import { createRouter, createWebHistory } from 'vue-router'
import AppShell from '../layouts/AppShell.vue'
import { useAuthStore } from '../stores/auth'
import AskView from '../views/AskView.vue'
import AuditView from '../views/AuditView.vue'
import ComingSoonView from '../views/ComingSoonView.vue'
import LoginView from '../views/LoginView.vue'
import TermsView from '../views/TermsView.vue'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
  }
}

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    {
      path: '/',
      component: AppShell,
      redirect: { name: 'ask' },
      children: [
        { path: 'ask', name: 'ask', component: AskView },
        { path: 'terms', name: 'terms', component: TermsView },
        {
          path: 'data',
          name: 'data',
          component: ComingSoonView,
          props: {
            title: '데이터 연결',
            description: '연결된 데이터베이스와 공개 범위를 관리하는 화면입니다. 아직 준비 중입니다.',
          },
        },
        { path: 'audit', name: 'audit', component: AuditView },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login' }
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'ask' }
  }
})
