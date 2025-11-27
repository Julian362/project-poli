<template>
  <div class="chips">
    <button class="chip" @click="$emit('send', 'prende sala')">
      prende sala
    </button>
    <button class="chip" @click="$emit('send', 'prende cocina')">
      prende cocina
    </button>
    <button class="chip" @click="$emit('send', 'prende habitacion')">
      prende habitacion
    </button>
    <button class="chip danger" @click="$emit('send', 'apaga todo')">
      apaga todo
    </button>
  </div>
</template>

<script setup>
defineEmits(["send"]);
</script>
