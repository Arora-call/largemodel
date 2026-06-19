<template>
  <div class="app">
    <Calculator />
  </div>
</template>

<script setup>
import Calculator from './components/Calculator.vue'
</script>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  margin: 0;
}
</style>