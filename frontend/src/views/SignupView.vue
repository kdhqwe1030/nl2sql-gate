<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { ApiError } from "../api/http";
import { useAuthStore } from "../stores/auth";

const name = ref("");
const email = ref("");
const password = ref("");
const errorMessage = ref("");
const loading = ref(false);

const auth = useAuthStore();
const router = useRouter();

async function handleSubmit() {
  if (!name.value || !email.value || !password.value) return;
  if (password.value.length < 8) {
    errorMessage.value = "비밀번호는 8자 이상이어야 합니다.";
    return;
  }
  errorMessage.value = "";
  loading.value = true;
  try {
    await auth.signup({ name: name.value, email: email.value, password: password.value });
    router.push({ name: "ask" });
  } catch (e) {
    errorMessage.value =
      e instanceof ApiError ? e.message : "회원가입에 실패했습니다.";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-mark">nl2sql-gate</div>
      <h1>회원가입</h1>
      <p class="sub">이름, 이메일, 비밀번호를 입력하세요.</p>
      <form @submit.prevent="handleSubmit">
        <input
          v-model="name"
          class="field"
          type="text"
          placeholder="이름"
          autocomplete="name"
        />
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
          placeholder="비밀번호 (8자 이상)"
          autocomplete="new-password"
        />
        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
        <button class="btn btn-primary" type="submit" :disabled="loading">
          {{ loading ? "가입 중..." : "가입하기" }}
        </button>
      </form>
      <p class="switch">
        이미 계정이 있으신가요?
        <RouterLink :to="{ name: 'login' }">로그인</RouterLink>
      </p>
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
.switch {
  margin: 18px 0 0;
  text-align: center;
  font-size: 13px;
  color: var(--text-2);
}
.switch a {
  color: var(--color-main);
  font-weight: 600;
}
</style>
