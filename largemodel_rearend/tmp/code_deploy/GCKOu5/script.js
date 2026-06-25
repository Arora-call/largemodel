// ===== 状态 =====
let currentCity = '北京';
let isFetching = false;

// ===== DOM 引用 =====
const searchInput = document.getElementById('searchInput');
const searchBtn = document.getElementById('searchBtn');
const loading = document.getElementById('loading');
const error = document.getElementById('error');
const retryBtn = document.getElementById('retryBtn');
const weatherContent = document.getElementById('weatherContent');
const cityName = document.getElementById('cityName');
const currentDate = document.getElementById('currentDate');
const weatherIconLarge = document.getElementById('weatherIconLarge');
const temperature = document.getElementById('temperature');
const weatherDesc = document.getElementById('weatherDesc');
const feelsLike = document.getElementById('feelsLike');
const humidity = document.getElementById('humidity');
const windSpeed = document.getElementById('windSpeed');
const precipitation = document.getElementById('precipitation');
const forecastGrid = document.getElementById('forecastGrid');

// ===== 天气代码映射 =====
function getWeatherInfo(code) {
  const map = {
    0: { text: '晴天', icon: '☀️' },
    1: { text: '多云', icon: '🌤️' },
    2: { text: '阴天', icon: '⛅' },
    3: { text: '阴天', icon: '☁️' },
    45: { text: '雾', icon: '🌫️' },
    48: { text: '雾凇', icon: '🌫️' },
    51: { text: '小雨', icon: '🌦️' },
    53: { text: '中雨', icon: '🌦️' },
    55: { text: '大雨', icon: '🌧️' },
    56: { text: '冻雨', icon: '🌧️' },
    57: { text: '冻雨', icon: '🌧️' },
    61: { text: '小雨', icon: '🌧️' },
    63: { text: '中雨', icon: '🌧️' },
    65: { text: '大雨', icon: '🌧️' },
    66: { text: '冻雨', icon: '❄️' },
    67: { text: '冻雨', icon: '❄️' },
    71: { text: '小雪', icon: '🌨️' },
    73: { text: '中雪', icon: '🌨️' },
    75: { text: '大雪', icon: '❄️' },
    77: { text: '雪粒', icon: '❄️' },
    80: { text: '阵雨', icon: '🌦️' },
    81: { text: '阵雨', icon: '🌦️' },
    82: { text: '暴雨', icon: '🌧️' },
    85: { text: '阵雪', icon: '🌨️' },
    86: { text: '阵雪', icon: '🌨️' },
    95: { text: '雷暴', icon: '⛈️' },
    96: { text: '冰雹', icon: '⛈️' },
    99: { text: '冰雹', icon: '⛈️' },
  };
  return map[code] || { text: '未知', icon: '❓' };
}

// ===== 格式化日期 =====
function formatDate(dateStr) {
  const d = new Date(dateStr);
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  const month = d.getMonth() + 1;
  const day = d.getDate();
  const wd = weekdays[d.getDay()];
  return `${month}月${day}日 ${wd}`;
}

function getDayLabel(dateStr, index) {
  const d = new Date(dateStr);
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  if (index === 0) return '今天';
  if (index === 1) return '明天';
  return weekdays[d.getDay()];
}

function getTodayStr() {
  const d = new Date();
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  const month = d.getMonth() + 1;
  const day = d.getDate();
  const wd = weekdays[d.getDay()];
  return `${d.getFullYear()}年${month}月${day}日 ${wd}`;
}

// ===== 显示/隐藏状态 =====
function showLoading() {
  loading.classList.add('active');
  error.classList.remove('active');
  weatherContent.classList.remove('active');
}

function showError() {
  loading.classList.remove('active');
  error.classList.add('active');
  weatherContent.classList.remove('active');
}

function showWeather() {
  loading.classList.remove('active');
  error.classList.remove('active');
  weatherContent.classList.add('active');
}

// ===== 渲染当前天气 =====
function renderCurrentWeather(data, city) {
  const current = data.current;
  const daily = data.daily;

  cityName.textContent = city;
  currentDate.textContent = getTodayStr();

  const weather = getWeatherInfo(current.weather_code);
  weatherIconLarge.textContent = weather.icon;
  temperature.textContent = Math.round(current.temperature_2m);
  weatherDesc.textContent = weather.text;
  feelsLike.textContent = Math.round(current.apparent_temperature) + '°C';
  humidity.textContent = current.relative_humidity_2m + '%';
  windSpeed.textContent = current.wind_speed_10m.toFixed(1) + ' km/h';
  precipitation.textContent = (current.precipitation || 0).toFixed(1) + ' mm';
}

// ===== 渲染7天预报 =====
function renderForecast(data) {
  const daily = data.daily;
  const count = Math.min(daily.time.length, 7);

  let html = '';
  for (let i = 0; i < count; i++) {
    const dayLabel = getDayLabel(daily.time[i], i);
    const weather = getWeatherInfo(daily.weather_code[i]);
    const tempMax = Math.round(daily.temperature_2m_max[i]);
    const tempMin = Math.round(daily.temperature_2m_min[i]);

    html += `
      <div class="forecast-card">
        <span class="forecast-day">${dayLabel}</span>
        <span class="forecast-icon">${weather.icon}</span>
        <div class="forecast-temps">
          <span class="forecast-temp-max">${tempMax}°</span>
          <span class="forecast-temp-min">${tempMin}°</span>
        </div>
      </div>
    `;
  }
  forecastGrid.innerHTML = html;
}

// ===== 获取城市坐标 =====
async function getCityCoords(city) {
  const url = `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(city)}&count=5&language=zh&format=json`;
  const res = await fetch(url);
  if (!res.ok) throw new Error('Geocoding API 请求失败');
  const data = await res.json();
  if (!data.results || data.results.length === 0) {
    throw new Error('未找到该城市');
  }
  const result = data.results[0];
  return {
    lat: result.latitude,
    lon: result.longitude,
    name: result.name,
    country: result.country || ''
  };
}

// ===== 获取天气数据 =====
async function getWeatherData(lat, lon) {
  const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min,weather_code&timezone=auto&forecast_days=7`;
  const res = await fetch(url);
  if (!res.ok) throw new Error('天气API请求失败');
  return await res.json();
}

// ===== 主流程 =====
async function fetchWeather(city) {
  if (isFetching) return;
  isFetching = true;

  showLoading();

  try {
    const coords = await getCityCoords(city);
    const weatherData = await getWeatherData(coords.lat, coords.lon);

    renderCurrentWeather(weatherData, coords.name);
    renderForecast(weatherData);

    currentCity = coords.name;
    searchInput.value = coords.name;
    showWeather();
  } catch (err) {
    console.error('获取天气失败:', err);
    showError();
  } finally {
    isFetching = false;
  }
}

// ===== 搜索处理 =====
function handleSearch() {
  const city = searchInput.value.trim();
  if (!city) {
    searchInput.focus();
    return;
  }
  fetchWeather(city);
}

// ===== 事件绑定 =====
searchBtn.addEventListener('click', handleSearch);

searchInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') {
    e.preventDefault();
    handleSearch();
  }
});

retryBtn.addEventListener('click', () => {
  fetchWeather(currentCity);
});

// ===== 初始化 =====
document.addEventListener('DOMContentLoaded', () => {
  fetchWeather('北京');
});
