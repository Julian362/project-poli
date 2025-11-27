<template>
  <div ref="wrap" class="messages auto-scroll">
    <div
      v-for="m in messages"
      :key="m.id + m.ts"
      class="msg"
      :class="m.role === 'user' ? 'user' : 'bot'"
    >
        <template v-for="(block, bi) in blocks" :key="bi">
          <DayDivider v-if="block.dayDivider" :label="block.dayDivider" />
          <div class="block" :class="block.role">
            <div class="avatar" v-if="showAvatar(block.role, bi)">
              <span v-if="block.role==='assistant'">🤖</span>
              <span v-else>👤</span>
            </div>
            <div class="bubbles">
              <div class="bubble" :class="block.role" v-for="m in block.items" :key="m.id+m.ts">
                <span class="text">{{ m.text }}</span>
                <span class="time" :title="fmtTime(m.ts)">{{ shortTime(m.ts) }}</span>
              </div>
            </div>
          </div>
        </template>
    </div>
    <div ref="sentinel" style="height:1px"></div>
  </div>

  </template>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, computed } from 'vue';
import DayDivider from './DayDivider.vue';
function isBot(m){ return m.role === 'assistant' || m.role === 'bot'; }
function isUser(m){ return m.role === 'user'; }

const blocks = computed(()=>{
  const out = [];
  let current = null; let lastDay = null;
  for (const m of props.messages) {
    const day = new Date(m.ts).toDateString();
    if (day !== lastDay) { out.push({ dayDivider: humanDay(m.ts), role: 'system', items: [] }); lastDay = day; }
    if (!current || current.role !== m.role) { current = { role: m.role, items: [] }; out.push(current); }
    current.items.push(m);
  }
  return out;
});

function humanDay(ts){
  const d = new Date(ts); const today = new Date();
  const diff = (today.setHours(0,0,0,0) - new Date(d.toDateString()).getTime())/86400000;
  if (diff===0) return 'Hoy'; if (diff===1) return 'Ayer';
  return d.toLocaleDateString();
}

function shortTime(ts){ return new Date(ts).toLocaleTimeString([], { hour:'2-digit', minute:'2-digit'}); }
function fmtTime(ts){ return new Date(ts).toLocaleString(); }
function showAvatar(role, idx){
  // show avatar only on first block of a consecutive sequence of same role
  if (role==='system') return false;
  const prev = blocks.value[idx-1];
  return !prev || prev.role !== role;
}
const props = defineProps({ messages: { type: Array, default: () => [] } });
const wrap = ref(null);
const sentinel = ref(null);

function scrollToBottom() {
  if (!wrap.value) return;
  // Prefer scrolling sentinel into view for smoother behavior
  nextTick(() => {
    try {
      sentinel.value?.scrollIntoView({ behavior: 'smooth', block: 'end' });
    } catch {
      wrap.value.scrollTop = wrap.value.scrollHeight;
    }
  });
}

watch(() => props.messages.length, () => scrollToBottom());
onMounted(() => scrollToBottom());
</script>

<style scoped>
.messages.auto-scroll {
  overflow-y: auto;
  max-height: 420px;
  scroll-behavior: smooth;
  /* Oculta scrollbar visual sin impedir scroll */
  scrollbar-width: none; /* Firefox */
}
.messages.auto-scroll::-webkit-scrollbar { display: none; }
.messages { overflow-y:auto; max-height:420px; padding:4px 8px 16px; scrollbar-width:none; scroll-behavior:smooth; }
.messages::-webkit-scrollbar { display:none; }
.block { display:flex; align-items:flex-start; gap:8px; margin:8px 0; }
.block.assistant .bubble { background:#2f3645; }
.block.user .bubble { background:#3a475e; }
.avatar { width:32px; height:32px; border-radius:50%; background:#262b31; display:flex; align-items:center; justify-content:center; font-size:16px; color:#fff; box-shadow:0 0 0 1px #31373d; flex-shrink:0; }
.bubbles { display:flex; flex-direction:column; gap:4px; }
.bubble { padding:8px 12px; border-radius:14px; font-size:14px; max-width:560px; position:relative; }
.bubble .time { position:absolute; bottom:4px; right:10px; font-size:10px; opacity:0; transition:opacity .2s; }
.bubble:hover .time { opacity:.6; }
.bubble.user { align-self:flex-end; }
@media (max-width:640px){ .bubble { font-size:13px; } }
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 12px;
  overflow-y: auto;
  max-height: 420px;
  padding: 4px 4px 12px;
  scroll-behavior: smooth;
  scrollbar-width: none;
}
  .cell { display: flex; align-items: flex-start; min-height: 32px; }
  .bot-cell { justify-content: flex-start; }
  .user-cell { justify-content: flex-end; }
  .avatar { width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; font-size: 18px; background: var(--avatar-bg); border-radius: 50%; box-shadow: 0 0 0 1px var(--border); }
  .avatar.bot { background: var(--bot-avatar,#2d3748); color: #fff; }
  .avatar.user { background: var(--user-avatar,#1a202c); color: #fff; }
  .bubble { background: var(--bubble,#222); padding: 8px 12px; border-radius: 14px; max-width: 95%; line-height: 1.25; font-size: 14px; white-space: pre-wrap; }
  .bot-bubble { background: var(--bot,#2f3645); }
  .user-bubble { background: var(--user,#3a475e); }
  .placeholder { visibility: hidden; }
@media (max-width: 640px) {
  .messages-grid { grid-template-columns: 1fr 1fr; }
  .bubble { font-size: 13px; }
}
</style>
