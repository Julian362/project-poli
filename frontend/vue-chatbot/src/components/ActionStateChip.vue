<template>
  <div class="chip" :class="[status, typeClass]" @click="$emit('click')">
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
defineEmits(["click"]);
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
  cursor: pointer;
  transition: transform 120ms ease, box-shadow 200ms ease, background 200ms ease;
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
.chip:hover {
  transform: translateY(-1px);
}
.chip:active {
  transform: translateY(0);
}
/* subtle glow when ON */
.chip.success.is-on {
  animation: glow 1600ms ease-in-out infinite;
}
@keyframes glow {
  0% {
    box-shadow: 0 0 0 1px #48a055, 0 0 6px rgba(72, 160, 85, 0.35);
  }
  50% {
    box-shadow: 0 0 0 1px #48a055, 0 0 10px rgba(72, 160, 85, 0.6);
  }
  100% {
    box-shadow: 0 0 0 1px #48a055, 0 0 6px rgba(72, 160, 85, 0.35);
  }
}
/* gentle fade when OFF */
.chip.success.is-off {
  filter: saturate(0.9);
}
/* simple ripple */
.chip::after {
  content: "";
  position: absolute;
  left: 50%;
  top: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  background: rgba(255, 255, 255, 0.08);
  pointer-events: none;
}
.chip:active::after {
  animation: ripple 420ms ease-out;
}
@keyframes ripple {
  0% {
    width: 0;
    height: 0;
    opacity: 0.35;
  }
  100% {
    width: 240%;
    height: 240%;
    opacity: 0;
  }
}
.icon {
  font-size: 14px;
}
.state {
  font-weight: 500;
}
</style>
