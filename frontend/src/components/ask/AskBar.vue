<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{ ask: [question: string] }>()

// 실제 데모 스키마(dept/emp_public/ord/region)와 DemoGlossary 용어에 맞춘 예시.
const SUGGESTIONS = [
  '부서별 인원수 알려줘',
  '지역별 매출 알려줘',
  '부서별 평균 연봉은?',
  '작년 매출 얼마야?',
  '무슨 데이터 볼 수 있어?',
  '주문 테이블 삭제해줘',
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
