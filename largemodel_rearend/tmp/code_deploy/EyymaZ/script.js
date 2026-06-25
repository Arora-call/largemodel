// ===== 城市天气数据集 =====
const cityData = {
    '北京': {
        temp: 25,
        desc: '晴',
        icon: '☀️',
        feelsLike: 23,
        humidity: 42,
        windSpeed: 3.2,
        pressure: 1013,
        forecast: [
            { day: '明天', icon: '☀️', temp: '17~28°C', desc: '晴' },
            { day: '后天', icon: '⛅', temp: '16~26°C', desc: '多云' },
            { day: '大后天', icon: '☁️', temp: '14~24°C', desc: '阴' }
        ]
    },
    '上海': {
        temp: 27,
        desc: '多云',
        icon: '⛅',
        feelsLike: 29,
        humidity: 68,
        windSpeed: 2.1,
        pressure: 1008,
        forecast: [
            { day: '明天', icon: '🌧️', temp: '22~26°C', desc: '阵雨' },
            { day: '后天', icon: '⛅', temp: '21~27°C', desc: '多云' },
            { day: '大后天', icon: '☀️', temp: '20~28°C', desc: '晴' }
        ]
    },
    '广州': {
        temp: 31,
        desc: '雷阵雨',
        icon: '⛈️',
        feelsLike: 35,
        humidity: 78,
        windSpeed: 2.8,
        pressure: 1005,
        forecast: [
            { day: '明天', icon: '⛈️', temp: '25~30°C', desc: '雷阵雨' },
            { day: '后天', icon: '🌧️', temp: '24~29°C', desc: '阵雨' },
            { day: '大后天', icon: '⛅', temp: '24~31°C', desc: '多云' }
        ]
    },
    '深圳': {
        temp: 30,
        desc: '阵雨',
        icon: '🌧️',
        feelsLike: 33,
        humidity: 74,
        windSpeed: 3.5,
        pressure: 1006,
        forecast: [
            { day: '明天', icon: '🌧️', temp: '24~29°C', desc: '阵雨' },
            { day: '后天', icon: '⛅', temp: '23~30°C', desc: '多云' },
            { day: '大后天', icon: '☀️', temp: '23~31°C', desc: '晴' }
        ]
    },
    '杭州': {
        temp: 26,
        desc: '阴',
        icon: '☁️',
        feelsLike: 27,
        humidity: 62,
        windSpeed: 1.8,
        pressure: 1010,
        forecast: [
            { day: '明天', icon: '🌧️', temp: '20~24°C', desc: '小雨' },
            { day: '后天', icon: '☁️', temp: '19~25°C', desc: '阴' },
            { day: '大后天', icon: '⛅', temp: '18~26°C', desc: '多云' }
        ]
    },
    '成都': {
        temp: 24,
        desc: '多云',
        icon: '⛅',
        feelsLike: 25,
        humidity: 58,
        windSpeed: 1.5,
        pressure: 1012,
        forecast: [
            { day: '明天', icon: '☁️', temp: '18~23°C', desc: '阴' },
            { day: '后天', icon: '🌧️', temp: '17~22°C', desc: '小雨' },
            { day: '大后天', icon: '⛅', temp: '16~24°C', desc: '多云' }
        ]
    },
    '武汉': {
        temp: 28,
        desc: '晴',
        icon: '☀️',
        feelsLike: 29,
        humidity: 48,
        windSpeed: 2.6,
        pressure: 1010,
        forecast: [
            { day: '明天', icon: '☀️', temp: '20~29°C', desc: '晴' },
            { day: '后天', icon: '⛅', temp: '19~27°C', desc: '多云' },
            { day: '大后天', icon: '☁️', temp: '17~25°C', desc: '阴' }
        ]
    },
    '西安': {
        temp: 23,
        desc: '晴',
        icon: '☀️',
        feelsLike: 22,
        humidity: 36,
        windSpeed: 3.8,
        pressure: 1015,
        forecast: [
            { day: '明天', icon: '☀️', temp: '14~25°C', desc: '晴' },
            { day: '后天', icon: '⛅', temp: '13~23°C', desc: '多云' },
            { day: '大后天', icon: '☁️', temp: '11~21°C', desc: '阴' }
        ]
    },
    '重庆': {
        temp: 22,
        desc: '雾',
        icon: '🌫️',
        feelsLike: 23,
        humidity: 72,
        windSpeed: 1.2,
        pressure: 1011,
        forecast: [
            { day: '明天', icon: '🌫️', temp: '18~21°C', desc: '雾' },
            { day: '后天', icon: '☁️', temp: '17~23°C', desc: '阴' },
            { day: '大后天', icon: '⛅', temp: '16~24°C', desc: '多云' }
        ]
    },
    '南京': {
        temp: 26,
        desc: '多云',
        icon: '⛅',
        feelsLike: 27,
        humidity: 55,
        windSpeed: 2.3,
        pressure: 1009,
        forecast: [
            { day: '明天', icon: '🌧️', temp: '19~24°C', desc: '小雨' },
            { day: '后天', icon: '☁️', temp: '18~25°C', desc: '阴' },
            { day: '大后天', icon: '☀️', temp: '17~27°C', desc: '晴' }
        ]
    }
};

