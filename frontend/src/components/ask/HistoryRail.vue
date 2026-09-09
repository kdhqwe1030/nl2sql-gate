<script setup lang="ts">
import { computed } from 'vue'
import { useAskStore } from '../../stores/ask'

const store = useAskStore()
const reversed = computed(() => [...store.history].reverse())
</script>

<template>
  <aside class="hist">
    <p v-if="!store.history.length" class="empty">아직 질문이 없습니다.</p>
    <template v-else>
      <p class="lbl">오늘</p>
      <button
        v-for="e in reversed"
        :key="e.id"
        class="item"
        :class="{ on: e.id === store.currentId }"
        @click="store.select(e.id)"
      >
        {{ e.question }}
      </button>
    </template>
  </aside>
</template>

<style scoped>
.hist {
  width: 232px;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
  flex-shrink: 0;
  overflow-y: auto;
  padding: 14px 10px;
}
.lbl {
  font-size: 11.5px;
  color: var(--text-3);
  padding: 0 8px;
  margin: 0 0 6px;
}
.item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 7px 9px;
  border-radius: 6px;
  color: var(--text-2);
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 1px;
}
.item:hover {
  background: var(--color-sunk);
}
.item.on {
  background: var(--color-sunk);
  color: var(--text-1);
  font-weight: 600;
}
.empty {
  padding: 8px;
  color: var(--text-3);
  font-size: 12.5px;
}
</style>
