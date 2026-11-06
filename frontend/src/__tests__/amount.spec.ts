import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

import { estimate, isPercent, isPrice, isQuantity, shiftDecimal, withCommas } from '../amount'

// 與後端 FavoriteServiceTests 讀同一份案例，前端的預估和後端實際存的金額必須一致
const csv = readFileSync(
  new URL('../../../backend/src/test/resources/fee-cases.csv', import.meta.url),
  'utf8',
)
const feeCases = csv
  .split(/\r?\n/)
  .filter((line) => line && !line.startsWith('#'))
  .slice(1)
  .map((line) => {
    const [price = '', feeRate = '', quantity = '', fee = '', total = '', description = ''] =
      line.split(',')
    return { price, feeRate, quantity: Number(quantity), fee, total, description }
  })

describe('estimate', () => {
  it('讀得到共用案例', () => {
    expect(feeCases.length).toBeGreaterThan(0)
  })

  it.each(feeCases)('$description', ({ price, feeRate, quantity, fee, total }) => {
    expect(estimate(price, feeRate, quantity)).toEqual({ fee, total })
  })

  it.each([
    ['價格是 0', '0', '0.001425', 1],
    ['價格超過兩位小數', '1.001', '0.001425', 1],
    ['費率大於 1', '10.00', '1.5', 1],
    ['數量是 0', '10.00', '0.001425', 0],
    ['數量超過上限', '10.00', '0.001425', 1_000_001],
  ])('%s時不顯示預估', (_, price, rate, quantity) => {
    expect(estimate(price, rate, quantity)).toBeNull()
  })
})

describe('輸入檢查', () => {
  it('價格要大於 0，最多兩位小數', () => {
    expect(isPrice('0.01')).toBe(true)
    expect(isPrice('9999999999.99')).toBe(true)
    expect(isPrice('0')).toBe(false)
    expect(isPrice('10000000000')).toBe(false)
    expect(isPrice('-1')).toBe(false)
    expect(isPrice('1e3')).toBe(false)
  })

  it('費率百分比介於 0 到 100，最多六位小數', () => {
    expect(isPercent('0')).toBe(true)
    expect(isPercent('0.1425')).toBe(true)
    expect(isPercent('100')).toBe(true)
    expect(isPercent('100.5')).toBe(false)
    expect(isPercent('0.1234567')).toBe(false)
  })

  it('數量是 1 到 1,000,000 的整數', () => {
    expect(isQuantity('1')).toBe(true)
    expect(isQuantity('1000000')).toBe(true)
    expect(isQuantity('0')).toBe(false)
    expect(isQuantity('1000001')).toBe(false)
    expect(isQuantity('1.5')).toBe(false)
  })
})

describe('shiftDecimal', () => {
  // 畫面上的費率是百分比，送給後端前要往左移兩位
  it.each([
    ['0.1425', -2, '0.001425'],
    ['100', -2, '1'],
    ['0', -2, '0'],
    ['0.001425', 2, '0.1425'],
    ['1', 2, '100'],
  ])('%s 移 %i 位是 %s', (text, places, expected) => {
    expect(shiftDecimal(text, places)).toBe(expected)
  })
})

describe('withCommas', () => {
  it.each([
    ['0.00', '0.00'],
    ['999', '999'],
    ['1000', '1,000'],
    ['45664.98', '45,664.98'],
    ['19999999999980000.00', '19,999,999,999,980,000.00'],
  ])('%s 顯示成 %s', (text, expected) => {
    expect(withCommas(text)).toBe(expected)
  })
})
