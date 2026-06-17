<template>
  <div class="auth-layout">
    <div class="auth-container">
      <div class="auth-header">
        <h1 class="auth-title">大模型应用平台</h1>
        <p class="auth-subtitle">代码应用生成与管理</p>
      </div>
      <div class="auth-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.auth-container {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.auth-subtitle {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.auth-content {
  width: 100%;
}
</style>
