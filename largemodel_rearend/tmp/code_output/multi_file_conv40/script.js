// ===== DOM 引用 =====
const cityNameEl = document.getElementById('cityName');
const dateTimeEl = document.getElementById('dateTime');
const weatherIconEl = document.getElementById('weatherIcon');
const currentTempEl = document.getElementById('currentTemp');
const weatherDescEl = document.getElementById('weatherDesc');
const feelsLikeEl = document.getElementById('feelsLike');
const humidityEl = document.getElementById('humidity');
const windSpeedEl = document.getElementById('windSpeed');
const aqiEl = document.getElementById('aqi');
const forecastContainer = document.getElementById('forecastContainer');
const searchInput = document.getElementById('searchInput');
const searchBtn = document.getElementById('searchBtn');

// ===== 天气数据模拟 =====
const weatherConditions = [
  { icon: '☀️', desc: '晴' },
  { icon: '⛅', desc: '多云' },
  { icon: '☁️', desc: '阴' },
  { icon: '🌧️', desc: '小雨' },
  { icon: '🌦️', desc: '阵雨' },
  { icon: '⛈️', desc: '雷阵雨' },
  { icon: '🌬️', desc: '大风' },
  { icon: '🌫️', desc: '雾' },
];

// 城市名简单映射
const cityAdjectives = ['阳光', '清新', '活力', '温暖', '宁静', '繁华'];

/** 获取随机天气条件 */
function getRandomCondition() {
  return weatherConditions[Math.floor(Math.random() * weatherConditions.length)];
}

/** 生成 5 天预报数据 */
function generateForecast() {
  const forecast = [];
  const today = new Date();
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

  for (let i = 0; i < 5; i++) {
    const date = new Date(today);
    date.setDate(today.getDate() + i);
    const condition = getRandomCondition();
    const high = Math.floor(Math.random() * 8) + 22; // 22~29
    const low = high - Math.floor(Math.random() * 6) - 2; // 比高温度低2~7
    forecast.push({
      day: i === 0 ? '今天' : weekdays[date.getDay()],
      date: `${date.getMonth() + 1}/${date.getDate()}`,
      icon: condition.icon,
      desc: condition.desc,
      high,
      low,
    });
  }
  return forecast;
}

/** 生成当前天气数据 */
function generateCurrentWeather(city) {
  const condition = getRandomCondition();
  const temp = Math.floor(Math.random() * 10) + 18; // 18~27
  const feelsLike = temp + Math.floor(Math.random() * 4) - 1;
  const humidity = Math.floor(Math.random() * 30) + 40; // 40~69
  const windLevel = Math.floor(Math.random() * 4) + 1; // 1~4
  const aqiOptions = ['优', '良', '轻度污染', '中度污染'];
  const aqi = aqiOptions[Math.floor(Math.random() * 3)]; // 前三个
  return {
    city: city || '北京',
    icon: condition.icon,
    desc: condition.desc,
    temp,
    feelsLike,
    humidity,
    windLevel,
    aqi,
  };
}

// ===== 渲染函数 =====

/** 更新当前天气 */
function renderCurrentWeather(data) {
  cityNameEl.textContent = data.city;
  weatherIconEl.textContent = data.icon;
  currentTempEl.textContent = `${data.temp}°`;
  weatherDescEl.textContent = data.desc;
  feelsLikeEl.textContent = `${data.feelsLike}°`;
  humidityEl.textContent = `${data.humidity}%`;
  windSpeedEl.textContent = `${data.windLevel}级`;
  aqiEl.textContent = data.aqi;

  // 更新时间
  const now = new Date();
  const options = { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' };
  dateTimeEl.textContent = now.toLocaleDateString('zh-CN', options);
}

/** 渲染预报卡片 */
function renderForecast(forecast) {
  forecastContainer.innerHTML = '';
  forecast.forEach(item => {
    const card = document.createElement('div');
    card.className = 'forecast-card';
    card.innerHTML = `
      <div class="day">${item.day}</div>
      <div class="date">${item.date}</div>
      <div class="forecast-icon">${item.icon}</div>
      <div class="forecast-desc">${item.desc}</div>
      <div class="temp-range">
        <span class="high">${item.high}°</span>
        <span class="low">${item.low}°</span>
      </div>
    `;
    forecastContainer.appendChild(card);
  });
}

// ===== 主更新函数 =====
function updateWeather(cityName) {
  // 生成新数据
  const current = generateCurrentWeather(cityName);
  const forecast = generateForecast();

  // 渲染
  renderCurrentWeather(current);
  renderForecast(forecast);

  // 可选的额外前缀形容词
  const adj = cityAdjectives[Math.floor(Math.random() * cityAdjectives.length)];
  // 不修改城市名，只做视觉点缀
}

// ===== 事件绑定 =====

/** 搜索处理 */
function handleSearch() {
  let city = searchInput.value.trim();
  if (!city) {
    city = '北京';
    searchInput.value = city;
  }
  updateWeather(city);
}

searchBtn.addEventListener('click', handleSearch);

searchInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') {
    e.preventDefault();
    handleSearch();
  }
});

// ===== 初始化 =====
updateWeather('北京');
