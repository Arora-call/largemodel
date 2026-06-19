<template>
  <div class="app">
    <Calculator />
  </div>
</template>

<script setup>
import Calculator from './components/Calculator.vue'
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background: #f0f0f0;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
}
.app {
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>