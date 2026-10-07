import { api } from './api';
import type {
  Product,
  ProductSummary,
  Category,
  Cart,
  Order,
  OrderStatus,
  Address,
  User,
  AuthTokens,
  CheckoutRequest,
  CheckoutSession,
  ProductFilters,
  PaginatedResponse,
} from '../types/domain';

export const productApi = {
  getProducts: (filters: ProductFilters = {}) =>
    api.get<PaginatedResponse<ProductSummary>>('/products', filters),

  getProduct: (id: string) =>
    api.get<Product>(`/products/${id}`),

  getFeaturedProducts: (limit = 8) =>
    api.get<ProductSummary[]>('/products/featured', { limit }),

  getRelatedProducts: (productId: string, limit = 4) =>
    api.get<ProductSummary[]>(`/products/${productId}/related`, { limit }),

  searchProducts: (query: string, filters: Omit<ProductFilters, 'search'> = {}) =>
    api.get<PaginatedResponse<ProductSummary>>('/products/search', { ...filters, search: query }),
};

export const categoryApi = {
  getCategories: () =>
    api.get<Category[]>('/categories'),

  getCategoryTree: () =>
    api.get<Category[]>('/categories/tree'),

  getCategory: (id: string) =>
    api.get<Category>(`/categories/${id}`),

  getCategoryProducts: (categoryId: string, filters: ProductFilters = {}) =>
    api.get<PaginatedResponse<ProductSummary>>(`/categories/${categoryId}/products`, filters),
};

export const cartApi = {
  getCart: () =>
    api.get<Cart>('/cart'),

  addItem: (productId: string, quantity: number, attributes?: Record<string, string>) =>
    api.post<Cart>('/cart/items', { productId, quantity, attributes }),

  updateItem: (itemId: string, quantity: number) =>
    api.patch<Cart>(`/cart/items/${itemId}`, { quantity }),

  removeItem: (itemId: string) =>
    api.delete<Cart>(`/cart/items/${itemId}`),

  clearCart: () =>
    api.delete<Cart>('/cart'),

  applyPromoCode: (code: string) =>
    api.post<Cart>('/cart/promo', { code }),

  removePromoCode: () =>
    api.delete<Cart>('/cart/promo'),
};

export const orderApi = {
  getOrders: (page = 0, size = 10, status?: OrderStatus) =>
    api.get<PaginatedResponse<Order>>('/orders', { page, size, status }),

  getOrder: (id: string) =>
    api.get<Order>(`/orders/${id}`),

  createOrder: (checkoutData: CheckoutRequest) =>
    api.post<CheckoutSession>('/orders', checkoutData),

  cancelOrder: (id: string) =>
    api.post<Order>(`/orders/${id}/cancel`),

  returnOrder: (id: string, items: string[], reason: string) =>
    api.post<Order>(`/orders/${id}/return`, { items, reason }),

  getOrderTracking: (id: string) =>
    api.get<{ trackingNumber: string; carrier: string; events: Array<{ date: string; status: string; location: string }> }>(`/orders/${id}/tracking`),
};

export const authApi = {
  login: (email: string, password: string) =>
    api.post<AuthTokens>('/auth/login', { email, password }),

  register: (data: { email: string; password: string; firstName: string; lastName: string; phone?: string }) =>
    api.post<User>('/auth/register', data),

  logout: () =>
    api.post('/auth/logout'),

  getProfile: () =>
    api.get<User>('/auth/profile'),

  updateProfile: (data: Partial<User>) =>
    api.patch<User>('/auth/profile', data),

  changePassword: (currentPassword: string, newPassword: string) =>
    api.post('/auth/change-password', { currentPassword, newPassword }),

  requestPasswordReset: (email: string) =>
    api.post('/auth/forgot-password', { email }),

  resetPassword: (token: string, password: string) =>
    api.post('/auth/reset-password', { token, password }),

  verifyEmail: (token: string) =>
    api.post('/auth/verify-email', { token }),

  resendVerificationEmail: () =>
    api.post('/auth/resend-verification'),
};

export const addressApi = {
  getAddresses: () =>
    api.get<Address[]>('/addresses'),

  getAddress: (id: string) =>
    api.get<Address>(`/addresses/${id}`),

  createAddress: (address: Omit<Address, 'id'>) =>
    api.post<Address>('/addresses', address),

  updateAddress: (id: string, address: Partial<Address>) =>
    api.patch<Address>(`/addresses/${id}`, address),

  deleteAddress: (id: string) =>
    api.delete(`/addresses/${id}`),

  setDefaultAddress: (id: string, type: 'SHIPPING' | 'BILLING') =>
    api.post(`/addresses/${id}/default`, { type }),
};

export const paymentApi = {
  createPaymentIntent: (amount: number, currency: string, orderId: string) =>
    api.post<{ clientSecret: string; paymentIntentId: string }>('/payments/intent', { amount, currency, orderId }),

  confirmPayment: (paymentIntentId: string, paymentMethodId: string) =>
    api.post<{ status: string }>(`/payments/${paymentIntentId}/confirm`, { paymentMethodId }),

  getPaymentMethods: () =>
    api.get<Array<{ id: string; type: string; brand?: string; last4?: string; expiryMonth?: number; expiryYear?: number }>>('/payments/methods'),

  addPaymentMethod: (paymentMethodId: string) =>
    api.post('/payments/methods', { paymentMethodId }),

  removePaymentMethod: (id: string) =>
    api.delete(`/payments/methods/${id}`),
};