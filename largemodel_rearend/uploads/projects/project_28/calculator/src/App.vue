<template>
  <div class="app-wrapper">
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
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #1a1a2e;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.app-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
</style>