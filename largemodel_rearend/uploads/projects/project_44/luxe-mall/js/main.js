/**
 * LUXE 商城首页 - 交互脚本
 * 功能：顶部通知条关闭、导航栏滚动效果、移动端菜单、轮播图、商品渲染
 */

(() => {
  'use strict';

  // ==========================================
  // 1. 顶部通知条关闭
  // ==========================================
  const topBar = document.getElementById('topBarClose');
  if (topBar) {
    topBar.addEventListener('click', () => {
      const bar = topBar.parentElement;
      bar.classList.add('hidden');
      // 减少 header 的 sticky 偏移
      document.documentElement.style.setProperty('--top-bar-height', '0px');
    });
  }

  // ==========================================
  // 2. 导航栏滚动效果
  // ==========================================
  const header = document.getElementById('header');
  let lastScroll = 0;

  window.addEventListener('scroll', () => {
    const current = window.pageYOffset || document.documentElement.scrollTop;
    if (current > 20) {
      header.classList.add('scrolled');
    } else {
      header.classList.remove('scrolled');
    }
    lastScroll = current;
  }, { passive: true });

  // ==========================================
  // 3. 移动端菜单
  // ==========================================
  const menuToggle = document.getElementById('menuToggle');
  const nav = document.getElementById('nav');

  if (menuToggle && nav) {
    menuToggle.addEventListener('click', () => {
      menuToggle.classList.toggle('active');
      nav.classList.toggle('open');
      document.body.style.overflow = nav.classList.contains('open') ? 'hidden' : '';
    });

    // 点击导航链接后关闭菜单（移动端）
    nav.querySelectorAll('.nav-link').forEach(link => {
      link.addEventListener('click', () => {
        menuToggle.classList.remove('active');
        nav.classList.remove('open');
        document.body.style.overflow = '';
      });
    });

    // 点击外部关闭菜单
    document.addEventListener('click', (e) => {
      if (nav.classList.contains('open') &&
          !nav.contains(e.target) &&
          !menuToggle.contains(e.target)) {
        menuToggle.classList.remove('active');
        nav.classList.remove('open');
        document.body.style.overflow = '';
      }
    });
  }

  // ==========================================
  // 4. 轮播图
  // ==========================================
  const slider = document.getElementById('heroSlider');
  const slides = slider ? slider.querySelectorAll('.hero-slide') : [];
  const dots = document.querySelectorAll('#heroDots .dot');
  const prevBtn = document.getElementById('heroPrev');
  const nextBtn = document.getElementById('heroNext');
  let currentIndex = 0;
  let autoplayTimer = null;
  const AUTOPLAY_INTERVAL = 5000;

  function goToSlide(index) {
    if (slides.length === 0) return;
    slides.forEach((slide, i) => {
      slide.classList.toggle('active', i === index);
    });
    dots.forEach((dot, i) => {
      dot.classList.toggle('active', i === index);
    });
    currentIndex = index;
  }

  function nextSlide() {
    const next = (currentIndex + 1) % slides.length;
    goToSlide(next);
  }

  function prevSlide() {
    const prev = (currentIndex - 1 + slides.length) % slides.length;
    goToSlide(prev);
  }

  function startAutoplay() {
    stopAutoplay();
    autoplayTimer = setInterval(nextSlide, AUTOPLAY_INTERVAL);
  }

  function stopAutoplay() {
    if (autoplayTimer) {
      clearInterval(autoplayTimer);
      autoplayTimer = null;
    }
  }

  // 初始化轮播
  if (slides.length > 0) {
    goToSlide(0);
    startAutoplay();

    // 事件绑定
    if (nextBtn) nextBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      nextSlide();
      startAutoplay();
    });
    if (prevBtn) prevBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      prevSlide();
      startAutoplay();
    });

    // 指示器点击
    dots.forEach((dot, i) => {
      dot.addEventListener('click', () => {
        goToSlide(i);
        startAutoplay();
      });
    });

    // 鼠标悬浮暂停
    const hero = document.getElementById('hero');
    if (hero) {
      hero.addEventListener('mouseenter', stopAutoplay);
      hero.addEventListener('mouseleave', startAutoplay);
    }

    // 触摸滑动支持
    let touchStartX = 0;
    let touchEndX = 0;
    slider.addEventListener('touchstart', (e) => {
      touchStartX = e.changedTouches[0].screenX;
    }, { passive: true });
    slider.addEventListener('touchend', (e) => {
      touchEndX = e.changedTouches[0].screenX;
      const diff = touchStartX - touchEndX;
      if (Math.abs(diff) > 50) {
        if (diff > 0) nextSlide();
        else prevSlide();
        startAutoplay();
      }
    }, { passive: true });
  }

  // ==========================================
  // 5. 商品数据
  // ==========================================
  const products = [
    {
      id: 1,
      name: '经典双排扣羊毛大衣',
      brand: 'LUXE Collection',
      price: 3680,
      originalPrice: 4680,
      badge: '热卖',
      badgeType: 'hot',
      image: 'https://images.unsplash.com/photo-1539533113208-f6df8cc8b543?w=600&q=80',
    },
    {
      id: 2,
      name: '真丝印花衬衫',
      brand: 'LUXE Studio',
      price: 1890,
      originalPrice: 2390,
      badge: '新品',
      badgeType: 'new',
      image: 'https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=600&q=80',
    },
    {
      id: 3,
      name: '小牛皮信封包',
      brand: 'LUXE Atelier',
      price: 5200,
      originalPrice: null,
      badge: null,
      badgeType: '',
      image: 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?w=600&q=80',
    },
    {
      id: 4,
      name: '方扣粗跟短靴',
      brand: 'LUXE Shoes',
      price: 2680,
      originalPrice: 3280,
      badge: '热卖',
      badgeType: 'hot',
      image: 'https://images.unsplash.com/photo-1603808033192-082d6919d3e1?w=600&q=80',
    },
    {
      id: 5,
      name: '18K金珍珠耳坠',
      brand: 'LUXE Jewelry',
      price: 4350,
      originalPrice: null,
      badge: '新品',
      badgeType: 'new',
      image: 'https://images.unsplash.com/photo-1515562141589-6770d12c0c1c?w=600&q=80',
    },
    {
      id: 6,
      name: '羊绒混纺围巾',
      brand: 'LUXE Accessories',
      price: 1280,
      originalPrice: 1680,
      badge: null,
      badgeType: '',
      image: 'https://images.unsplash.com/photo-1600978257452-c6c0bc8660d4?w=600&q=80',
    },
    {
      id: 7,
      name: '高腰阔腿西裤',
      brand: 'LUXE Tailoring',
      price: 2180,
      originalPrice: 2780,
      badge: '热卖',
      badgeType: 'hot',
      image: 'https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?w=600&q=80',
    },
    {
      id: 8,
      name: '雾面哑光唇膏礼盒',
      brand: 'LUXE Beauty',
      price: 690,
      originalPrice: null,
      badge: '新品',
      badgeType: 'new',
      image: 'https://images.unsplash.com/photo-1586495777744-4413f21062fa?w=600&q=80',
    },
  ];

  // ==========================================
  // 6. 渲染商品卡片
  // ==========================================
  const productGrid = document.getElementById('productGrid');

  function renderProducts() {
    if (!productGrid) return;

    productGrid.innerHTML = products.map(product => {
      const badgeHTML = product.badge
        ? `<span class="product-badge ${product.badgeType}">${product.badge}</span>`
        : '';

      const originalHTML = product.originalPrice
        ? `<span class="price-original">¥${product.originalPrice.toLocaleString()}</span>`
        : '';

      return `
        <div class="product-card" data-id="${product.id}">
          ${badgeHTML}
          <div class="product-img-wrap">
            <img
              src="${product.image}"
              alt="${product.name}"
              loading="lazy"
            />
          </div>
          <div class="product-info">
            <div class="product-brand">${product.brand}</div>
            <div class="product-name">${product.name}</div>
            <div class="product-price">
              <span class="price-current">¥${product.price.toLocaleString()}</span>
              ${originalHTML}
            </div>
            <div class="product-actions">
              <button class="btn-add-cart" data-id="${product.id}">加入购物车</button>
              <button class="btn-wishlist" data-id="${product.id}" aria-label="收藏">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
              </button>
            </div>
          </div>
        </div>
      `;
    }).join('');

    // 绑定加入购物车事件
    productGrid.querySelectorAll('.btn-add-cart').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const id = parseInt(btn.dataset.id);
        const product = products.find(p => p.id === id);
        if (product) {
          // 更新购物车角标
          const badge = document.querySelector('.cart-badge');
          if (badge) {
            const count = parseInt(badge.textContent) + 1;
            badge.textContent = count;
            // 小动画
            badge.style.transform = 'translate(4px, -4px) scale(1.3)';
            setTimeout(() => {
              badge.style.transform = 'translate(4px, -4px) scale(1)';
            }, 300);
          }
          // 提示（轻反馈）
          btn.textContent = '已加入 ✓';
          btn.style.borderColor = '#c9a96e';
          btn.style.color = '#c9a96e';
          setTimeout(() => {
            btn.textContent = '加入购物车';
            btn.style.borderColor = '';
            btn.style.color = '';
          }, 1200);
        }
      });
    });

    // 绑定收藏按钮事件
    productGrid.querySelectorAll('.btn-wishlist').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        btn.classList.toggle('wished');
        if (btn.classList.contains('wished')) {
          btn.style.borderColor = '#d45858';
          btn.style.color = '#d45858';
          btn.style.background = 'rgba(212, 88, 88, 0.08)';
        } else {
          btn.style.borderColor = '';
          btn.style.color = '';
          btn.style.background = '';
        }
      });
    });

    // 点击卡片跳转（模拟）
    productGrid.querySelectorAll('.product-card').forEach(card => {
      card.addEventListener('click', () => {
        const id = card.dataset.id;
        // 示意页面跳转
        console.log(`[LUXE] 跳转至商品详情页，ID: ${id}`);
      });
    });
  }

  // ==========================================
  // 7. 初始化
  // ==========================================
  document.addEventListener('DOMContentLoaded', () => {
    renderProducts();
    console.log('[LUXE] 商城首页已加载');
  });
})();