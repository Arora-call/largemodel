<template>
  <div class="dashboard">
    <h1>仪表盘</h1>
    <div class="cards">
      <div class="card">用户数: 120</div>
      <div class="card">订单数: 45</div>
      <div class="card">收入: ¥12,000</div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 20px;
}
.cards {
  display: flex;
  gap: 20px;
  margin-top: 20px;
}
.card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  flex: 1;
}
</style>