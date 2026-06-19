<template>
  <div class="calculator">
    <div class="display">
      <div class="expression">{{ expression }}</div>
      <div class="result" :class="{ 'result-small': current.length > 12 }">{{ displayValue }}</div>
    </div>
    <div class="buttons">
      <button class="btn btn-function" @click="clear">C</button>
      <button class="btn btn-function" @click="toggleSign">±</button>
      <button class="btn btn-function" @click="percent">%</button>
      <button class="btn btn-operator" @click="operator('/')">÷</button>

      <button class="btn btn-number" @click="append('7')">7</button>
      <button class="btn btn-number" @click="append('8')">8</button>
      <button class="btn btn-number" @click="append('9')">9</button>
      <button class="btn btn-operator" @click="operator('*')">×</button>

      <button class="btn btn-number" @click="append('4')">4</button>
      <button class="btn btn-number" @click="append('5')">5</button>
      <button class="btn btn-number" @click="append('6')">6</button>
      <button class="btn btn-operator" @click="operator('-')">−</button>

      <button class="btn btn-number" @click="append('1')">1</button>
      <button class="btn btn-number" @click="append('2')">2</button>
      <button class="btn btn-number" @click="append('3')">3</button>
      <button class="btn btn-operator" @click="operator('+')">+</button>

      <button class="btn btn-number btn-zero" @click="append('0')">0</button>
      <button class="btn btn-number" @click="append('.')">.</button>
      <button class="btn btn-equal" @click="calculate">=</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const current = ref('0')
const previous = ref('')
const operation = ref(null)
const resetNext = ref(false)
const expression = ref('')

const displayValue = computed(() => {
  const num = parseFloat(current.value)
  if (isNaN(num)) return '0'
  if (Number.isInteger(num)) return num.toString()
  return current.value
})

function append(num) {
  if (resetNext.value) {
    current.value = ''
    resetNext.value = false
  }
  if (num === '.' && current.value.includes('.')) return
  if (current.value === '0' && num !== '.') {
    current.value = num
  } else {
    current.value += num
  }
}

function operator(op) {
  const cur = parseFloat(current.value)
  if (isNaN(cur)) return
  if (operation.value && !resetNext.value) {
    calculate()
  }
  previous.value = current.value
  operation.value = op
  expression.value = `${previous.value} ${getOpSymbol(op)} `
  resetNext.value = true
}

function getOpSymbol(op) {
  const map = { '+': '+', '-': '−', '*': '×', '/': '÷' }
  return map[op] || op
}

function calculate() {
  const prev = parseFloat(previous.value)
  const cur = parseFloat(current.value)
  if (isNaN(prev) || isNaN(cur)) return
  let result
  switch (operation.value) {
    case '+': result = prev + cur; break
    case '-': result = prev - cur; break
    case '*': result = prev * cur; break
    case '/':
      if (cur === 0) {
        current.value = 'Error'
        operation.value = null
        previous.value = ''
        expression.value = ''
        resetNext.value = true
        return
      }
      result = prev / cur
      break
    default: return
  }
  expression.value = `${previous.value} ${getOpSymbol(operation.value)} ${current.value} =`
  current.value = parseFloat(result.toFixed(10)).toString()
  operation.value = null
  previous.value = ''
  resetNext.value = true
}

function clear() {
  current.value = '0'
  previous.value = ''
  operation.value = null
  resetNext.value = false
  expression.value = ''
}

function toggleSign() {
  if (current.value === '0') return
  if (current.value.startsWith('-')) {
    current.value = current.value.slice(1)
  } else {
    current.value = '-' + current.value
  }
}

function percent() {
  const num = parseFloat(current.value)
  if (isNaN(num)) return
  current.value = (num / 100).toString()
}
</script>

<style scoped>
.calculator {
  background: #1c1c2e;
  border-radius: 20px;
  padding: 25px;
  width: 320px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.6), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.display {
  background: #0f0f1a;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 20px;
  text-align: right;
  min-height: 80px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  word-break: break-all;
}

.expression {
  color: #8888aa;
  font-size: 14px;
  min-height: 20px;
  margin-bottom: 6px;
  letter-spacing: 0.5px;
}

.result {
  color: #f0f0ff;
  font-size: 40px;
  font-weight: 300;
  line-height: 1.1;
  transition: font-size 0.15s ease;
}

.result-small {
  font-size: 28px;
}

.buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.btn {
  border: none;
  border-radius: 12px;
  padding: 18px 0;
  font-size: 22px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  user-select: none;
  outline: none;
  box-shadow: 0 4px 0 rgba(0, 0, 0, 0.3);
}

.btn:active {
  transform: translateY(2px);
  box-shadow: 0 2px 0 rgba(0, 0, 0, 0.3);
}

.btn-number {
  background: #2d2d44;
  color: #e8e8ff;
}

.btn-number:hover {
  background: #3a3a55;
}

.btn-function {
  background: #3a3a50;
  color: #aab;
}

.btn-function:hover {
  background: #4a4a62;
}

.btn-operator {
  background: #3a2a3e;
  color: #ff9f4a;
}

.btn-operator:hover {
  background: #4d3555;
}

.btn-equal {
  background: #f09a36;
  color: #1c1c2e;
  font-weight: 600;
  box-shadow: 0 4px 0 #b87820;
}

.btn-equal:hover {
  background: #ffaa44;
}

.btn-equal:active {
  transform: translateY(2px);
  box-shadow: 0 2px 0 #b87820;
}

.btn-zero {
  grid-column: span 2;
}
</style>