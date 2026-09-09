import { defineStore } from 'pinia'
import { http } from '../api/http'
import { clearToken, getToken, setToken } from '../lib/tokenStorage'
import type { AuthResponse, LoginRequest, UserResponse } from '../types/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken() as string | null,
    user: null as UserResponse | null,
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
  },

  actions: {
    async login(payload: LoginRequest) {
      const res = await http.post<AuthResponse>('/api/auth/login', payload)
      this.token = res.accessToken
      setToken(res.accessToken)
      await this.fetchMe()
    },

    async fetchMe() {
      this.user = await http.get<UserResponse>('/api/auth/me')
    },

    // 새로고침 직후 토큰은 있지만 user 정보가 비어 있는 상태를 복구한다.
    // 토큰이 만료·위조됐으면 fetchMe가 401을 던지므로 그 자리에서 로그아웃 처리한다.
    async restoreSession() {
      if (!this.token || this.user) return
      try {
        await this.fetchMe()
      } catch {
        this.logout()
      }
    },

    logout() {
      this.token = null
      this.user = null
      clearToken()
    },
  },
})
