<template>
  <div class="chip" :class="status">
    <span class="icon" v-if="status === 'pending'">⏳</span>
    <span class="icon" v-else-if="status === 'success' && type === 'on'"
      >💡</span
    >
    <span class="icon" v-else-if="status === 'success' && type === 'off'"
      >🕯️</span
    >
    <span class="icon" v-else-if="status === 'fail'">⚠️</span>
    <span class="label">{{ label }}</span>
    <span class="state" v-if="status === 'pending'">…</span>
    <span class="state" v-else-if="status === 'success' && type === 'on'"
      >encendida</span
    >
    <span class="state" v-else-if="status === 'success' && type === 'off'"
      >apagada</span
    >
    <span class="state" v-else-if="status === 'fail'">error</span>
  </div>
</template>

<script setup>
const props = defineProps({
  label: String, // sala, cocina, habitación, todas
  status: { type: String, default: "pending" }, // pending|success|fail
  type: { type: String, default: "on" }, // on|off
});
</script>

<style scoped>
.chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 16px;
  background: #2b2f36;
  color: #e6e9ed;
  line-height: 1;
  margin: 2px 4px 2px 0;
}
.chip.pending {
  opacity: 0.8;
}
.chip.success {
  background: #254a2b;
}
.chip.success.on {
  box-shadow: 0 0 0 1px #2e7d32;
}
.chip.success.off {
  box-shadow: 0 0 0 1px #546e7a;
}
.chip.fail {
  background: #4a2525;
  box-shadow: 0 0 0 1px #d32f2f;
}
.icon {
  font-size: 14px;
}
.state {
  font-weight: 500;
}
</style>
