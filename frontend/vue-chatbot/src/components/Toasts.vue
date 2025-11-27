<template>
  <div class="toasts" aria-live="polite" aria-atomic="true">
    <div v-for="t in toasts" :key="t.id" class="toast" :class="t.type">
      <span class="msg">{{ t.text }}</span>
    </div>
  </div>
</template>
<script setup>
import { defineProps } from "vue";
const props = defineProps({ toasts: { type: Array, default: () => [] } });
</script>
<style scoped>
.toasts {
  position: fixed;
  right: 16px;
  bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 1000;
}
.toast {
  background: #11161c;
  color: #e8edf2;
  border-radius: 10px;
  padding: 10px 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
  font-size: 13px;
  opacity: 0.95;
}
.toast.success {
  border-left: 3px solid #48a055;
}
.toast.error {
  border-left: 3px solid #d32f2f;
}
.msg {
  display: inline-block;
}
</style>
