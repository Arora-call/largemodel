<template>
  <div class="dashboard">
    <h2 class="greeting">Welcome to the Admin Dashboard</h2>
    <div class="cards">
      <div class="card">
        <h4>Total Users</h4>
        <p class="value">1,234</p>
      </div>
      <div class="card">
        <h4>Revenue</h4>
        <p class="value">$12,345</p>
      </div>
      <div class="card">
        <h4>Orders</h4>
        <p class="value">567</p>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.dashboard {
  padding: 10px;
}

.greeting {
  margin-bottom: 30px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.card {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.card h4 {
  font-size: 14px;
  color: #888;
  margin-bottom: 10px;
}

.card .value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}
</style>