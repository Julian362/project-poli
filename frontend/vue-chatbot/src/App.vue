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
.app-shell {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 10px 30px;
}
.nav {
  display: flex;
  gap: 14px;
  padding: 14px 4px 10px;
  border-bottom: 1px solid #2a3036;
  margin-bottom: 12px;
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
  min-height: 600px;
}
@media (max-width: 640px) {
  .nav {
    flex-wrap: wrap;
  }
}
</style>
