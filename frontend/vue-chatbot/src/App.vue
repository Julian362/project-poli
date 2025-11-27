<template>
  <div class="app-shell">
    <nav class="nav">
      <RouterLink to="/" class="link" active-class="active" exact
        >Chat</RouterLink
      >
      <RouterLink to="/historial" class="link" active-class="active"
        >Historial & Stats</RouterLink
      >
    </nav>
    <div class="view-wrap">
      <RouterView />
    </div>
  </div>
</template>

<script setup>
// Shell only uses router now
</script>

<style scoped>
:global(html),
:global(body),
:global(#app),
.app-shell {
  height: 100svh;
  max-height: 100svh;
}
@supports (height: 100dvh) {
  :global(html),
  :global(body),
  :global(#app),
  .app-shell {
    height: 100dvh;
    max-height: 100dvh;
  }
}
:global(html),
:global(body) {
  overflow: hidden;
}
.app-shell {
  box-sizing: border-box;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 16px 12px;
  display: flex;
  flex-direction: column;
}
.nav {
  display: flex;
  gap: 10px;
  padding: 10px 4px 8px;
  border-bottom: 1px solid #2a3036;
  margin-bottom: 6px;
}
.link {
  text-decoration: none;
  color: #b9c4ce;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 8px;
  background: #21262c;
  transition: background 0.15s, color 0.15s;
}
.link:hover {
  background: #2a3138;
  color: #e6ebef;
}
.active {
  background: #2f5e35;
  color: #fff;
}
.view-wrap {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding-bottom: env(safe-area-inset-bottom, 0px);
}
@media (max-width: 640px) {
  .nav {
    flex-wrap: wrap;
  }
}
</style>
