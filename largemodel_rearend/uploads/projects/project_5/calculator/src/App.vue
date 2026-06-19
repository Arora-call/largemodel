<template>
  <div class="app-wrapper">
    <header class="app-header">
      <h1>🧮 计算器</h1>
      <p class="subtitle">Vue3 科学计算</p>
    </header>
    <main>
      <Calculator />
    </main>
    <footer class="app-footer">
      <p>Vue3 + Vite</p>
    </footer>
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
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: linear-gradient(145deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  min-height: 100vh;
  color: #e0e0e0;
}

#app {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
}

.app-wrapper {
  width: 100%;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.app-header {
  text-align: center;
  margin-bottom: 24px;
}

.app-header h1 {
  font-size: 2rem;
  font-weight: 700;
  background: linear-gradient(135deg, #f7971e, #ffd200);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 2px;
}

.subtitle {
  font-size: 0.85rem;
  color: #8899aa;
  margin-top: 4px;
  letter-spacing: 4px;
  text-transform: uppercase;
}

.app-footer {
  margin-top: 24px;
  font-size: 0.75rem;
  color: #556677;
  letter-spacing: 1px;
}
</style>