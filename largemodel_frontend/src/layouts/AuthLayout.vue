<template>
  <div class="auth-layout">
    <!-- 左侧品牌展示区 -->
    <div class="auth-brand">
      <div class="brand-content">
        <div class="brand-logo">
          <span class="logo-dot-large"></span>
          <span class="logo-text-large">CodeForge</span>
        </div>
        <h1 class="brand-headline">AI 驱动的代码锻造平台</h1>
        <p class="brand-desc">用自然语言描述你的需求，AI 自动生成完整的前后端代码、工程项目，支持实时预览和编辑。</p>
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-icon">✨</span>
            <span>智能代码生成</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">📦</span>
            <span>完整工程项目</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">👁</span>
            <span>实时预览 & 编辑</span>
          </div>
        </div>
      </div>
      <!-- 装饰背景 -->
      <div class="brand-bg">
        <div class="bg-orb orb-1"></div>
        <div class="bg-orb orb-2"></div>
        <div class="bg-orb orb-3"></div>
        <div class="bg-grid"></div>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="auth-form-panel">
      <div class="form-wrapper">
        <router-view />
      </div>
      <div class="form-footer-bar">
        <span>&copy; 2026 CodeForge</span>
        <span>AI 驱动的代码锻造平台</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-layout {
  display: flex;
  min-height: 100vh;
  width: 100%;
}

/* ====== Brand Panel ====== */
.auth-brand {
  flex: 0 0 48%;
  position: relative;
  background: linear-gradient(135deg, #0a0d13 0%, #111630 50%, #0d1225 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.brand-content {
  position: relative;
  z-index: 2;
  max-width: 460px;
  padding: 60px 48px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 40px;
}

.logo-dot-large {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #7c8aff, #a78bfa);
  box-shadow: 0 4px 20px rgba(124, 138, 255, 0.4);
}

.logo-text-large {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  letter-spacing: -1px;
}

.brand-headline {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 16px;
  line-height: 1.3;
  letter-spacing: -0.5px;
}

.brand-desc {
  font-size: 16px;
  color: #8b9496;
  line-height: 1.7;
  margin: 0 0 36px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  color: #c9d1d9;
}

.feature-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(124, 138, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

/* Brand background decorations */
.brand-bg {
  position: absolute;
  inset: 0;
  z-index: 1;
  overflow: hidden;
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: #7c8aff;
  top: -100px;
  right: -100px;
}

.orb-2 {
  width: 300px;
  height: 300px;
  background: #a78bfa;
  bottom: -50px;
  left: -50px;
  opacity: 0.2;
}

.orb-3 {
  width: 200px;
  height: 200px;
  background: #6366f1;
  top: 50%;
  left: 30%;
  opacity: 0.15;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.02) 1px, transparent 1px);
  background-size: 60px 60px;
}

/* ====== Form Panel ====== */
.auth-form-panel {
  flex: 0 0 52%;
  background: var(--bg-primary);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.form-wrapper {
  width: 100%;
  max-width: 400px;
  padding: 40px 20px;
}

.form-footer-bar {
  position: absolute;
  bottom: 24px;
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-dim);
}

/* ====== Responsive ====== */
@media (max-width: 900px) {
  .auth-brand {
    display: none;
  }
  .auth-form-panel {
    flex: 1;
  }
}
</style>
