<template>
  <div class="day-divider">
    <div class="line"></div>
    <div class="label">{{ label }}</div>
    <div class="line"></div>
  </div>
</template>
<script setup>
const props = defineProps({ label: String });
</script>
<style scoped>
.day-divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 14px 0;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #8892a0;
}
.line {
  flex: 1;
  height: 1px;
  background: #30343a;
}
.label {
  padding: 2px 8px;
  background: #22272e;
  border-radius: 12px;
  box-shadow: 0 0 0 1px #2d3136;
}
</style>
