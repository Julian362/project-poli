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
      <section class="stats sticky">
        <div class="stat-card full">
          <h3>Resumen por habitación (24h / total)</h3>
          <div class="rooms">
            <div class="room" v-for="r in roomsSummary" :key="r.key">
              <div class="room-head">
                <span class="room-name">{{ r.name }}</span>
              </div>
              <div class="counts">
                <div class="row">
                  <span class="label">💡 Encendida</span>
                  <span class="value">{{ r.on24h }} / {{ r.onTotal }}</span>
                </div>
                <div class="row">
                  <span class="label">🕯️ Apagada</span>
                  <span class="value">{{ r.off24h }} / {{ r.offTotal }}</span>
                </div>
              </div>
              <div class="top-badges" v-if="r.top24h.length">
                <span class="mini-title">Top hoy</span>
                <ul class="badge-list">
                  <li v-for="c in r.top24h" :key="c.command" class="badge">
                    <span class="cmd">{{ formatCommand(c.command) }}</span>
                    <span class="count">{{ c.count }}</span>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </section>
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
const roomsSummary = ref([]);
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
  computeRoomsSummary();
});

function formatCommand(code) {
  const map = {
    H: "💡 Habitación encendida",
    h: "🕯️ Habitación apagada",
    C: "💡 Cocina encendida",
    c: "🕯️ Cocina apagada",
    S: "💡 Sala encendida",
    s: "🕯️ Sala apagada",
    X: "Otro",
  };
  return map[code] || code;
}

function computeRoomsSummary() {
  const byCode24 = Object.fromEntries(
    topCommands.value.map((c) => [c.command, c.count])
  );
  const byCodeTotal = Object.fromEntries(
    counts.value.map((c) => [c.command, c.count])
  );
  const defs = [
    {
      key: "habitacion",
      name: "Habitación",
      onCode24: "H",
      offCode24: "h",
      onCodeTot: "H",
      offCodeTot: "h",
    },
    {
      key: "cocina",
      name: "Cocina",
      onCode24: "C",
      offCode24: "c",
      onCodeTot: "C",
      offCodeTot: "c",
    },
    {
      key: "sala",
      name: "Sala",
      onCode24: "S",
      offCode24: "s",
      onCodeTot: "S",
      offCodeTot: "s",
    },
  ];
  roomsSummary.value = defs.map((d) => {
    const on24h = byCode24[d.onCode24] || 0;
    const off24h = byCode24[d.offCode24] || 0;
    const onTotal = byCodeTotal[d.onCodeTot] || 0;
    const offTotal = byCodeTotal[d.offCodeTot] || 0;
    const top24h = topCommands.value.filter(
      (c) => c.command === d.onCode24 || c.command === d.offCode24
    );
    return {
      key: d.key,
      name: d.name,
      on24h,
      off24h,
      onTotal,
      offTotal,
      top24h,
    };
  });
}
</script>
<style scoped>
.hist-layout {
  box-sizing: border-box;
  padding: 16px 20px 32px;
  max-width: 1400px;
  margin: 0 auto;
  min-height: 100svh;
  overflow: auto;
  display: flex;
  flex-direction: column;
}
@supports (height: 100dvh) {
  .hist-layout {
    min-height: 100dvh;
  }
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
  flex-direction: column;
  gap: 24px;
  flex: 1 1 auto;
  min-height: 0;
}
.history {
  flex: 1;
  min-width: 0;
}
.stats {
  display: block;
}
.stats.sticky {
  position: sticky;
  top: 0;
  z-index: 5;
  padding-top: 6px;
  background: linear-gradient(#0a1016 0%, rgba(10,16,22,0.92) 60%, rgba(10,16,22,0.85) 100%);
}
.sect-title {
  font-size: 14px;
  margin: 0 0 8px;
  letter-spacing: 0.5px;
  opacity: 0.8;
}
.list {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  border-radius: 12px;
  padding: 12px 14px;
  padding-bottom: calc(20px + env(safe-area-inset-bottom, 0px));
  background: #11161c;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.03);
}
.msg {
  display: grid;
  grid-template-columns: 64px 60px 1fr;
  gap: 8px;
  padding: 8px 0;
  font-size: 13px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
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
  background: #11161c;
  border-radius: 12px;
  padding: 16px 18px;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.03);
}
.stat-card.full {
  padding: 16px 18px;
}
.stat-card h3 {
  margin: 0 0 10px;
  font-size: 15px;
}
.rooms {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}
.room {
  background: #0e1318;
  border-radius: 12px;
  padding: 12px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}
.room-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.room-name {
  font-weight: 600;
}
.counts {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.row {
  display: grid;
  grid-template-columns: 140px 1fr;
  align-items: center;
  gap: 12px;
}
.top-badges {
  margin-top: 8px;
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
  background: #141a20;
  padding: 6px 10px;
  border-radius: 14px;
  font-size: 12px;
  display: flex;
  gap: 6px;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
}
.badge .count {
  background: #1f252b;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 11px;
}
@media (max-width: 1000px) {
  .content {
    gap: 20px;
  }
  .rooms {
    grid-template-columns: 1fr;
  }
}
</style>
