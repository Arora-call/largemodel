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

<style scoped>
.auth-layout {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(ellipse at 30% 20%, #2a1a4a 0%, transparent 60%),
              radial-gradient(ellipse at 70% 80%, #1a2a5a 0%, transparent 60%),
              linear-gradient(135deg, #0a0d1a 0%, #12183a 50%, #0d1225 100%);
}

.auth-container {
  width: 430px;
  padding: 44px 40px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(24px);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255,255,255,0.06);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
  letter-spacing: 1px;
}

.auth-subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.auth-content {
  width: 100%;
}
</style>
