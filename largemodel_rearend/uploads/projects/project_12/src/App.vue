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
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #1a1a2e;
  margin: 0;
  font-family: 'Arial', sans-serif;
}
</style>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
</style>