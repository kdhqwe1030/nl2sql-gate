import { defineStore } from 'pinia'
import { getQueryLogs } from '../api/audit'
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
    historyLoaded: false,
  }),

  actions: {
    // 오늘 내가 던진 질문을 query_log에서 불러와 히스토리 rail에 미리 채운다.
    // params가 저장 안 돼 있어서 재실행은 못 하고, 질문/상태/executedSql만 보여주는 "가벼운 버전".
    async loadHistory() {
      if (this.historyLoaded) return
      this.historyLoaded = true

      const auth = useAuthStore()
      if (!auth.user) return

      const startOfToday = new Date()
      startOfToday.setHours(0, 0, 0, 0)

      try {
        const page = await getQueryLogs({
          userId: auth.user.id,
          from: startOfToday.toISOString(),
          limit: 100,
        })
        const loaded: HistoryEntry[] = page.items
          .slice()
          .reverse() // API는 최신순 → 오래된 순으로 뒤집어서 앞쪽에 쌓는다
          .map((log) => ({
            id: log.id,
            question: log.question,
            loading: false,
            open: false,
            answer: {
              type: 'LOGGED',
              status: log.status,
              executedSql: log.executedSql,
              rowCount: log.rowCount,
            },
          }))
        this.history = [...loaded, ...this.history]
      } catch {
        // 히스토리를 못 불러와도 질문하기 자체는 정상 동작해야 하니 조용히 무시한다.
      }
    },

    async ask(question: string) {
      const id = `q${++seq}`
      this.history.forEach((e) => (e.open = false))
      this.history.push({ id, question, loading: true, open: true, answer: null })
      this.currentId = id

      // push한 원본 객체가 아니라 배열 안의 reactive proxy를 다시 꺼내 써야 한다 —
      // 원본을 계속 들고 mutate하면 값은 바뀌어도 Vue가 감지를 못 해 화면이 안 바뀐다.
      const entry = this.history[this.history.length - 1]

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
