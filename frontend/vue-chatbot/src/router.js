import { createRouter, createWebHistory } from "vue-router";
import ChatView from "./views/ChatView.vue";
import HistoryStatsView from "./views/HistoryStatsView.vue";

const routes = [
  { path: "/", component: ChatView, name: "chat" },
  { path: "/historial", component: HistoryStatsView, name: "history" },
];

export const router = createRouter({
  history: createWebHistory(),
  routes,
});
