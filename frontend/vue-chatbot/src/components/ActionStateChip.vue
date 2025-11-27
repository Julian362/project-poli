<template>
  <div class="chip" :class="[status, typeClass]">
    <span class="icon" v-if="status === 'pending'">⏳</span>
    <span class="icon" v-else-if="status === 'fail'">⚠️</span>
    <span class="icon" v-else>{{ icon }}</span>
    <span class="label">{{ label }}</span>
    <span class="state" v-if="status === 'pending'">…</span>
    <span class="state" v-else-if="status === 'success'">{{ stateWord }}</span>
    <span class="state" v-else-if="status === 'fail'">error</span>
  </div>
</template>

<script setup>
import { computed } from "vue";
const props = defineProps({
  label: String,
  status: { type: String, default: "pending" },
  type: { type: String, default: "on" },
});
const icon = computed(() => (props.type === "on" ? "💡" : "🕯️"));
const stateWord = computed(() =>
  props.type === "on" ? "encendida" : "apagada"
);
// add class to differentiate on/off for background gradients
const typeClass = computed(() => (props.type === "on" ? "is-on" : "is-off"));
</script>

<style scoped>
.chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 18px;
  background: #2b2f36;
  color: #e6e9ed;
  line-height: 1;
  margin: 2px 4px 2px 0;
  position: relative;
}
.chip.pending {
  opacity: 0.85;
}
.chip.success.is-on {
  background: linear-gradient(135deg, #2f5e35, #3d7a42);
  box-shadow: 0 0 0 1px #48a055;
}
.chip.success.is-off {
  background: #2b3138;
  box-shadow: 0 0 0 1px #4d5a63;
  opacity: 0.9;
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
