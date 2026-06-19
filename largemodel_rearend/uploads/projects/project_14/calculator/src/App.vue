<template>
  <div class="calculator-container">
    <Calculator />
  </div>
</template>

<script setup>
import Calculator from './components/Calculator.vue'
</script>

<style scoped>
.calculator-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #1e1e2f, #2a2a3e);
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}
</style>