const GQL_URL = import.meta.env?.VITE_GRAPHQL_BASE || "http://localhost:8090/";

async function gql(query, variables = {}) {
  const res = await fetch(GQL_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query, variables }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const json = await res.json();
  if (json.errors)
    throw new Error(json.errors.map((e) => e.message).join("; "));
  return json.data;
}

export async function health() {
  const data = await gql("query { health }");
  return data.health;
}

export async function createSession(sessionId) {
  const data = await gql(
    "mutation($sid:String!){ createSession(sessionId:$sid) }",
    { sid: sessionId }
  );
  return data.createSession;
}

export async function history(sessionId) {
  const data = await gql(
    "query($sid:String!){ history(sessionId:$sid){ id role text ts } }",
    { sid: sessionId }
  );
  return data.history;
}

export async function ask(sessionId, message) {
  const data = await gql(
    "mutation($sid:String!,$msg:String!){ ask(sessionId:$sid,message:$msg){ message { id sessionId role text ts } } }",
    { sid: sessionId, msg: message }
  );
  return data.ask.message;
}

export async function lightsState() {
  const data = await gql("query { lightsState { habitacion cocina sala } }");
  return data.lightsState;
}

export async function commandCounts() {
  const data = await gql("query { commandCounts { command count } }");
  return data.commandCounts;
}

export async function topSince(iso) {
  const data = await gql(
    "query($since:String!){ topSince(since:$since){ command count } }",
    { since: iso }
  );
  return data.topSince;
}

// events removed from UI
