<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="shell-root">
    <header class="topbar">
      <span class="mark">nl2sql-gate</span>
      <div class="top-right">
        <span v-if="auth.user" class="role-badge">{{ auth.user.role }}</span>
        <span v-if="auth.user" class="uname">{{ auth.user.name }}</span>
        <button class="btn" @click="handleLogout">로그아웃</button>
      </div>
    </header>

    <div class="body">
      <nav class="rail">
        <RouterLink to="/ask" class="rail-btn" active-class="on">
          <svg width="15" height="15" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.4">
            <path d="M2.5 3.5h11v7h-6l-3 2.5v-2.5h-2z" />
          </svg>
          <span>질문하기</span>
        </RouterLink>

        <p class="grp">관리</p>

        <RouterLink to="/terms" class="rail-btn" active-class="on">
          <svg width="15" height="15" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.4">
            <path d="M3 2.5h10v11H3z" />
            <path d="M5.5 5.5h5M5.5 8h5M5.5 10.5h3" />
          </svg>
          <span>업무 용어</span>
        </RouterLink>

        <RouterLink to="/data" class="rail-btn" active-class="on">
          <svg width="15" height="15" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.4">
            <ellipse cx="8" cy="4" rx="5" ry="2" />
            <path d="M3 4v8c0 1.1 2.2 2 5 2s5-.9 5-2V4" />
            <path d="M3 8c0 1.1 2.2 2 5 2s5-.9 5-2" />
          </svg>
          <span>데이터 연결</span>
        </RouterLink>

        <RouterLink to="/audit" class="rail-btn" active-class="on">
          <svg width="15" height="15" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.4">
            <path d="M2.5 13.5v-5M6.5 13.5v-9M10.5 13.5v-6M14 13.5v-3" />
          </svg>
          <span>사용 현황</span>
        </RouterLink>
      </nav>

      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell-root {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.topbar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 18px;
  height: 52px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}
.mark {
  font-weight: 700;
  font-size: 14px;
  color: var(--color-main);
  letter-spacing: -0.01em;
}
.top-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}
.role-badge {
  background: var(--color-main-soft);
  color: var(--color-main);
  border-radius: 3px;
  padding: 3px 9px;
  font-size: 12.5px;
  font-weight: 600;
}
.uname {
  font-size: 13px;
  color: var(--text-2);
}

.body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.rail {
  width: 186px;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
  padding: 14px 10px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}
.rail-btn {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 8px 10px;
  border-radius: 6px;
  color: var(--text-2);
  margin-bottom: 2px;
  text-decoration: none;
  font-size: 13px;
}
.rail-btn:hover {
  background: var(--color-sunk);
}
.rail-btn.on {
  background: var(--color-main-soft);
  color: var(--color-main);
  font-weight: 600;
}
.rail-btn svg {
  flex-shrink: 0;
}
.grp {
  font-size: 11.5px;
  color: var(--text-3);
  padding: 0 10px;
  margin: 16px 0 6px;
}

.content {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
}
</style>
