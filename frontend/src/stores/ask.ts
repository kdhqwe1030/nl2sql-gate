import { defineStore } from 'pinia'
import { ApiError } from '../api/http'
import { getSchema, postQuery } from '../api/query'
import type { HistoryEntry } from '../types/query'
import { useAuthStore } from './auth'

// 백엔드엔 의도 라우팅이 없어서(기획서 5.2 미구현), 메타 질문은 여기서 감지해
// /api/query 대신 /api/schema로 보낸다. 프로토타입 resolve()의 동일한 정규식.
const META_PATTERN = /무슨데이터|무슨테이블|어떤데이터|뭐볼수|볼수있어|목록/

let seq = 0

export const useAskStore = defineStore('ask', {
  state: () => ({
    history: [] as HistoryEntry[],
    currentId: null as string | null,
  }),

  actions: {
    async ask(question: string) {
      const id = `q${++seq}`
      const entry: HistoryEntry = { id, question, loading: true, open: true, answer: null }
      this.history.forEach((e) => (e.open = false))
      this.history.push(entry)
      this.currentId = id

      try {
        if (META_PATTERN.test(question.replace(/\s/g, ''))) {
          const schema = await getSchema()
          entry.answer = { type: 'META', tables: schema.tables }
        } else {
          entry.answer = await postQuery(question)
        }
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) {
          // 세션이 끊긴 상태 — 토큰만 비워두면 다음 화면 전환에서 라우터 가드가 로그인으로 보낸다.
          useAuthStore().logout()
          entry.answer = {
            type: 'ERROR',
            status: 'ERROR',
            message: '로그인이 만료되었습니다. 다시 로그인해주세요.',
          }
        } else {
          entry.answer = {
            type: 'ERROR',
            status: 'ERROR',
            message: e instanceof ApiError ? e.message : '요청 중 오류가 발생했습니다.',
          }
        }
      } finally {
        entry.loading = false
      }
    },

    select(id: string) {
      this.history.forEach((e) => (e.open = e.id === id))
      this.currentId = id
    },

    toggle(id: string) {
      const entry = this.history.find((e) => e.id === id)
      if (!entry) return
      entry.open = !entry.open
      if (entry.open) this.currentId = id
    },
  },
})
