// 壓力測試：登入後混合讀取與寫入，用 k6 對 Nginx 的 HTTPS 入口施壓
// 執行方式見 docs/TESTING.md 的「壓力測試」
import http from 'k6/http'
import { check, sleep } from 'k6'

const BASE = __ENV.BASE_URL || 'https://web'
// 每次操作之間的停頓（秒），設成 0 就是不停施壓，用來找出吞吐量的上限
const THINK_TIME = Number(__ENV.THINK_TIME ?? 0.5)

// 示範使用者 A 到 E，密碼與 README 的示範使用者相同
const USERS = ['A1236456789', 'B2345678901', 'C3456789012', 'D4567890123', 'E5678901234']
const PASSWORDS = ['DemoUserA#2026', 'DemoUserB#2026', 'DemoUserC#2026', 'DemoUserD#2026', 'DemoUserE#2026']

export const options = {
	insecureSkipTLSVerify: true, // 本機自簽憑證
	scenarios: {
		mixed: {
			executor: 'ramping-vus',
			stages: [
				{ duration: '15s', target: 10 },
				{ duration: '30s', target: 50 },
				{ duration: '30s', target: 50 },
				{ duration: '15s', target: 0 },
			],
		},
	},
	thresholds: {
		http_req_failed: ['rate<0.01'],
		http_req_duration: ['p(95)<500'],
		checks: ['rate>0.99'],
	},
}

// Nginx 限制登入每分鐘 10 次（突發 5 次），所以只登入 5 位使用者一次，所有虛擬使用者共用這幾個 Session
export function setup() {
	return USERS.map((userId, i) => {
		const jar = new http.CookieJar()
		// 未登入會回 401，同時拿到 XSRF-TOKEN Cookie
		http.get(`${BASE}/api/me`, { jar, responseCallback: http.expectedStatuses(401) })
		const token = jar.cookiesForURL(`${BASE}/`)['XSRF-TOKEN'][0]
		const login = http.post(`${BASE}/api/auth/login`, { userId, password: PASSWORDS[i] }, {
			jar,
			headers: { 'X-XSRF-TOKEN': token },
		})
		if (login.status !== 204) throw new Error(`登入失敗 ${userId}：${login.status}`)
		const me = http.get(`${BASE}/api/me`, { jar }) // 登入後 XSRF-TOKEN 會換新
		const cookies = jar.cookiesForURL(`${BASE}/`)
		return {
			headers: {
				Cookie: `JSESSIONID=${cookies['JSESSIONID'][0]}; XSRF-TOKEN=${cookies['XSRF-TOKEN'][0]}`,
				'X-XSRF-TOKEN': cookies['XSRF-TOKEN'][0],
			},
			account: me.json('defaultAccount'),
		}
	})
}

function body(account, quantity) {
	return JSON.stringify({
		productName: `壓測商品 ${__VU}-${__ITER}`,
		price: '45.60',
		feeRate: '0.001425',
		purchaseQuantity: quantity,
		account,
		note: '壓力測試',
	})
}

export default function (sessions) {
	const { headers, account } = sessions[__VU % sessions.length]
	const json = { ...headers, 'Content-Type': 'application/json' }
	const roll = Math.random()

	if (roll < 0.7) {
		const list = http.get(`${BASE}/api/favorites`, { headers })
		check(list, { '清單 200': (r) => r.status === 200 })
	} else if (roll < 0.8) {
		const me = http.get(`${BASE}/api/me`, { headers })
		check(me, { '使用者資料 200': (r) => r.status === 200 })
	} else {
		// 新增、修改、刪除一輪，只動自己剛建立的資料，結束時不留紀錄
		const created = http.post(`${BASE}/api/favorites`, body(account, 100), { headers: json })
		if (!check(created, { '新增 201': (r) => r.status === 201 })) return
		const sn = created.json('sn')
		const updated = http.put(`${BASE}/api/favorites/${sn}`, body(account, 200), {
			headers: { ...json, 'If-Match': created.headers['Etag'] },
		})
		check(updated, { '修改 200': (r) => r.status === 200 })
		const removed = http.del(`${BASE}/api/favorites/${sn}`, null, {
			headers: { ...headers, 'If-Match': updated.headers['Etag'] },
		})
		check(removed, { '刪除 204': (r) => r.status === 204 })
	}
	sleep(THINK_TIME)
}
