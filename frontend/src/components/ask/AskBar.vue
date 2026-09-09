<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{ ask: [question: string] }>()

// docs/llm-테스트-질문-20개.md 기준으로 role 상관없이 안정적으로 성공하는 질문만 골랐다.
// 지역별 매출/작년 매출/부서별 평균 연봉 등은 프롬프트 회귀(관계없음 경고 과일반화)로
// CLARIFY가 나오는 걸 확인해서 뺐다 — 백엔드(PromptBuilder) 고친 뒤에 다시 넣는다.
const SUGGESTIONS = [
  '부서별 인원수 알려줘',
  '재직 중인 직원은 몇 명이야?',
  '최근 30일 주문 총액은?',
  '이번달 주문 몇 건이야?',
  '서울 지역 주문 건수는?',
  '가장 매출이 높은 지역은 어디야?',
  '무슨 데이터 볼 수 있어?',
]

const question = ref('')

function submit() {
  const q = question.value.trim()
  if (!q) return
  emit('ask', q)
  question.value = ''
}
</script>

<template>
  <div class="ask-bar-wrap">
    <div class="ask-bar">
      <input
        v-model="question"
        class="field"
        placeholder="평소 쓰는 말로 물어보세요"
        autocomplete="off"
        @keydown.enter="submit"
      />
      <button class="btn btn-primary" @click="submit">물어보기</button>
    </div>
    <div class="sugg">
      <button v-for="s in SUGGESTIONS" :key="s" class="chip-btn" @click="emit('ask', s)">
        {{ s }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.ask-bar-wrap {
  margin-bottom: 22px;
}
.ask-bar {
  display: flex;
  gap: 8px;
}
.ask-bar .field {
  flex: 1;
  min-width: 0;
}
.ask-bar .btn-primary {
  flex-shrink: 0;
  padding: 0 20px;
}
.sugg {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}
.chip-btn {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  border-radius: 99px;
  padding: 5px 12px;
  font-size: 12.5px;
  color: var(--text-2);
}
.chip-btn:hover {
  border-color: var(--color-main);
  color: var(--color-main);
}
</style>
