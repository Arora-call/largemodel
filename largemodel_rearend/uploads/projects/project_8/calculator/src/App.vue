<template>
  <div id="app">
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
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #f0f2f5;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
}

#app {
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>