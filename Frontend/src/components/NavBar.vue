<template>
  <header class="navbar">
    <div class="navbar-brand">Documents</div>
    <nav class="navbar-links">
      <RouterLink to="/" :class="{ active: route.path === '/' }">Search</RouterLink>
      <RouterLink to="/create" :class="{ active: route.path === '/create' }">Create</RouterLink>
      <RouterLink to="/chat" :class="{ active: route.path === '/chat' }">AI Chat</RouterLink>
    </nav>
  </header>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 36px;
  padding: 0 16px;
  background: #1e293b;
  border-bottom: 1px solid #0f172a;
  flex-shrink: 0;
}

.navbar-brand {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #94a3b8;
  user-select: none;
}

.navbar-links {
  display: flex;
  align-items: center;
  gap: 2px;
  height: 100%;
}

.navbar-links a {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 12px;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.05em;
  color: #64748b;
  border-bottom: 2px solid transparent;
  transition: color 0.1s, border-color 0.1s;
}

.navbar-links a:hover { color: #cbd5e1; }

.navbar-links a.active {
  color: #f8fafc;
  border-bottom-color: #3b82f6;
}
</style>
