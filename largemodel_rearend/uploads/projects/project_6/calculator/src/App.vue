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
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background: #1a1a2e;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
}

.app-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}
</style>