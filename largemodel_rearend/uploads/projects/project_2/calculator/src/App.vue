<template>
  <div class="app">
    <Calculator />
  </div>
</template>

<script setup>
import Calculator from './components/Calculator.vue'
</script>

<style>
body {
  margin: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f0f0f0;
  font-family: Arial, sans-serif;
}
.app {
  width: 100%;
  display: flex;
  justify-content: center;
}
</style>