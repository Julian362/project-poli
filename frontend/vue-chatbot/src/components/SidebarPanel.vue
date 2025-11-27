<template>
  <aside class="sidebar" v-if="visible">
    <h2 class="title">Estado</h2>
    <div class="lights">
      <ActionStateChip :label="'Habitación'" :status="'success'" :type="state.habitacion==='on'?'on':'off'" />
      <ActionStateChip :label="'Cocina'" :status="'success'" :type="state.cocina==='on'?'on':'off'" />
      <ActionStateChip :label="'Sala'" :status="'success'" :type="state.sala==='on'?'on':'off'" />
    </div>
    <h2 class="title">Top hoy</h2>
    <ul class="top">
      <li v-for="c in top" :key="c.command">
        <span class="cmd">{{ c.command }}</span>
        <span class="count">{{ c.count }}</span>
      </li>
    </ul>
  </aside>
</template>
<script setup>
import ActionStateChip from './ActionStateChip.vue';
const props = defineProps({ state: Object, top: { type:Array, default:()=>[] }, visible: { type:Boolean, default:true } });
</script>
<style scoped>
.sidebar { width:250px; padding:14px 12px 24px; background:#1e2227; border-left:1px solid #2b3036; display:flex; flex-direction:column; gap:12px; }
.title { font-size:13px; font-weight:600; margin:0; letter-spacing:0.5px; color:#b8c1cc; }
.lights { display:flex; flex-wrap:wrap; }
.top { list-style:none; padding:0; margin:0; display:flex; flex-direction:column; gap:6px; }
.cmd { font-size:12px; color:#d0d5dc; }
.count { font-size:11px; background:#2a3036; padding:2px 6px; border-radius:10px; margin-left:4px; }
@media (max-width: 900px){ .sidebar { display:none; } }
</style>