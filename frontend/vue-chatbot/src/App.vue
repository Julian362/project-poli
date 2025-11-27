<template>
  <div class="layout">
    <div class="container">
    <div class="header">
      <h1>ChatBot IoT</h1>
      <div class="indicators">
        <LightIndicator
          label="Sala"
          :on="state.sala === 'on'"
          colorOn="var(--yellow)"
        />
        <LightIndicator
          label="Cocina"
          :on="state.cocina === 'on'"
          colorOn="var(--green)"
        />
        <LightIndicator
          label="Habitación"
          :on="state.habitacion === 'on'"
          colorOn="var(--blue)"
        />
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

      <div class="small" style="margin-top: 6px">Sesión: {{ sessionId }} · {{ messages.length }} msgs</div>
    </div>
    <SidebarPanel :state="state" :top="topCommands" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from "vue";
import { createSession, history, ask, lightsState, topSince } from "./api/graphql";
import LightIndicator from "./components/LightIndicator.vue";
import MessageList from "./components/MessageList.vue";
import QuickChips from "./components/QuickChips.vue";
import ActionStateChip from "./components/ActionStateChip.vue";
import SidebarPanel from "./components/SidebarPanel.vue";

const sessionId = getOrCreateSession();
const allMessages = ref([]);
const pageSize = ref(40);
const visibleMessages = computed(()=> allMessages.value.slice(-pageSize.value));
const draft = ref("");
const loading = ref(false);
const state = reactive({ habitacion: "off", cocina: "off", sala: "off" });
const prevState = reactive({ habitacion: "off", cocina: "off", sala: "off" });
const pending = ref([]); // {id,label,type,on, status}
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

function snapshotState(){
  prevState.habitacion = state.habitacion;
  prevState.cocina = state.cocina;
  prevState.sala = state.sala;
}

function detectActions(text){
  const t = text.toLowerCase();
  const roomsMap = { hab: 'Habitación', habitacion:'Habitación', cocina:'Cocina', sala:'Sala' };
  const isOn = /(prende|enciende)/.test(t);
  const isOff = /(apaga)/.test(t);
  const type = isOn? 'on' : (isOff? 'off':'');
  if(!type) return [];
  const all = /(todo|todas)/.test(t);
  const rooms = [];
  if(all) return [{ id: 'all-'+Date.now(), label:'Todas', type, status:'pending' }];
  for(const key of Object.keys(roomsMap)) if(t.includes(key)) rooms.push(roomsMap[key]);
  const uniq = [...new Set(rooms)];
  return uniq.map(r=>({ id:r+'-'+Date.now(), label:r, type, status:'pending' }));
}

function updatePendingFromState(){
  for(const chip of pending.value){
    if(chip.label==='Todas'){
      const allOn = state.habitacion==='on' && state.cocina==='on' && state.sala==='on';
      const allOff = state.habitacion==='off' && state.cocina==='off' && state.sala==='off';
      if(chip.type==='on' && allOn) chip.status='success';
      else if(chip.type==='off' && allOff) chip.status='success';
      else continue;
    } else {
      const code = chip.label.toLowerCase();
      const cur = code.startsWith('hab')? state.habitacion : code.startsWith('coc')? state.cocina : state.sala;
      if(chip.type==='on' && cur==='on') chip.status='success';
      else if(chip.type==='off' && cur==='off') chip.status='success';
      else continue;
    }
  }
  // remove success chips after delay
  pending.value = pending.value.filter(c=>{
    if(c.status==='success'){ setTimeout(()=>{
      pending.value = pending.value.filter(x=>x.id!==c.id);
    },1500); }
    return true;
  });
}

async function refreshTop(){
  try {
    const since = new Date(Date.now()-24*3600*1000).toISOString();
    topCommands.value = await topSince(since);
  } catch(e){ console.warn(e); }
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
  // push user message optimistically
  const userMsg = {
    id: Date.now().toString(),
    sessionId,
    role: "user",
    text,
    ts: new Date().toISOString(),
  };
  allMessages.value = [...allMessages.value, userMsg];
  // optimistic chips
  snapshotState();
  const chips = detectActions(text);
  if(chips.length) pending.value.push(...chips);
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
.layout { display:flex; }
.container { flex:1; }
.chips-row { display:flex; flex-wrap:wrap; margin:6px 0 4px; }
</style>
