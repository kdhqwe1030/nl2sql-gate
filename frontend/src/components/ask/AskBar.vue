<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{ ask: [question: string] }>()

// docs/llm-테스트-질문-20개.md 기준으로 role 상관없이 안정적으로 성공하는 질문만 골랐다.
// few-shot + FK 구조화 수정(프롬프트-회귀-분석.md) 이후 지역별 매출류가 다시 안정화된 걸
// 확인하고 재추가했다. 지역별 매출/월별 실적은 여러 행이 나오는 표를 보여주기 좋다.
const SUGGESTIONS = [
  '부서별 인원수 알려줘',
  '재직 중인 직원은 몇 명이야?',
  '최근 30일 주문 총액은?',
  '이번달 주문 몇 건이야?',
  '서울 지역 주문 건수는?',
  '가장 매출이 높은 지역은 어디야?',
  '지역별 매출 알려줘',
  '월별 실적 보여줘',
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
