<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createGlossaryTerm, deleteGlossaryTerm, getGlossaryTerms, updateGlossaryTerm } from '../api/glossary'
import { ApiError } from '../api/http'
import { useAskStore } from '../stores/ask'
import { useAuthStore } from '../stores/auth'
import type { GlossaryTerm, GlossaryTermInput } from '../types/glossary'

const auth = useAuthStore()
const router = useRouter()
const askStore = useAskStore()

const ROLE_LEVELS = [
  { level: 10, label: 'STAFF (전 직원)' },
  { level: 50, label: 'MANAGER 이상' },
  { level: 100, label: 'ADMIN 전용' },
]

function emptyForm(): GlossaryTermInput {
  return { term: '', aliases: [], definition: '', sqlHint: null, relatedTables: [], minRoleLevel: 10, enabled: true }
}

const terms = ref<GlossaryTerm[]>([])
const selectedId = ref<string | null>(null)
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')

const form = reactive<GlossaryTermInput>(emptyForm())
const aliasesText = ref('')
const relatedTablesText = ref('')

const selectedTerm = computed(() => terms.value.find((t) => t.id === selectedId.value) ?? null)
const isNew = computed(() => selectedId.value === null)

// 수정이면 "기존 등급"과 "새로 고르려는 등급" 중 더 높은 쪽까지 권한이 있어야 한다 —
// 백엔드(GlossaryRepository.update)와 같은 규칙.
const requiredLevel = computed(() => Math.max(selectedTerm.value?.minRoleLevel ?? 0, form.minRoleLevel))
const canManage = computed(() => (auth.user?.roleLevel ?? 0) >= requiredLevel.value)

async function load() {
  loading.value = true
  try {
    terms.value = await getGlossaryTerms()
    if (terms.value.length > 0 && selectedId.value === null) {
      selectTerm(terms.value[0].id)
    }
  } finally {
    loading.value = false
  }
}

function selectTerm(id: string) {
  const t = terms.value.find((x) => x.id === id)
  if (!t) return
  selectedId.value = id
  form.term = t.term
  form.aliases = t.aliases
  form.definition = t.definition
  form.sqlHint = t.sqlHint
  form.relatedTables = t.relatedTables
  form.minRoleLevel = t.minRoleLevel
  form.enabled = t.enabled
  aliasesText.value = t.aliases.join(', ')
  relatedTablesText.value = t.relatedTables.join(', ')
  errorMessage.value = ''
}

function newTerm() {
  selectedId.value = null
  Object.assign(form, emptyForm())
  aliasesText.value = ''
  relatedTablesText.value = ''
  errorMessage.value = ''
}

function syncListsFromText() {
  form.aliases = aliasesText.value.split(',').map((s) => s.trim()).filter(Boolean)
  form.relatedTables = relatedTablesText.value.split(',').map((s) => s.trim()).filter(Boolean)
}

async function save() {
  syncListsFromText()
  if (!form.term.trim() || !form.definition.trim()) {
    errorMessage.value = '용어와 정의는 필수입니다.'
    return
  }
  saving.value = true
  errorMessage.value = ''
  try {
    if (isNew.value) {
      const created = await createGlossaryTerm(form)
      terms.value = [...terms.value, created].sort((a, b) => a.term.localeCompare(b.term, 'ko'))
      selectedId.value = created.id
    } else if (selectedId.value) {
      const updated = await updateGlossaryTerm(selectedId.value, form)
      terms.value = terms.value.map((t) => (t.id === updated.id ? updated : t))
    }
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '저장 중 오류가 발생했습니다.'
  } finally {
    saving.value = false
  }
}

async function remove() {
  if (!selectedId.value) return
  if (!confirm(`"${form.term}" 용어를 삭제할까요?`)) return
  saving.value = true
  errorMessage.value = ''
  try {
    await deleteGlossaryTerm(selectedId.value)
    terms.value = terms.value.filter((t) => t.id !== selectedId.value)
    newTerm()
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '삭제 중 오류가 발생했습니다.'
  } finally {
    saving.value = false
  }
}

function tryTerm() {
  if (!selectedTerm.value) return
  router.push({ name: 'ask' })
  askStore.ask(`${selectedTerm.value.term} 알려줘`)
}

onMounted(load)
</script>

