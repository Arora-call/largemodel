<template>
  <div class="calculator">
    <div class="display">
      <div class="expression">{{ expression || '0' }}</div>
      <div class="result">{{ result !== null ? result : '' }}</div>
    </div>
    <div class="buttons">
      <button class="btn btn-clear" @click="clear">C</button>
      <button class="btn btn-backspace" @click="backspace">⌫</button>
      <button class="btn btn-operator" @click="appendOperator('%')">%</button>
      <button class="btn btn-operator" @click="appendOperator('/')">÷</button>

      <button class="btn btn-number" @click="appendNumber('7')">7</button>
      <button class="btn btn-number" @click="appendNumber('8')">8</button>
      <button class="btn btn-number" @click="appendNumber('9')">9</button>
      <button class="btn btn-operator" @click="appendOperator('*')">×</button>

      <button class="btn btn-number" @click="appendNumber('4')">4</button>
      <button class="btn btn-number" @click="appendNumber('5')">5</button>
      <button class="btn btn-number" @click="appendNumber('6')">6</button>
      <button class="btn btn-operator" @click="appendOperator('-')">−</button>

      <button class="btn btn-number" @click="appendNumber('1')">1</button>
      <button class="btn btn-number" @click="appendNumber('2')">2</button>
      <button class="btn btn-number" @click="appendNumber('3')">3</button>
      <button class="btn btn-operator" @click="appendOperator('+')">+</button>

      <button class="btn btn-number btn-zero" @click="appendNumber('0')">0</button>
      <button class="btn btn-number" @click="appendDecimal">.</button>
      <button class="btn btn-equal" @click="calculate">=</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { evaluate } from 'mathjs'

const expression = ref('')
const result = ref(null)

// 追加数字
function appendNumber(num) {
  // 如果已经有结果，开始新计算
  if (result.value !== null) {
    expression.value = num
    result.value = null
  } else {
    expression.value += num
  }
}

// 追加小数点
function appendDecimal() {
  // 如果已经有结果，开始新计算
  if (result.value !== null) {
    expression.value = '0.'
    result.value = null
    return
  }
  // 获取当前最后一个数字部分
  const parts = expression.value.split(/[\+\-\*\/%]/)
  const lastPart = parts[parts.length - 1]
  if (!lastPart.includes('.')) {
    expression.value += '.'
  }
}

// 追加运算符
function appendOperator(op) {
  // 如果表达式为空，不允许运算符开头（除了负号，但这里简化）
  if (expression.value === '' && op !== '-') return
  // 如果最后一个字符是运算符，替换
  const lastChar = expression.value.slice(-1)
  if (['+', '-', '*', '/', '%'].includes(lastChar)) {
    expression.value = expression.value.slice(0, -1) + op
  } else {
    expression.value += op
  }
  // 清空结果
  result.value = null
}

// 清空
function clear() {
  expression.value = ''
  result.value = null
}

// 退格
function backspace() {
  expression.value = expression.value.slice(0, -1)
  if (expression.value === '') {
    result.value = null
  }
}

// 计算
function calculate() {
  if (expression.value === '') return
  try {
    // 替换显示字符为计算字符
    let expr = expression.value
      .replace(/×/g, '*')
      .replace(/÷/g, '/')
      .replace(/−/g, '-')
    const rawResult = evaluate(expr)
    result.value = rawResult
    // 将表达式结果设置为新的起始表达式，以便连续计算
    expression.value = rawResult.toString()
  } catch (error) {
    result.value = 'Error'
  }
}
</script>

<style scoped>
.calculator {
  width: 280px;
  background-color: #333;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
}

.display {
  background-color: #222;
  color: white;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  min-height: 80px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: flex-end;
  overflow: hidden;
}

.expression {
  font-size: 1.5rem;
  line-height: 1.2;
  word-break: break-all;
  text-align: right;
  width: 100%;
  color: #ccc;
}

.result {
  font-size: 2.2rem;
  font-weight: bold;
  line-height: 1.2;
  word-break: break-all;
  text-align: right;
  width: 100%;
  color: white;
  margin-top: 4px;
}

.buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.btn {
  padding: 16px 0;
  font-size: 1.3rem;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.1s;
  text-align: center;
  user-select: none;
}

.btn:hover {
  opacity: 0.9;
}

.btn:active {
  opacity: 0.7;
}

.btn-number {
  background-color: #4d4d4d;
  color: white;
}

.btn-operator {
  background-color: #ff9f0a;
  color: white;
}

.btn-clear, .btn-backspace {
  background-color: #a5a5a5;
  color: black;
}

.btn-equal {
  background-color: #ff9f0a;
  color: white;
  grid-column: span 1;
}

.btn-zero {
  grid-column: span 2;
}
</style>