<template>
  <div class="carousel-container" @mouseenter="pause" @mouseleave="resume">
    <div class="carousel-track" :style="{ transform: `translateX(-${currentIndex * 100}%)` }">
      <div v-for="slide in slides" :key="slide.id" class="carousel-slide">
        <img :src="slide.image" :alt="slide.title" />
        <div class="slide-overlay">
          <h2>{{ slide.title }}</h2>
          <p>{{ slide.subtitle }}</p>
        </div>
      </div>
    </div>

    <button class="carousel-btn carousel-prev" @click="prev">
      <i class="fas fa-chevron-left"></i>
    </button>
    <button class="carousel-btn carousel-next" @click="next">
      <i class="fas fa-chevron-right"></i>
    </button>

    <div class="carousel-dots">
      <span
        v-for="(slide, index) in slides"
        :key="'dot-' + slide.id"
        :class="['dot', { active: index === currentIndex }]"
        @click="goTo(index)"
      ></span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  slides: {
    type: Array,
    required: true
  }
})

const currentIndex = ref(0)
let timer = null

function next() {
  currentIndex.value = (currentIndex.value + 1) % props.slides.length
}

function prev() {
  currentIndex.value = (currentIndex.value - 1 + props.slides.length) % props.slides.length
}

function goTo(index) {
  currentIndex.value = index
}

function startAutoPlay() {
  timer = setInterval(() => {
    next()
  }, 4000)
}

function pause() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function resume() {
  if (!timer) {
    startAutoPlay()
  }
}

onMounted(() => {
  startAutoPlay()
})

onUnmounted(() => {
  pause()
})
</script>

<style scoped>
.carousel-container {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  background: #ddd;
}

.carousel-track {
  display: flex;
  transition: transform 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  height: 100%;
}

.carousel-slide {
  min-width: 100%;
  position: relative;
}

.carousel-slide img {
  width: 100%;
  height: 360px;
  object-fit: cover;
  display: block;
}

.slide-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 24px 30px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.6));
  color: #fff;
}

.slide-overlay h2 {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 6px;
}

.slide-overlay p {
  font-size: 16px;
  opacity: 0.9;
}

.carousel-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  border: none;
  cursor: pointer;
  font-size: 18px;
  color: #333;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s, background 0.3s;
  z-index: 10;
}

.carousel-container:hover .carousel-btn {
  opacity: 1;
}

.carousel-btn:hover {
  background: #fff;
}

.carousel-prev {
  left: 16px;
}

.carousel-next {
  right: 16px;
}

.carousel-dots {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  z-index: 10;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s;
}

.dot.active {
  background: #fff;
  transform: scale(1.3);
}

@media (max-width: 768px) {
  .carousel-slide img {
    height: 220px;
  }

  .slide-overlay h2 {
    font-size: 20px;
  }

  .slide-overlay p {
    font-size: 14px;
  }
}
</style>