<template>
  <div class="pane">
    <div class="pane-inner">
      <h1>업무 용어</h1>
      <p class="sub">
        회사에서 쓰는 말을 등록해 두면 질문할 때 그 기준으로 계산합니다. 여기 적힌 내용이 답변 정확도를 그대로 결정합니다.
      </p>

      <div class="split">
        <div class="col-list">
          <div class="panel">
            <div class="panel-h">
              등록된 용어
              <span class="r">{{ terms.length }}</span>
            </div>
            <button class="new-btn" @click="newTerm">+ 새 용어</button>
            <button
              v-for="t in terms"
              :key="t.id"
              class="termitem"
              :class="{ on: t.id === selectedId }"
              @click="selectTerm(t.id)"
            >
              {{ t.term }}
              <span class="lv" :class="{ mgr: t.minRoleLevel >= 50 }">
                {{ t.minRoleLevel >= 50 ? '매니저 이상' : '전 직원' }}
              </span>
            </button>
          </div>
        </div>

        <div class="col-form">
          <div class="panel">
            <div class="panel-h">{{ isNew ? '새 용어' : form.term || '(제목 없음)' }}</div>
            <div class="panel-b">
              <div class="frow">
                <label>용어</label>
                <input v-model="form.term" class="field fv" placeholder="예: 이탈 고객" />
              </div>
              <div class="frow">
                <label>같은 뜻</label>
                <input v-model="aliasesText" class="field fv" placeholder="쉼표로 구분, 예: 휴면 고객, 떠난 고객" />
              </div>
              <div class="frow">
                <label>정의</label>
                <textarea v-model="form.definition" class="field fv tall" rows="3" placeholder="이 용어를 어떻게 계산할지 설명" />
              </div>
              <div class="frow">
                <label>계산 기준</label>
                <input v-model="form.sqlHint" class="field fv mono" placeholder="예: MAX(o.ordered_at) < CURRENT_DATE - INTERVAL '90 days'" />
              </div>
              <div class="frow">
                <label>관련 테이블</label>
                <input v-model="relatedTablesText" class="field fv" placeholder="쉼표로 구분, 예: ord, region" />
              </div>
              <div class="frow">
                <label>공개 범위</label>
                <select v-model.number="form.minRoleLevel" class="field fv">
                  <option v-for="opt in ROLE_LEVELS" :key="opt.level" :value="opt.level">{{ opt.label }}</option>
                </select>
              </div>
              <div class="frow">
                <label>사용 여부</label>
                <label class="checkbox-fv">
                  <input v-model="form.enabled" type="checkbox" />
                  질문에 적용
                </label>
              </div>

              <p v-if="!canManage" class="error-text">
                이 등급(레벨 {{ requiredLevel }}) 용어를 관리하려면 내 등급이 그 이상이어야 합니다.
              </p>
              <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

              <div class="opts">
                <button class="btn btn-primary" :disabled="saving || !canManage" @click="save">저장</button>
                <button v-if="!isNew" class="btn" :disabled="saving || !canManage" @click="remove">삭제</button>
                <button v-if="!isNew" class="btn quiet" :disabled="saving" @click="tryTerm">이 용어로 질문해보기</button>
              </div>
            </div>
          </div>
          <p class="note">
            등록 항목이 많아지면 전부 불러오지 않고 질문과 관련된 용어만 찾아 쓰는 방식으로 바뀔 수 있습니다. 이 화면은 그대로입니다.
          </p>
        </div>
      </div>
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
  margin: 0 0 24px;
  max-width: 64ch;
}

.split {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  flex-wrap: wrap;
}
.col-list {
  width: 260px;
  flex-shrink: 0;
}
.col-form {
  flex: 1;
  min-width: 320px;
}

.new-btn {
  width: 100%;
  text-align: left;
  padding: 9px 14px;
  color: var(--color-main);
  font-size: 13px;
  font-weight: 600;
  border-bottom: 1px solid var(--color-border);
}
.new-btn:hover {
  background: var(--color-sunk);
}
.termitem {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  text-align: left;
  padding: 9px 14px;
  border-bottom: 1px solid var(--color-border);
  font-size: 13px;
  color: var(--text-2);
}
.termitem:last-child {
  border-bottom: none;
}
.termitem:hover {
  background: var(--color-sunk);
}
.termitem.on {
  background: var(--color-main-soft);
  color: var(--color-main);
  font-weight: 600;
}
.lv {
  margin-left: auto;
  font-size: 11px;
  border: 1px solid var(--color-border);
  border-radius: 2px;
  padding: 0 6px;
  color: var(--text-3);
  font-weight: 400;
}
.lv.mgr {
  border-color: var(--color-danger);
  color: var(--color-danger);
}

.panel-b {
  padding: 16px;
}
.frow {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  align-items: flex-start;
  flex-wrap: wrap;
}
.frow label {
  width: 90px;
  flex-shrink: 0;
  color: var(--text-2);
  padding-top: 9px;
  font-size: 13px;
}
.fv {
  flex: 1;
  min-width: 200px;
}
textarea.fv {
  resize: vertical;
  font-family: inherit;
}
.fv.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
}
.checkbox-fv {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-2);
  padding-top: 9px;
}
.opts {
  display: flex;
  gap: 8px;
  margin-top: 4px;
  flex-wrap: wrap;
}
.btn.quiet {
  color: var(--text-2);
  border-color: transparent;
  background: none;
}
.btn.quiet:hover {
  background: var(--color-sunk);
}
</style>
