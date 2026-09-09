<script setup lang="ts">
defineProps<{
  title: string
  description: string
}>()
</script>

<template>
  <div class="pane">
    <div class="empty-state">
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
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
