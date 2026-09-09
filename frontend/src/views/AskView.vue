<script setup lang="ts">
// 다음 단계(4단계)에서 히스토리 목록 + 질문 입력 + POST /api/query 연동으로 채운다.
</script>

<template>
  <div class="pane">
    <div class="empty-state">
      <h1>질문하기</h1>
      <p>다음 단계에서 이 화면에 질문 입력과 결과 카드(RESULT / CLARIFY / ERROR)를 구현합니다.</p>
    </div>
  </div>
</template>

<style scoped>
.pane {
  padding: 32px;
}
.empty-state {
  max-width: 520px;
}
.empty-state h1 {
  font-size: 19px;
  font-weight: 700;
  letter-spacing: -0.01em;
  margin: 0 0 8px;
  color: var(--text-1);
}
.empty-state p {
  color: var(--text-2);
  margin: 0;
}
</style>
