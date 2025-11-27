<template>
  <div class="hist-layout">
    <header class="hist-header">
      <h1 class="title">Historial & Estadísticas</h1>
      <div class="meta">
        <div class="lights">
          <ActionStateChip
            :label="'Habitación'"
            :status="'success'"
            :type="state.habitacion === 'on' ? 'on' : 'off'"
          />
          <ActionStateChip
            :label="'Cocina'"
            :status="'success'"
            :type="state.cocina === 'on' ? 'on' : 'off'"
          />
          <ActionStateChip
            :label="'Sala'"
            :status="'success'"
            :type="state.sala === 'on' ? 'on' : 'off'"
          />
        </div>
        <div class="range">
          <label>Últimas 24h</label>
        </div>
      </div>
    </header>
    <div class="content">
      <section class="history">
        <h2 class="sect-title">Mensajes sesión actual</h2>
        <div class="list" ref="listWrap">
          <div v-for="m in allMessages" :key="m.id" class="msg" :class="m.role">
            <span class="time" :title="fmtTime(m.ts)">{{
              shortTime(m.ts)
            }}</span>
            <span class="role">{{ labelRole(m.role) }}</span>
            <span class="text">{{ m.text }}</span>
          </div>
        </div>
      </section>
      <aside class="stats">
        <div class="stat-card">
          <h3>Top comandos 24h</h3>
          <ul class="badge-list">
            <li v-for="c in topCommands" :key="c.command" class="badge">
              <span class="cmd">{{ c.command }}</span>
              <span class="count">{{ c.count }}</span>
            </li>
          </ul>
        </div>
        <div class="stat-card">
          <h3>Conteos totales</h3>
          <ul class="badge-list">
            <li v-for="c in counts" :key="c.command" class="badge">
              <span class="cmd">{{ c.command }}</span>
              <span class="count">{{ c.count }}</span>
            </li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from "vue";
import { history, lightsState, topSince, commandCounts } from "../api/graphql";
import ActionStateChip from "../components/ActionStateChip.vue";

const sessionId = getSession();
const allMessages = ref([]);
const topCommands = ref([]);
const counts = ref([]);
const state = reactive({ habitacion: "off", cocina: "off", sala: "off" });

function getSession() {
  return localStorage.getItem("chat_session_id") || "web-unknown";
}
function labelRole(r) {
  return r === "assistant" ? "Bot" : r === "user" ? "Usuario" : r;
}
function shortTime(ts) {
  return new Date(ts).toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  });
}
function fmtTime(ts) {
  return new Date(ts).toLocaleString();
}

onMounted(async () => {
  try {
    allMessages.value = await history(sessionId);
  } catch (e) {
    console.error(e);
  }
  try {
    const st = await lightsState();
    state.habitacion = st.habitacion;
    state.cocina = st.cocina;
    state.sala = st.sala;
  } catch (e) {
    console.error(e);
  }
  try {
    const since = new Date(Date.now() - 24 * 3600 * 1000).toISOString();
    topCommands.value = await topSince(since);
  } catch (e) {
    console.error(e);
  }
  try {
    counts.value = await commandCounts();
  } catch (e) {
    console.error(e);
  }
});
</script>
<style scoped>
.hist-layout {
  padding: 12px 10px 28px;
  max-width: 1200px;
  margin: 0 auto;
}
.hist-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
}
.title {
  margin: 0;
  font-size: 22px;
}
.meta {
  display: flex;
  gap: 18px;
  align-items: center;
  flex-wrap: wrap;
}
.lights {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.content {
  display: flex;
  gap: 24px;
  align-items: stretch;
}
.history {
  flex: 1;
  min-width: 0;
}
.stats {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.sect-title {
  font-size: 14px;
  margin: 0 0 8px;
  letter-spacing: 0.5px;
  opacity: 0.8;
}
.list {
  max-height: 520px;
  overflow-y: auto;
  border: 1px solid #2b3036;
  border-radius: 10px;
  padding: 8px 10px;
  background: #1c2025;
}
.msg {
  display: grid;
  grid-template-columns: 64px 60px 1fr;
  gap: 8px;
  padding: 4px 0;
  font-size: 12px;
  border-bottom: 1px solid #242a2f;
}
.msg:last-child {
  border-bottom: none;
}
.msg.assistant .text {
  color: #c8e7ff;
}
.msg.user .text {
  color: #e6e9ed;
}
.time {
  opacity: 0.6;
}
.role {
  font-weight: 600;
  opacity: 0.75;
}
.text {
  white-space: pre-wrap;
}
.stat-card {
  background: #1c2025;
  border: 1px solid #2b3036;
  border-radius: 12px;
  padding: 12px 14px;
}
.stat-card h3 {
  margin: 0 0 10px;
  font-size: 15px;
}
.badge-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.badge {
  background: #1e2227;
  padding: 6px 10px;
  border-radius: 14px;
  font-size: 12px;
  display: flex;
  gap: 6px;
  align-items: center;
}
.badge .count {
  background: #2a3036;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 11px;
}
@media (max-width: 1000px) {
  .content {
    flex-direction: column;
  }
  .stats {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 280px;
  }
}
</style>
