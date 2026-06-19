<template>
  <header class="navbar">
    <div class="navbar-inner">
      <div class="logo">
        <i class="fas fa-shopping-bag"></i>
        <span>乐购商城</span>
      </div>

      <nav class="nav-menu">
        <a href="#" class="nav-item active">首页</a>
        <a href="#" class="nav-item">手机数码</a>
        <a href="#" class="nav-item">电脑办公</a>
        <a href="#" class="nav-item">家用电器</a>
        <a href="#" class="nav-item">服饰鞋包</a>
        <a href="#" class="nav-item">美妆个护</a>
      </nav>

      <div class="search-box">
        <input type="text" v-model="keyword" placeholder="搜索商品..." @keyup.enter="handleSearch" />
        <button @click="handleSearch"><i class="fas fa-search"></i></button>
      </div>

      <div class="user-actions">
        <a href="#" class="action-btn"><i class="fas fa-user"></i><span>我的</span></a>
        <a href="#" class="action-btn cart-btn">
          <i class="fas fa-shopping-cart"></i>
          <span>购物车</span>
          <span class="cart-badge">3</span>
        </a>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'

const keyword = ref('')

function handleSearch() {
  if (keyword.value.trim()) {
    alert('搜索: ' + keyword.value)
    keyword.value = ''
  }
}
</script>

<style scoped>
.navbar {
  background: linear-gradient(135deg, #e4393c 0%, #c1272d 100%);
  color: #fff;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 64px;
  gap: 30px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 22px;
  font-weight: 700;
  cursor: pointer;
  flex-shrink: 0;
}

.logo i {
  font-size: 28px;
}

.nav-menu {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.nav-item {
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
  padding: 8px 14px;
  border-radius: 4px;
  font-size: 15px;
  transition: all 0.25s;
}

.nav-item:hover,
.nav-item.active {
  color: #fff;
  background: rgba(255, 255, 255, 0.2);
}

.search-box {
  display: flex;
  align-items: center;
  flex: 1;
  max-width: 360px;
  margin: 0 auto;
}

.search-box input {
  flex: 1;
  padding: 9px 14px;
  border: none;
  border-radius: 4px 0 0 4px;
  outline: none;
  font-size: 14px;
}

.search-box button {
  padding: 9px 16px;
  background: #fdd835;
  border: none;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
  color: #333;
  font-size: 16px;
  transition: background 0.25s;
}

.search-box button:hover {
  background: #fbc02d;
}

.user-actions {
  display: flex;
  gap: 18px;
  flex-shrink: 0;
}

.action-btn {
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  transition: color 0.25s;
  position: relative;
}

.action-btn:hover {
  color: #fff;
}

.cart-badge {
  position: absolute;
  top: -8px;
  right: -12px;
  background: #ffd600;
  color: #333;
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 10px;
  line-height: 1.4;
}

@media (max-width: 768px) {
  .nav-menu {
    display: none;
  }
}
</style>