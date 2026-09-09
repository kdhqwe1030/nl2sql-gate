<script setup lang="ts">
import { computed } from 'vue'
import { useAskStore } from '../../stores/ask'
import type { HistoryEntry } from '../../types/query'

const props = defineProps<{ entry: HistoryEntry }>()
const store = useAskStore()

const STATUS_LABEL: Record<string, string> = {
  DENIED: '권한 없음',
  BLOCKED: '차단됨',
  ERROR: '오류',
}

// 판별 유니온을 템플릿에서 바로 좁히면 vue-tsc가 놓치는 경우가 있어
// 갈래별로 computed를 하나씩 둬서 타입을 확정한다.
const resultAnswer = computed(() =>
  props.entry.answer?.type === 'RESULT' ? props.entry.answer : null,
)
const clarifyAnswer = computed(() =>
  props.entry.answer?.type === 'CLARIFY' ? props.entry.answer : null,
)
const metaAnswer = computed(() => (props.entry.answer?.type === 'META' ? props.entry.answer : null))
const errorAnswer = computed(() =>
  props.entry.answer?.type === 'ERROR' ? props.entry.answer : null,
)

const summary = computed(() => {
  if (props.entry.loading || !props.entry.answer) return { text: '처리 중', tone: '' }
  if (resultAnswer.value) return { text: `${resultAnswer.value.result.totalCount}건`, tone: 'success' }
  if (clarifyAnswer.value) return { text: '확인 필요', tone: 'warning' }
  if (metaAnswer.value) return { text: '데이터 목록', tone: '' }
  if (errorAnswer.value) return { text: STATUS_LABEL[errorAnswer.value.status], tone: 'danger' }
  return { text: '—', tone: '' }
})

function toggle() {
  store.toggle(props.entry.id)
}

function formatCell(value: unknown) {
  return typeof value === 'number' ? value.toLocaleString() : String(value ?? '')
}
</script>

<template>
  <article class="qcard" :class="{ open: entry.open }">
    <button class="qhead" :aria-expanded="entry.open" @click="toggle">
      <h3>{{ entry.question }}</h3>
      <span class="qsum" :class="summary.tone">{{ summary.text }}</span>
      <span class="caret">{{ entry.open ? '▾' : '▸' }}</span>
    </button>

    <div v-if="entry.open" class="qbody">
      <div v-if="entry.loading" class="loading">
        <span class="spinner" />
        <span>답변을 만드는 중입니다...</span>
      </div>

      <template v-else>
        <template v-if="resultAnswer">
          <div class="chips">
            <span class="chip chip-success"><span class="dot" />읽기만 함</span>
            <span class="chip chip-success"><span class="dot" />권한 안의 데이터</span>
            <span v-if="resultAnswer.result.truncated" class="chip chip-warning">
              <span class="dot" />결과가 잘렸습니다
            </span>
          </div>
          <p class="count">
            {{ resultAnswer.result.totalCount }}건
            <span>정렬 · 필터는 다음 단계에서 지원</span>
          </p>
          <div class="table-wrap">
            <div class="table-scroll">
              <table class="data-table">
                <thead>
                  <tr>
                    <th v-for="c in resultAnswer.result.columns" :key="c">{{ c }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, i) in resultAnswer.result.rows" :key="i">
                    <td v-for="(cell, j) in row" :key="j">{{ formatCell(cell) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <details class="sql">
            <summary>실행된 조회 내용 보기</summary>
            <pre>{{ resultAnswer.result.executedSql }}</pre>
          </details>
        </template>

        <template v-else-if="clarifyAnswer">
          <div class="chips">
            <span class="chip chip-warning"><span class="dot" />확인이 필요합니다</span>
          </div>
          <div class="card card-warning">
            <p>{{ clarifyAnswer.clarify }}</p>
          </div>
        </template>

        <template v-else-if="metaAnswer">
          <div class="chips">
            <span class="chip chip-muted">데이터 목록</span>
          </div>
          <div class="card">
            <h4>지금 권한으로 볼 수 있는 데이터</h4>
            <ul class="meta-list">
              <li v-for="t in metaAnswer.tables" :key="t.name">
                <b>{{ t.name }}</b>
                <span>{{ t.columns.join(', ') }}</span>
              </li>
            </ul>
          </div>
        </template>

        <template v-else-if="errorAnswer">
          <div class="chips">
            <span class="chip chip-danger">
              <span class="dot" />{{ STATUS_LABEL[errorAnswer.status] }}
            </span>
          </div>
          <div class="card card-danger">
            <h4>{{ STATUS_LABEL[errorAnswer.status] }}</h4>
            <p>{{ errorAnswer.message }}</p>
          </div>
        </template>
      </template>
    </div>
  </article>
</template>

<style scoped>
.qcard {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  margin-bottom: 10px;
  overflow: hidden;
}
.qcard.open {
  box-shadow: 0 1px 2px rgba(16, 28, 37, 0.05);
}
.qhead {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  text-align: left;
  padding: 14px 18px;
}
.qhead:hover {
  background: var(--color-sunk);
}
.qhead h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  letter-spacing: -0.01em;
  color: var(--text-1);
}
.qsum {
  font-size: 12.5px;
  color: var(--text-3);
  white-space: nowrap;
  flex-shrink: 0;
}
.qsum.success {
  color: var(--color-success);
}
.qsum.warning {
  color: var(--color-warning);
}
.qsum.danger {
  color: var(--color-danger);
}
.caret {
  color: var(--text-3);
  font-size: 10px;
  width: 10px;
  text-align: center;
  flex-shrink: 0;
}
.qcard.open .qhead {
  border-bottom: 1px solid var(--color-border);
}
.qcard.open .qhead:hover {
  background: transparent;
}
.qbody {
  padding: 16px 18px 18px;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  margin-bottom: 16px;
}
.count {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 10px;
  color: var(--text-1);
}
.count span {
  font-weight: 400;
  color: var(--text-3);
  font-size: 13px;
  margin-left: 8px;
}
.loading {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--text-2);
  font-size: 13px;
}
.meta-list {
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
  border-top: 1px solid var(--color-border);
}
.meta-list li {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border);
  font-size: 13px;
}
.meta-list b {
  font-weight: 600;
  width: 110px;
  flex-shrink: 0;
  color: var(--text-1);
}
.meta-list span {
  color: var(--text-2);
}
details.sql {
  margin-top: 12px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 6px;
}
details.sql summary {
  padding: 10px 14px;
  color: var(--text-2);
  font-size: 13px;
  cursor: pointer;
  list-style: none;
}
details.sql summary::-webkit-details-marker {
  display: none;
}
details.sql summary::before {
  content: '▸ ';
  color: var(--text-3);
}
details.sql[open] summary::before {
  content: '▾ ';
}
details.sql pre {
  margin: 0;
  padding: 0 14px 14px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  line-height: 1.75;
  color: var(--text-2);
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
