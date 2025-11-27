import { ApolloServer } from "@apollo/server";
import { startStandaloneServer } from "@apollo/server/standalone";
import fetch from "cross-fetch";
const CHATBOT_BASE = process.env.CHATBOT_BASE || "http://chatbot-spring:8081";
const ANALYTICS_BASE =
  process.env.ANALYTICS_BASE || "http://analytics-spring:8082";
const ROS_BASE = process.env.ROS_BASE || "http://ros-cli-api:8000";
const typeDefs = `#graphql
  type ChatMessage {
    id: ID
    sessionId: String!
    role: String!
    text: String!
    ts: String!
  }

  type AskResult {
    message: ChatMessage!
  }

  type Query {
    health: String!
    history(sessionId: String!): [ChatMessage!]!
    lightsState: LightsState!
    commandCounts: [CommandCount!]!
    topSince(since: String!): [TopCommand!]!
  }

  type Mutation {
    ask(sessionId: String!, message: String!): AskResult!
    createSession(sessionId: String!): Boolean!
    recordCommand(command: String!, source: String!): Boolean!
    setRoom(room: String!, on: Boolean!): Boolean!
    publishCmd(data: String!): Boolean!
  }

  type LightsState {
    habitacion: String!
    cocina: String!
    sala: String!
  }

  type CommandCount {
    command: String!
    count: Int!
  }

  type TopCommand {
    command: String!
    count: Int!
  }
`;
const resolvers = {
  Query: {
    health: async () => "ok",
    history: async (_, { sessionId }) => {
      const url = `${CHATBOT_BASE}/api/chat/history?sessionId=${encodeURIComponent(
        sessionId
      )}`;
      const res = await fetch(url);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      return data;
    },
    lightsState: async () => {
      const url = `${ANALYTICS_BASE}/api/analytics/lights/state`;
      const res = await fetch(url);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      return data;
    },
    commandCounts: async () => {
      const url = `${ANALYTICS_BASE}/api/analytics/commands/counts`;
      const res = await fetch(url);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const raw = await res.json();
      const data = raw && typeof raw === "object" ? raw : {};
      const list = Object.entries(data).map(([command, count]) => ({
        command,
        count: Number(count),
      }));
      return list.filter((c) => c.count > 0);
    },
    topSince: async (_, { since }) => {
      const url = `${ANALYTICS_BASE}/api/analytics/commands/top?since=${encodeURIComponent(
        since
      )}`;
      const res = await fetch(url);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const raw = await res.json();
      const obj = raw && typeof raw === "object" ? raw : {};
      const countsObj =
        obj.counts && typeof obj.counts === "object" ? obj.counts : {};
      const list = Object.entries(countsObj).map(([command, count]) => ({
        command,
        count: Number(count),
      }));
      list.sort((a, b) => b.count - a.count);
      return list;
    },
  },
  Mutation: {
    ask: async (_, { sessionId, message }) => {
      const url = `${CHATBOT_BASE}/api/chat/ask`;
      const res = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sessionId, message }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      return { message: data };
    },
    createSession: async (_, { sessionId }) => {
      const url = `${CHATBOT_BASE}/api/chat/session`;
      const res = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sessionId }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      return true;
    },
    recordCommand: async (_, { command, source }) => {
      const url = `${ANALYTICS_BASE}/api/analytics/commands/record?command=${encodeURIComponent(
        command
      )}&source=${encodeURIComponent(source)}`;
      const res = await fetch(url, { method: "POST" });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      return true;
    },
    setRoom: async (_, { room, on }) => {
      // Map room to path segment expected by ros-cli-api: hab|coc|sal
      const map = {
        habitacion: "hab",
        hab: "hab",
        cocina: "coc",
        coc: "coc",
        sala: "sal",
        sal: "sal",
      };
      const seg = map[room.toLowerCase()];
      if (!seg) throw new Error("invalid room");
      const state = on ? "on" : "off";
      const url = `${ROS_BASE}/${seg}/${state}`;
      const res = await fetch(url);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      return true;
    },
    publishCmd: async (_, { data }) => {
      const url = `${ROS_BASE}/cmd`;
      const res = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ data }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      return true;
    },
  },
};
async function main() {
  const server = new ApolloServer({ typeDefs, resolvers });
  const { url } = await startStandaloneServer(server, {
    listen: { port: parseInt(process.env.PORT || "8090") },
  });
  console.log(`GraphQL Gateway running at ${url}`);
}
main().catch((e) => {
  console.error(e);
  process.exit(1);
});
