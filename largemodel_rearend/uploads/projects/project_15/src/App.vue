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
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}
body {
  font-family: Arial, sans-serif;
  background-color: #f5f5f5;
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