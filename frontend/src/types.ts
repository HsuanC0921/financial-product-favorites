export interface Account {
  account: string
  accountName: string
}

export interface Me {
  userId: string
  userName: string
  email: string
  defaultAccount: string
  accounts: Account[]
}

export interface Favorite {
  sn: number
  productName: string
  price: string
  feeRate: string
  purchaseQuantity: number
  account: string
  totalFee: string
  totalAmount: string
  note: string
  createdAt: string
  version: number
}

export interface FavoritePage {
  user: Pick<Me, 'userId' | 'userName' | 'email'>
  items: Favorite[]
  nextAfterSn: number | null
}

export interface FavoriteInput {
  productName: string
  price: string
  feeRate: string
  purchaseQuantity: number
  account: string
  note: string
}
