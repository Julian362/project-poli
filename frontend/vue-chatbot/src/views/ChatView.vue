<template>
  <div class="layout">
    <div class="container">
      <div class="top-header">
        <div class="left">
          <h1 class="app-title">ChatBot IoT</h1>
          <div class="status-group">
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
        </div>
        <div class="top-commands" v-if="topCommands.length">
          <h2 class="mini-title">Top hoy</h2>
          <ul class="top-list">
            <li v-for="c in topCommands" :key="c.command" class="top-item">
              <span class="cmd">{{ c.command }}</span>
              <span class="count">{{ c.count }}</span>
            </li>
          </ul>
        </div>
      </div>

      <div class="card">
        <div class="bothello">
          Hola 👋 Escribe "prende sala" o selecciona un chip de ejemplo.
        </div>
        <QuickChips @send="onQuickSend" />
        <div class="chips-row" v-if="pending.length">
          <ActionStateChip
            v-for="c in pending"
            :key="c.id"
            :label="c.label"
            :status="c.status"
            :type="c.type"
          />
        </div>
        <MessageList :messages="visibleMessages" />
        <div class="footer">
          <input
            class="input"
            v-model="draft"
            @keyup.enter="send"
            placeholder="Escribe un comando... (ej: 'prende sala')"
          />
          <button
            class="button"
            :disabled="loading || !draft.trim()"
            @click="send"
          >
            Enviar
          </button>
        </div>
        <div class="small" style="margin-top: 6px">
          Sesión: {{ sessionId }} · {{ allMessages.length }} msgs
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from "vue";
import {
  createSession,
  history,
  ask,
  lightsState,
  topSince,
} from "../api/graphql";
import MessageList from "../components/MessageList.vue";
import QuickChips from "../components/QuickChips.vue";
import ActionStateChip from "../components/ActionStateChip.vue";

const sessionId = getOrCreateSession();
const allMessages = ref([]);
const pageSize = ref(40);
const visibleMessages = computed(() =>
  allMessages.value.slice(-pageSize.value)
);
const draft = ref("");
const loading = ref(false);
const state = reactive({ habitacion: "off", cocina: "off", sala: "off" });
const prevState = reactive({ habitacion: "off", cocina: "off", sala: "off" });
const pending = ref([]);
const topCommands = ref([]);

onMounted(async () => {
  try {
    await createSession(sessionId);
  } catch {}
  await refreshHistory();
  await refreshLights();
});

