import Big from 'big.js'

// 金額用十進位小數（big.js）計算，不用浮點數。規則與後端相同：手續費 = 價格 × 數量 × 費率，四捨五入到分

const MAX_QUANTITY = 1_000_000
const PRICE = /^\d{1,10}(\.\d{1,2})?$/
const RATE = /^(0(\.\d{1,8})?|1(\.0{1,8})?)$/
const PERCENT = /^(\d{1,2}(\.\d{1,6})?|100(\.0{1,6})?)$/

export function isPrice(text: string): boolean {
  return PRICE.test(text) && Number(text) > 0
}

export function isPercent(text: string): boolean {
  return PERCENT.test(text)
}

export function isQuantity(text: string): boolean {
  return /^\d{1,7}$/.test(text) && Number(text) >= 1 && Number(text) <= MAX_QUANTITY
}

/** 把十進位字串的小數點往右（正）或往左（負）移 */
export function shiftDecimal(text: string, places: number): string {
  return new Big(text).times(new Big(10).pow(places)).toFixed()
}

/** 輸入不合法就回 null，不顯示預估 */
export function estimate(
  price: string,
  rate: string,
  quantity: number,
): { fee: string; total: string } | null {
  if (!isPrice(price) || !RATE.test(rate) || !isQuantity(String(quantity))) {
    return null
  }
  const principal = new Big(price).times(quantity)
  const fee = principal.times(rate).round(2, Big.roundHalfUp)
  return { fee: fee.toFixed(2), total: principal.plus(fee).toFixed(2) }
}

/** "1234567.8" → "1,234,567.8" */
export function withCommas(text: string): string {
  const [whole = '', fraction] = text.split('.')
  const grouped = whole.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return fraction === undefined ? grouped : `${grouped}.${fraction}`
}
