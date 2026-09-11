import { createRouter, createWebHistory } from 'vue-router'
import AppShell from '../layouts/AppShell.vue'
import { useAuthStore } from '../stores/auth'
import AskView from '../views/AskView.vue'
import AuditView from '../views/AuditView.vue'
import DataView from '../views/DataView.vue'
import LoginView from '../views/LoginView.vue'
import SignupView from '../views/SignupView.vue'
import TermsView from '../views/TermsView.vue'
import UsersView from '../views/UsersView.vue'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    minRoleLevel?: number
  }
}

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/signup', name: 'signup', component: SignupView, meta: { public: true } },
    {
      path: '/',
      component: AppShell,
      redirect: { name: 'ask' },
      children: [
        { path: 'ask', name: 'ask', component: AskView },
        { path: 'terms', name: 'terms', component: TermsView },
        { path: 'data', name: 'data', component: DataView },
        { path: 'audit', name: 'audit', component: AuditView },
        { path: 'users', name: 'users', component: UsersView, meta: { minRoleLevel: 50 } },
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
  if ((to.name === 'login' || to.name === 'signup') && auth.isAuthenticated) {
    return { name: 'ask' }
  }
  if (to.meta.minRoleLevel !== undefined && (auth.user?.roleLevel ?? 0) < to.meta.minRoleLevel) {
    return { name: 'ask' }
  }
})
