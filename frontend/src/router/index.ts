import { createRouter, createWebHistory } from 'vue-router'
import AppShell from '../layouts/AppShell.vue'
import { useAuthStore } from '../stores/auth'
import AskView from '../views/AskView.vue'
import ComingSoonView from '../views/ComingSoonView.vue'
import LoginView from '../views/LoginView.vue'

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
        {
          path: 'terms',
          name: 'terms',
          component: ComingSoonView,
          props: {
            title: '업무 용어',
            description: '회사 용어를 등록·관리하는 화면입니다. 아직 준비 중입니다.',
          },
        },
        {
          path: 'data',
          name: 'data',
          component: ComingSoonView,
          props: {
            title: '데이터 연결',
            description: '연결된 데이터베이스와 공개 범위를 관리하는 화면입니다. 아직 준비 중입니다.',
          },
        },
        {
          path: 'audit',
          name: 'audit',
          component: ComingSoonView,
          props: {
            title: '사용 현황',
            description: '누가 무엇을 물었는지 조회 기록을 보는 화면입니다. 아직 준비 중입니다.',
          },
        },
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
