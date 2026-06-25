<template>
  <header class="header">
    <h2>{{ pageTitle }}</h2>
    <div class="user-info">
      管理员
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const pageTitle = computed(() => {
  const map = {
    'Dashboard': '仪表盘',
    'Users': '用户管理',
    'Login': '登录'
  }
  return map[route.name] || '后台管理'
})
</script>

<style scoped>
.header {
  background: white;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e8e8e8;
}
</style>