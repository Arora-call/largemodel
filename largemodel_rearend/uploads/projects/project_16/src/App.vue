<template>
  <div id="app">
    <NavBar />
    <main class="main-content">
      <Carousel :slides="slides" />
      <section class="product-section">
        <h2 class="section-title">热门推荐</h2>
        <div class="product-grid">
          <ProductCard v-for="product in products" :key="product.id" :product="product" />
        </div>
      </section>
    </main>
    <footer class="footer">
      <p>&copy; 2025 乐购商城 版权所有</p>
    </footer>
  </div>
</template>

<script setup>
import NavBar from './components/NavBar.vue'
import Carousel from './components/Carousel.vue'
import ProductCard from './components/ProductCard.vue'

const slides = [
  { id: 1, image: 'https://picsum.photos/seed/banner1/1200/400', title: '春季新品上市', subtitle: '全场低至5折' },
  { id: 2, image: 'https://picsum.photos/seed/banner2/1200/400', title: '数码狂欢节', subtitle: '满1000减200' },
  { id: 3, image: 'https://picsum.photos/seed/banner3/1200/400', title: '家居焕新季', subtitle: '精选好物限量抢' }
]

const products = [
  { id: 1, name: '无线蓝牙耳机', price: 299, originalPrice: 499, image: 'https://picsum.photos/seed/p1/300/300', rating: 4.5, sales: 2345 },
  { id: 2, name: '智能运动手表', price: 899, originalPrice: 1299, image: 'https://picsum.photos/seed/p2/300/300', rating: 4.8, sales: 1876 },
  { id: 3, name: '轻薄笔记本电脑', price: 5299, originalPrice: 5999, image: 'https://picsum.photos/seed/p3/300/300', rating: 4.6, sales: 987 },
  { id: 4, name: '便携式咖啡机', price: 399, originalPrice: 599, image: 'https://picsum.photos/seed/p4/300/300', rating: 4.3, sales: 3210 },
  { id: 5, name: '高端机械键盘', price: 459, originalPrice: 699, image: 'https://picsum.photos/seed/p5/300/300', rating: 4.7, sales: 1567 },
  { id: 6, name: '降噪头戴耳机', price: 799, originalPrice: 1099, image: 'https://picsum.photos/seed/p6/300/300', rating: 4.9, sales: 4123 },
  { id: 7, name: '4K高清投影仪', price: 2499, originalPrice: 3299, image: 'https://picsum.photos/seed/p7/300/300', rating: 4.4, sales: 876 },
  { id: 8, name: '智能扫地机器人', price: 1899, originalPrice: 2399, image: 'https://picsum.photos/seed/p8/300/300', rating: 4.6, sales: 2340 }
]
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background-color: #f5f5f5;
  color: #333;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  margin: 40px 0 20px;
  padding-left: 12px;
  border-left: 4px solid #e4393c;
  color: #222;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
}

.footer {
  text-align: center;
  padding: 30px 0;
  background-color: #fff;
  border-top: 1px solid #eee;
  color: #999;
  font-size: 14px;
}
</style>