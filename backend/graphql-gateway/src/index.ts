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
    history: async (_: any, { sessionId }: { sessionId: string }) => {
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
      const raw: unknown = await res.json();
      const data: Record<string, number> = (raw && typeof raw === 'object') ? raw as Record<string, number> : {};
      interface CountItem { command: string; count: number; }
      const list: CountItem[] = Object.entries(data).map(([command, count]) => ({
        command,
        count: Number(count)
      }));
      return list.filter((c) => c.count > 0);
    },
    topSince: async (_: any, { since }: { since: string }) => {
      const url = `${ANALYTICS_BASE}/api/analytics/commands/top?since=${encodeURIComponent(
        since
      )}`;
      const res = await fetch(url);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const raw: unknown = await res.json();
      console.log('[topSince] raw response:', raw);
      const obj = (raw && typeof raw === 'object') ? raw as any : {};
      const countsObj: Record<string, number> = (obj.counts && typeof obj.counts === 'object') ? obj.counts : {};
      interface TopItem { command: string; count: number; }
      const list: TopItem[] = Object.entries(countsObj).map(([command, count]) => ({
        command,
        count: Number(count)
      }));
      list.sort((a, b) => b.count - a.count);
      console.log('[topSince] list built:', list);
      return Array.isArray(list) ? list : [];
    },
  },
  Mutation: {
    ask: async (
      _: any,
      { sessionId, message }: { sessionId: string; message: string }
    ) => {
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
    createSession: async (_: any, { sessionId }: { sessionId: string }) => {
      const url = `${CHATBOT_BASE}/api/chat/session`;
      const res = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sessionId }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      return true;
    },
    recordCommand: async (
      _: any,
      { command, source }: { command: string; source: string }
    ) => {
      const url = `${ANALYTICS_BASE}/api/analytics/commands/record?command=${encodeURIComponent(
        command
      )}&source=${encodeURIComponent(source)}`;
      const res = await fetch(url, { method: "POST" });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      return true;
    },
    setRoom: async (_: any, { room, on }: { room: string; on: boolean }) => {
      // Map room to path segment expected by ros-cli-api: hab|coc|sal
      const map: Record<string, string> = {
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
    publishCmd: async (_: any, { data }: { data: string }) => {
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
