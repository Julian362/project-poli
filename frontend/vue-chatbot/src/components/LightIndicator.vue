<template>
  <div class="indicator">
    <span class="dot" :style="{ background: color }" />
    <span>{{ label }}</span>
  </div>
</template>

<script setup>
import { computed } from "vue";
const props = defineProps({ label: String, on: Boolean, colorOn: String });
const color = computed(() => (props.on ? props.colorOn : "var(--off)"));
</script>
