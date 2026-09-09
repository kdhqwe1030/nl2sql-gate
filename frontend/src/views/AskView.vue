<script setup lang="ts">
import { computed } from 'vue'
import AskBar from '../components/ask/AskBar.vue'
import HistoryRail from '../components/ask/HistoryRail.vue'
import QuestionCard from '../components/ask/QuestionCard.vue'
import { useAskStore } from '../stores/ask'

const store = useAskStore()
const reversed = computed(() => [...store.history].reverse())

function handleAsk(question: string) {
  store.ask(question)
}
</script>

<template>
  <div class="ask-view">
    <HistoryRail />
    <div class="pane">
      <div class="pane-inner">
        <AskBar @ask="handleAsk" />

        <div v-if="!store.history.length" class="empty-state">
          <h2>무엇이 궁금하세요?</h2>
          <p>SQL을 몰라도 됩니다. 평소 쓰는 말로 물어보면 권한 안에서 찾아 보여줍니다.</p>
        </div>

        <QuestionCard v-for="e in reversed" :key="e.id" :entry="e" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.ask-view {
  display: flex;
  height: 100%;
}
.pane {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 26px 30px 60px;
}
.pane-inner {
  max-width: 960px;
}
.empty-state {
  padding: 44px 0;
  max-width: 520px;
}
.empty-state h2 {
  font-size: 19px;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--text-1);
}
.empty-state p {
  color: var(--text-2);
  margin: 0;
}
</style>
