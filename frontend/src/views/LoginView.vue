<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { ApiError } from "../api/http";
import { useAuthStore } from "../stores/auth";

const email = ref("");
const password = ref("");
const errorMessage = ref("");
const loading = ref(false);

const auth = useAuthStore();
const router = useRouter();

async function handleSubmit() {
  if (!email.value || !password.value) return;
  errorMessage.value = "";
  loading.value = true;
  try {
    await auth.login({ email: email.value, password: password.value });
    router.push({ name: "ask" });
  } catch (e) {
    errorMessage.value =
      e instanceof ApiError ? e.message : "로그인에 실패했습니다.";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-mark">nl2sql-gate</div>
      <h1>사내 데이터 조회</h1>
      <p class="sub">이메일과 비밀번호로 로그인하세요.</p>
      <form @submit.prevent="handleSubmit">
        <input
          v-model="email"
          class="field"
          type="email"
          placeholder="이메일"
          autocomplete="username"
        />
        <input
          v-model="password"
          class="field"
          type="password"
          placeholder="비밀번호"
          autocomplete="current-password"
        />
        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
        <button class="btn btn-primary" type="submit" :disabled="loading">
          {{ loading ? "로그인 중..." : "로그인" }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: var(--color-bg);
}
.login-card {
  width: 100%;
  max-width: 352px;
}
.login-mark {
  font-weight: 800;
  font-size: 36px;
  color: var(--color-main);
  letter-spacing: -0.01em;
  margin-bottom: 20px;
}
.login-card h1 {
  font-size: 18px;
  letter-spacing: -0.02em;
  margin: 0 0 6px;
}
.sub {
  color: var(--text-2);
  margin: 0 0 28px;
  font-size: 13.5px;
}
form .field {
  margin-bottom: 9px;
}
.btn-primary {
  width: 100%;
  padding: 12px;
  margin-top: 5px;
}
</style>
