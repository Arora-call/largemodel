<template>
  <header class="header">
    <h3 class="page-title">{{ currentPage }}</h3>
    <div class="user-info">
      <span class="avatar">👤</span>
      <span class="username">Admin</span>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const currentPage = computed(() => {
  return route.meta.title || route.name || 'Dashboard'
})
</script>

<style scoped>
.header {
  background: #fff;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  flex-shrink: 0;
}

.page-title {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar {
  font-size: 24px;
}

.username {
  font-size: 14px;
  color: #666;
}
</style>