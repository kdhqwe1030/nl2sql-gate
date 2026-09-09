<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError } from '../api/http'
import { getUsers, updateUser } from '../api/userAdmin'
import { useAuthStore } from '../stores/auth'
import type { Role } from '../types/auth'
import type { UserSummary } from '../types/userAdmin'

const auth = useAuthStore()
const users = ref<UserSummary[]>([])
const loading = ref(false)
const errorMessage = ref('')
const savingId = ref<string | null>(null)

const ROLE_OPTIONS: { value: Role; label: string }[] = [
  { value: 'STAFF', label: 'STAFF' },
  { value: 'MANAGER', label: 'MANAGER' },
  { value: 'ADMIN', label: 'ADMIN' },
]

function canManage(u: UserSummary) {
  return (auth.user?.roleLevel ?? 0) > u.roleLevel
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    users.value = await getUsers()
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function toggleActive(u: UserSummary) {
  savingId.value = u.id
  errorMessage.value = ''
  try {
    const updated = await updateUser(u.id, { active: !u.active })
    users.value = users.value.map((x) => (x.id === updated.id ? updated : x))
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '변경 중 오류가 발생했습니다.'
  } finally {
    savingId.value = null
  }
}

async function changeRole(u: UserSummary, role: Role) {
  if (role === u.role) return
  savingId.value = u.id
  errorMessage.value = ''
  try {
    const updated = await updateUser(u.id, { role })
    users.value = users.value.map((x) => (x.id === updated.id ? updated : x))
  } catch (e) {
    errorMessage.value = e instanceof ApiError ? e.message : '변경 중 오류가 발생했습니다.'
    await load() // select가 낙관적으로 바뀐 채 남지 않도록 서버 상태로 되돌린다
  } finally {
    savingId.value = null
  }
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

onMounted(load)
</script>

<template>
  <div class="pane">
    <div class="pane-inner">
      <h1>구성원 관리</h1>
      <p class="sub">
        가입 승인과 역할 변경을 합니다. 나보다 등급이 낮은 구성원만 수정할 수 있고, 내 등급 이상으로는 올릴 수 없습니다.
      </p>

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <div class="panel">
        <div class="panel-h">
          구성원
          <span class="r">{{ users.length }}</span>
        </div>
        <div class="table-scroll">
          <table class="data-table">
            <thead>
              <tr>
                <th>이름</th>
                <th>이메일</th>
                <th>등급</th>
                <th>상태</th>
                <th>가입일</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in users" :key="u.id">
                <td>{{ u.name }}</td>
                <td class="muted">{{ u.email }}</td>
                <td>
                  <select
                    class="field role-select"
                    :value="u.role"
                    :disabled="!canManage(u) || savingId === u.id"
                    @change="changeRole(u, ($event.target as HTMLSelectElement).value as Role)"
                  >
                    <option v-for="opt in ROLE_OPTIONS" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </option>
                  </select>
                </td>
                <td>
                  <span class="chip" :class="u.active ? 'chip-success' : 'chip-warning'">
                    {{ u.active ? '승인됨' : '승인 대기' }}
                  </span>
                </td>
                <td class="muted">{{ formatDate(u.createdAt) }}</td>
                <td class="action-cell">
                  <button class="btn" :disabled="!canManage(u) || savingId === u.id" @click="toggleActive(u)">
                    {{ u.active ? '승인 취소' : '승인' }}
                  </button>
                </td>
              </tr>
              <tr v-if="!loading && users.length === 0">
                <td colspan="6" class="empty-cell">구성원이 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <p class="note">이 화면은 매니저 이상만 볼 수 있습니다. 목록은 우리 회사 구성원 전체가 보입니다.</p>
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
  margin: 0 0 16px;
  max-width: 64ch;
}
.muted {
  color: var(--text-3);
  font-size: 12.5px;
}
.role-select {
  width: auto;
  padding: 5px 8px;
  font-size: 12.5px;
}
.action-cell {
  text-align: right;
  white-space: nowrap;
}
.empty-cell {
  text-align: center;
  color: var(--text-3);
  padding: 24px;
}
</style>