function getOrCreateSession() {
  const key = "chat_session_id";
  let sid = localStorage.getItem(key);
  if (!sid) {
    sid = "web-" + Math.random().toString(36).slice(2, 9);
    localStorage.setItem(key, sid);
  }
  return sid;
}
async function refreshHistory() {
  try {
    const hist = await history(sessionId);
    allMessages.value = hist;
    snapshotState();
  } catch (e) {
    console.error(e);
  }
}
async function refreshLights() {
  try {
    const st = await lightsState();
    state.habitacion = st.habitacion;
    state.cocina = st.cocina;
    state.sala = st.sala;
    updatePendingFromState();
  } catch (e) {
    console.error(e);
  }
}
function snapshotState() {
  prevState.habitacion = state.habitacion;
  prevState.cocina = state.cocina;
  prevState.sala = state.sala;
}
function detectActions(text) {
  const t = text.toLowerCase();
  const roomsMap = {
    hab: "Habitación",
    habitacion: "Habitación",
    cocina: "Cocina",
    sala: "Sala",
  };
  const isOn = /(prende|enciende)/.test(t);
  const isOff = /(apaga)/.test(t);
  const type = isOn ? "on" : isOff ? "off" : "";
  if (!type) return [];
  const all = /(todo|todas)/.test(t);
  if (all)
    return [
      { id: "all-" + Date.now(), label: "Todas", type, status: "pending" },
    ];
  const rooms = [];
  for (const key of Object.keys(roomsMap))
    if (t.includes(key)) rooms.push(roomsMap[key]);
  const uniq = [...new Set(rooms)];
  return uniq.map((r) => ({
    id: r + "-" + Date.now(),
    label: r,
    type,
    status: "pending",
  }));
}
function updatePendingFromState() {
  for (const chip of pending.value) {
    if (chip.label === "Todas") {
      const allOn =
        state.habitacion === "on" &&
        state.cocina === "on" &&
        state.sala === "on";
      const allOff =
        state.habitacion === "off" &&
        state.cocina === "off" &&
        state.sala === "off";
      if (chip.type === "on" && allOn) chip.status = "success";
      else if (chip.type === "off" && allOff) chip.status = "success";
      else continue;
    } else {
      const code = chip.label.toLowerCase();
      const cur = code.startsWith("hab")
        ? state.habitacion
        : code.startsWith("coc")
        ? state.cocina
        : state.sala;
      if (chip.type === "on" && cur === "on") chip.status = "success";
      else if (chip.type === "off" && cur === "off") chip.status = "success";
      else continue;
    }
  }
  pending.value = pending.value.filter((c) => {
    if (c.status === "success") {
      setTimeout(() => {
        pending.value = pending.value.filter((x) => x.id !== c.id);
      }, 1500);
    }
    return true;
  });
}
async function refreshTop() {
  try {
    const since = new Date(Date.now() - 24 * 3600 * 1000).toISOString();
    topCommands.value = await topSince(since);
  } catch (e) {
    console.warn(e);
  }
}
function onQuickSend(txt) {
  draft.value = txt;
  send();
}
async function send() {
  const text = draft.value.trim();
  if (!text) return;
  draft.value = "";
  loading.value = true;
  const userMsg = {
    id: Date.now().toString(),
    sessionId,
    role: "user",
    text,
    ts: new Date().toISOString(),
  };
  allMessages.value = [...allMessages.value, userMsg];
  snapshotState();
  const chips = detectActions(text);
  if (chips.length) pending.value.push(...chips);
  try {
    const botMsg = await ask(sessionId, text);
    allMessages.value = [...allMessages.value, botMsg];
  } catch (e) {
    allMessages.value = [
      ...allMessages.value,
      {
        id: "err-" + Date.now(),
        sessionId,
        role: "assistant",
        text: "Ups, hubo un problema. Intenta de nuevo.",
        ts: new Date().toISOString(),
      },
    ];
    console.error(e);
  } finally {
    loading.value = false;
    refreshLights();
    refreshTop();
  }
}
</script>

<style scoped>
.layout {
  display: flex;
}
.container {
  flex: 1;
}
.top-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  padding: 10px 4px 4px;
  flex-wrap: wrap;
}
.app-title {
  font-size: 20px;
  margin: 0 0 6px;
}
.status-group {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.mini-title {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 4px;
  letter-spacing: 0.5px;
  color: #b8c1cc;
}
.top-commands {
  display: flex;
  flex-direction: column;
  min-width: 180px;
}
.top-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.top-item {
  background: #1e2227;
  padding: 4px 8px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}
.cmd {
  color: #d0d5dc;
}
.count {
  background: #2a3036;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 11px;
}
.chips-row {
  display: flex;
  flex-wrap: wrap;
  margin: 6px 0 4px;
}
.card {
  background: #1c2025;
  border: 1px solid #2a3036;
  border-radius: 12px;
  padding: 12px 14px 18px;
  max-width: 820px;
  margin: 0 auto;
}
.bothello {
  font-size: 13px;
  opacity: 0.85;
  margin-bottom: 8px;
}
.footer {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
.input {
  flex: 1;
  background: #272d34;
  border: 1px solid #343b44;
  border-radius: 8px;
  padding: 8px 10px;
  color: #e8edf2;
  font-size: 14px;
}
.button {
  background: #2f5e35;
  border: none;
  color: #fff;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
.button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.small {
  font-size: 11px;
  opacity: 0.6;
}
</style>
