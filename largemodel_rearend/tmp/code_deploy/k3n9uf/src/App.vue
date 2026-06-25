<template>
  <div id="app-layout" :class="{ 'no-sidebar': isLogin }">
    <Sidebar v-if="!isLogin" />
    <div class="main-content">
      <Header v-if="!isLogin" />
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from '@/components/Sidebar.vue'
import Header from '@/components/Header.vue'

const route = useRoute()
const isLogin = computed(() => route.name === 'Login')
</script>

<style scoped>
#app-layout {
  display: flex;
  height: 100vh;
}
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
</style>