<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { getQueryLogs, getStats } from '../api/audit'
import type { LogStatus, QueryLogEntry, StatsResponse } from '../types/audit'

const STATUS_LABEL: Record<LogStatus, { label: string; chip: string }> = {
  SUCCESS: { label: '정상', chip: 'chip-success' },
  CLARIFY: { label: '확인 필요', chip: 'chip-warning' },
  DENIED: { label: '권한 없음', chip: 'chip-danger' },
  BLOCKED: { label: '차단', chip: 'chip-danger' },
  ERROR: { label: '오류', chip: 'chip-danger' },
}

function currentMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

function monthRange(month: string) {
  const [y, m] = month.split('-').map(Number)
  const from = new Date(Date.UTC(y, m - 1, 1)).toISOString()
  const to = new Date(Date.UTC(y, m, 1)).toISOString()
  return { from, to }
}

const selectedMonth = ref(currentMonth())
const stats = ref<StatsResponse | null>(null)
const logs = ref<QueryLogEntry[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const { from, to } = monthRange(selectedMonth.value)
    const [statsRes, logsRes] = await Promise.all([
      getStats(selectedMonth.value),
      // 더보기 없이 한 번에 다 보여줄 만큼 넉넉하게 (백엔드가 200으로 상한)
      getQueryLogs({ from, to, limit: 200 }),
    ])
    stats.value = statsRes
    logs.value = logsRes.items
  } finally {
    loading.value = false
  }
}

// 응답시간 3초 이내는 초록, 그 이상은 단계별로 경고
function latencyTone(ms: number | null) {
  if (ms === null) return ''
  if (ms <= 3000) return 'tone-success'
  if (ms <= 6000) return 'tone-warning'
  return 'tone-danger'
}

// 되묻은 비율 20% 미만이면 정상 범위(파란색), 그 이상은 단계별로 경고
function clarifyTone(rate: number) {
  if (rate < 0.2) return 'tone-info'
  if (rate < 0.4) return 'tone-warning'
  return 'tone-danger'
}

// 권한으로 막힌 질문 — 0건이면 문제 없음, 있으면 얼마나 있는지에 따라 경고
function deniedTone(count: number) {
  if (count === 0) return 'tone-success'
  if (count < 5) return 'tone-warning'
  return 'tone-danger'
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

function formatLatency(ms: number | null) {
  return ms === null ? '-' : `${(ms / 1000).toFixed(1)}초`
}

function formatPercent(rate: number) {
  return `${Math.round(rate * 100)}%`
}

onMounted(load)
watch(selectedMonth, load)
</script>

<template>
  <div class="pane">
    <div class="pane-inner">
      <div class="head-row">
        <div>
          <h1>사용 현황</h1>
          <p class="sub">
            누가 무엇을 물었는지 남습니다. 데이터를 바꾸지 않는 서비스라도 "누가 무엇을 봤는가"는 감사 대상입니다.
          </p>
        </div>
        <input v-model="selectedMonth" type="month" class="field month-input" />
      </div>

      <div v-if="stats" class="kpis">
        <div class="kpi">
          <div class="n">{{ stats.questionCount }}</div>
          <div class="l">이번 달 질문</div>
        </div>
        <div class="kpi">
          <div class="n" :class="latencyTone(stats.avgLatencyMs)">{{ formatLatency(stats.avgLatencyMs) }}</div>
          <div class="l">평균 응답 시간</div>
        </div>
        <div class="kpi">
          <div class="n" :class="deniedTone(stats.deniedCount)">{{ stats.deniedCount }}</div>
          <div class="l">권한으로 막힌 질문</div>
        </div>
        <div class="kpi">
          <div class="n" :class="clarifyTone(stats.clarifyRate)">{{ formatPercent(stats.clarifyRate) }}</div>
          <div class="l">되물은 비율</div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-h">
          조회 기록
          <span class="r">최근 순</span>
        </div>
        <div v-if="!loading && logs.length === 0" class="empty-state">
          <p>이번 달 조회 기록이 없습니다.</p>
        </div>
        <div v-else class="table-wrap">
          <div class="table-scroll">
            <table class="data-table">
              <thead>
                <tr>
                  <th>시각</th>
                  <th>사용자</th>
                  <th>질문</th>
                  <th>결과</th>
                  <th class="n">행수</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="log in logs" :key="log.id">
                  <td>{{ formatTime(log.createdAt) }}</td>
                  <td>{{ log.userName }} <span class="muted">{{ log.userRole }}</span></td>
                  <td class="question-cell">{{ log.question }}</td>
                  <td>
                    <span class="chip" :class="STATUS_LABEL[log.status].chip">
                      {{ STATUS_LABEL[log.status].label }}
                    </span>
                  </td>
                  <td class="n">{{ log.rowCount ?? '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <p class="note">이 화면에서 "질문하기"로 던진 질문은 실제로 위 기록에 쌓입니다.</p>
    </div>
  </div>
</template>

<style scoped>
.pane {
  padding: 26px 30px 60px;
  overflow-y: auto;
  height: 100%;
}
.pane-inner {
  max-width: 960px;
}
.head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
h1 {
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin: 0 0 4px;
  color: var(--text-1);
}
.sub {
  color: var(--text-2);
  margin: 0;
  max-width: 64ch;
}
.month-input {
  width: auto;
  flex-shrink: 0;
}
.empty-state {
  padding: 32px;
  text-align: center;
  color: var(--text-3);
}
.empty-state p {
  margin: 0;
}
.question-cell {
  max-width: 360px;
  white-space: normal;
}
.muted {
  color: var(--text-3);
  font-size: 12px;
}
.n.tone-success {
  color: var(--color-success);
}
.n.tone-info {
  color: var(--color-main);
}
.n.tone-warning {
  color: var(--color-warning);
}
.n.tone-danger {
  color: var(--color-danger);
}
</style>
