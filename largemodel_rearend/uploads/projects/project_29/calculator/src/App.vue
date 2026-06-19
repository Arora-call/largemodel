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
  padding: 0;
  background: #1a1a2e;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}
.app {
  width: 100%;
  display: flex;
  justify-content: center;
}
</style>