// ===== DOM 元素 =====
const cityNameEl = document.getElementById('cityName');
const updateTimeEl = document.getElementById('updateTime');
const weatherIconEl = document.getElementById('weatherIcon');
const temperatureEl = document.getElementById('temperature');
const weatherDescEl = document.getElementById('weatherDesc');
const feelsLikeEl = document.getElementById('feelsLike');
const humidityEl = document.getElementById('humidity');
const windSpeedEl = document.getElementById('windSpeed');
const pressureEl = document.getElementById('pressure');
const forecastListEl = document.getElementById('forecastList');
const searchInput = document.getElementById('searchInput');
const searchBtn = document.getElementById('searchBtn');
const toastEl = document.getElementById('toast');
const weatherCard = document.getElementById('weatherCard');

// ===== 当前显示的城市 =====
let currentCity = '北京';

// ===== 时间更新 =====
function updateClock() {
    const now = new Date();
    const h = String(now.getHours()).padStart(2, '0');
    const m = String(now.getMinutes()).padStart(2, '0');
    updateTimeEl.textContent = h + ':' + m;
}
setInterval(updateClock, 1000);
updateClock();

// ===== 显示天气 =====
function renderWeather(cityName) {
    const data = cityData[cityName];
    if (!data) return;

    cityNameEl.textContent = cityName;
    weatherIconEl.textContent = data.icon;
    temperatureEl.textContent = data.temp + '°C';
    weatherDescEl.textContent = data.desc;
    feelsLikeEl.textContent = data.feelsLike + '°C';
    humidityEl.textContent = data.humidity + '%';
    windSpeedEl.textContent = data.windSpeed + ' m/s';
    pressureEl.textContent = data.pressure + ' hPa';

    // 未来预报
    forecastListEl.innerHTML = '';
    data.forecast.forEach(function (item) {
        const div = document.createElement('div');
        div.className = 'forecast-item';
        div.innerHTML =
            '<div class="forecast-day">' + item.day + '</div>' +
            '<span class="forecast-icon">' + item.icon + '</span>' +
            '<div class="forecast-temp">' + item.temp + '</div>' +
            '<div class="forecast-desc">' + item.desc + '</div>';
        forecastListEl.appendChild(div);
    });

    // 卡片动画
    weatherCard.style.opacity = '0';
    weatherCard.style.transform = 'translateY(12px)';
    setTimeout(function () {
        weatherCard.style.opacity = '1';
        weatherCard.style.transform = 'translateY(0)';
    }, 50);
}

// ===== Toast 提示 =====
var toastTimer = null;
function showToast(msg) {
    toastEl.textContent = msg;
    toastEl.classList.add('show');
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
        toastEl.classList.remove('show');
    }, 2200);
}

// ===== 搜索逻辑 =====
function searchCity(keyword) {
    var trimmed = keyword.trim();
    if (!trimmed) {
        showToast('请输入城市名称');
        return;
    }

    // 精确匹配
    if (cityData[trimmed]) {
        currentCity = trimmed;
        renderWeather(trimmed);
        showToast('已切换到 ' + trimmed);
        searchInput.value = '';
        return;
    }

    // 模糊匹配（包含关键词）
    var matched = null;
    var keys = Object.keys(cityData);
    for (var i = 0; i < keys.length; i++) {
        if (keys[i].indexOf(trimmed) !== -1) {
            matched = keys[i];
            break;
        }
    }

    if (matched) {
        currentCity = matched;
        renderWeather(matched);
        showToast('已切换到 ' + matched);
        searchInput.value = '';
    } else {
        showToast('未找到城市「' + trimmed + '」，请尝试其他名称');
    }
}

// ===== 事件绑定 =====
searchBtn.addEventListener('click', function () {
    searchCity(searchInput.value);
});

searchInput.addEventListener('keydown', function (e) {
    if (e.key === 'Enter') {
        e.preventDefault();
        searchCity(searchInput.value);
    }
});

// ===== 初始化 =====
renderWeather(currentCity);

// ===== 控制台提示 =====
console.log('🌤 天气预报已加载！支持的城市：' + Object.keys(cityData).join('、'));