<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getSchemaCatalog } from '../api/schemaCatalog'
import type { SchemaCatalogTable } from '../types/schemaCatalog'

const tables = ref<SchemaCatalogTable[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    tables.value = await getSchemaCatalog()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="pane">
    <div class="pane-inner">
      <h1>데이터 연결</h1>
      <p class="sub">
        회사가 바뀌어도 새로 만드는 화면은 없습니다. 지금 연결된 테이블과 컬럼을 여기서 확인합니다.
      </p>

      <div v-for="t in tables" :key="t.tableName" class="panel table-panel">
        <div class="panel-h">
          {{ t.displayName }}
          <span class="table-name">{{ t.tableName }}</span>
          <span v-if="t.description" class="r desc">{{ t.description }}</span>
        </div>
        <div class="table-scroll">
          <table class="data-table">
            <tbody>
              <tr v-for="c in t.columns" :key="c.columnName">
                <td>{{ c.displayName }} <span class="muted">{{ c.columnName }}</span></td>
                <td class="lv-cell">
                  <span class="lv" :class="{ mgr: c.minRoleLevel >= 50 }">
                    {{ c.minRoleLevel >= 50 ? '매니저 이상' : '전 직원' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <p class="note">
        공개하지 않은 컬럼은 질문 화면(LLM 프롬프트)에 존재 자체가 나타나지 않습니다. 조회를 막는 것과, 그런 데이터가 있다는 사실을 감추는 것은 다릅니다.
        지금은 role 정책이 코드에 고정돼 있어 이 화면은 조회 전용입니다.
      </p>
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
h1 {
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin: 0 0 4px;
  color: var(--text-1);
}
.sub {
  color: var(--text-2);
  margin: 0 0 20px;
  max-width: 64ch;
}

.table-panel {
  margin-bottom: 14px;
}
/* 전역 .panel-h(회색 톤)보다 브랜드색을 써서 표 헤더처럼 밋밋해 보이지 않게 */
.table-panel .panel-h {
  background: var(--color-main-soft);
  color: var(--color-main);
  font-weight: 700;
  border-bottom-color: transparent;
  flex-wrap: wrap;
  row-gap: 4px;
}
.table-name {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  font-weight: 400;
  color: var(--color-main);
  opacity: 0.7;
}
.desc {
  font-size: 12px;
  font-weight: 400;
  color: var(--color-main);
  opacity: 0.75;
}
.muted {
  color: var(--text-3);
  font-size: 12px;
}
.lv-cell {
  text-align: right;
  white-space: nowrap;
}
</style>
