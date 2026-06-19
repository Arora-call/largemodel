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
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f0f2f5;
}
#app {
  width: 100%;
  display: flex;
  justify-content: center;
}
</style>