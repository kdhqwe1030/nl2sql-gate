import { defineStore } from 'pinia'
import { getTenant } from '../api/tenant'
import { http } from '../api/http'
import { clearToken, getToken, setToken } from '../lib/tokenStorage'
import type { AuthResponse, LoginRequest, TenantResponse, UserResponse } from '../types/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken() as string | null,
    user: null as UserResponse | null,
    tenant: null as TenantResponse | null,
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
  },

  actions: {
    async login(payload: LoginRequest) {
      const res = await http.post<AuthResponse>('/api/auth/login', payload)
      this.token = res.accessToken
      setToken(res.accessToken)
      await this.loadProfile()
    },

    async loadProfile() {
      const [user, tenant] = await Promise.all([
        http.get<UserResponse>('/api/auth/me'),
        getTenant(),
      ])
      this.user = user
      this.tenant = tenant
    },

    // 새로고침 직후 토큰은 있지만 user/tenant 정보가 비어 있는 상태를 복구한다.
    // 토큰이 만료·위조됐으면 loadProfile이 401을 던지므로 그 자리에서 로그아웃 처리한다.
    async restoreSession() {
      if (!this.token || this.user) return
      try {
        await this.loadProfile()
      } catch {
        this.logout()
      }
    },

    logout() {
      this.token = null
      this.user = null
      this.tenant = null
      clearToken()
    },
  },
})
