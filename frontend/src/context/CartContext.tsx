import { createContext, useContext, useReducer, useEffect, useCallback, ReactNode } from 'react';
import { cartApi } from '@services/endpoints';
import { useAuth } from './AuthContext';
import type { Cart, CartItem } from '../types/domain';
import { STORAGE_KEYS } from '@utils/constants';

interface CartState {
  cart: Cart | null;
  isLoading: boolean;
  isSyncing: boolean;
  error: string | null;
}

type CartAction =
  | { type: 'SET_CART'; payload: Cart }
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_SYNCING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'OPTIMISTIC_ADD'; payload: CartItem }
  | { type: 'OPTIMISTIC_UPDATE'; payload: { itemId: string; quantity: number } }
  | { type: 'OPTIMISTIC_REMOVE'; payload: string }
  | { type: 'OPTIMISTIC_CLEAR' }
  | { type: 'RESET' };

const initialState: CartState = {
  cart: null,
  isLoading: false,
  isSyncing: false,
  error: null,
};

function cartReducer(state: CartState, action: CartAction): CartState {
  switch (action.type) {
    case 'SET_CART':
      return { ...state, cart: action.payload, error: null };
    case 'SET_LOADING':
      return { ...state, isLoading: action.payload };
    case 'SET_SYNCING':
      return { ...state, isSyncing: action.payload };
    case 'SET_ERROR':
      return { ...state, error: action.payload };
    case 'OPTIMISTIC_ADD': {
      if (!state.cart) return state;
      const existingItem = state.cart.items.find((item) => item.productId === action.payload.productId);
      let newItems: CartItem[];
      if (existingItem) {
        newItems = state.cart.items.map((item) =>
          item.id === existingItem.id ? { ...item, quantity: item.quantity + action.payload.quantity, subtotal: (item.quantity + action.payload.quantity) * item.price } : item
        );
      } else {
        newItems = [...state.cart.items, { ...action.payload, id: action.payload.id || `temp-${Date.now()}` }];
      }
      const subtotal = newItems.reduce((sum, item) => sum + item.subtotal, 0);
      return {
        ...state,
        cart: { ...state.cart, items: newItems, subtotal, total: subtotal, itemCount: newItems.reduce((sum, item) => sum + item.quantity, 0) },
      };
    }
    case 'OPTIMISTIC_UPDATE': {
      if (!state.cart) return state;
      const newItems = state.cart.items.map((item) =>
        item.id === action.payload.itemId ? { ...item, quantity: action.payload.quantity, subtotal: action.payload.quantity * item.price } : item
      );
      const subtotal = newItems.reduce((sum, item) => sum + item.subtotal, 0);
      return {
        ...state,
        cart: { ...state.cart, items: newItems, subtotal, total: subtotal, itemCount: newItems.reduce((sum, item) => sum + item.quantity, 0) },
      };
    }
    case 'OPTIMISTIC_REMOVE': {
      if (!state.cart) return state;
      const newItems = state.cart.items.filter((item) => item.id !== action.payload);
      const subtotal = newItems.reduce((sum, item) => sum + item.subtotal, 0);
      return {
        ...state,
        cart: { ...state.cart, items: newItems, subtotal, total: subtotal, itemCount: newItems.reduce((sum, item) => sum + item.quantity, 0) },
      };
    }
    case 'OPTIMISTIC_CLEAR':
      return { ...state, cart: state.cart ? { ...state.cart, items: [], subtotal: 0, total: 0, itemCount: 0 } : null };
    case 'RESET':
      return initialState;
    default:
      return state;
  }
}

interface CartContextType extends CartState {
  fetchCart: () => Promise<void>;
  addItem: (productId: string, quantity: number, attributes?: Record<string, string>) => Promise<void>;
  updateItem: (itemId: string, quantity: number) => Promise<void>;
  removeItem: (itemId: string) => Promise<void>;
  clearCart: () => Promise<void>;
  applyPromoCode: (code: string) => Promise<void>;
  removePromoCode: () => Promise<void>;
  syncGuestCart: () => Promise<void>;
}

export const CartContext = createContext<CartContextType | undefined>(undefined);

export function CartProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(cartReducer, initialState);
  const { isAuthenticated } = useAuth();

  const loadGuestCart = useCallback((): Cart | null => {
    try {
      const stored = localStorage.getItem(STORAGE_KEYS.CART);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  }, []);

  const saveGuestCart = useCallback((cart: Cart) => {
    localStorage.setItem(STORAGE_KEYS.CART, JSON.stringify(cart));
  }, []);

  const fetchCart = useCallback(async () => {
    if (!isAuthenticated) {
      const guestCart = loadGuestCart();
      dispatch({ type: 'SET_CART', payload: guestCart || { id: 'guest', items: [], subtotal: 0, total: 0, currency: 'USD', itemCount: 0, updatedAt: new Date().toISOString() } });
      return;
    }

    dispatch({ type: 'SET_LOADING', payload: true });
    try {
      const cart = await cartApi.getCart();
      dispatch({ type: 'SET_CART', payload: cart });
    } catch {
      dispatch({ type: 'SET_ERROR', payload: 'Failed to load cart' });
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false });
    }
  }, [isAuthenticated, loadGuestCart]);

  const syncGuestCart = useCallback(async () => {
    if (!isAuthenticated) return;
    
    const guestCart = loadGuestCart();
    if (!guestCart || guestCart.items.length === 0) return;

    dispatch({ type: 'SET_SYNCING', payload: true });
    try {
      for (const item of guestCart.items) {
        await cartApi.addItem(item.productId, item.quantity, item.attributes);
      }
      localStorage.removeItem(STORAGE_KEYS.CART);
      await fetchCart();
    } catch {
      dispatch({ type: 'SET_ERROR', payload: 'Failed to sync cart' });
    } finally {
      dispatch({ type: 'SET_SYNCING', payload: false });
    }
  }, [isAuthenticated, loadGuestCart, fetchCart]);

  useEffect(() => {
    fetchCart();
  }, [fetchCart]);

  useEffect(() => {
    if (isAuthenticated) {
      syncGuestCart();
    }
  }, [isAuthenticated, syncGuestCart]);

  const addItem = async (productId: string, quantity: number, attributes?: Record<string, string>) => {
    if (!isAuthenticated) {
      // Optimistic update for guest
      const newItem: CartItem = {
        id: `temp-${Date.now()}`,
        productId,
        productName: '', // Will be filled by UI
        productThumbnail: '',
        price: 0,
        currency: 'USD',
        quantity,
        subtotal: 0,
        attributes: attributes || {},
        maxQuantity: 99,
      };
      dispatch({ type: 'OPTIMISTIC_ADD', payload: newItem });
      
      // Persist to localStorage
      if (state.cart) {
        const updatedCart = { ...state.cart, items: [...state.cart.items, newItem] };
        saveGuestCart(updatedCart);
      }
      return;
    }

    dispatch({ type: 'SET_SYNCING', payload: true });
    try {
      const cart = await cartApi.addItem(productId, quantity, attributes);
      dispatch({ type: 'SET_CART', payload: cart });
    } catch {
      dispatch({ type: 'SET_ERROR', payload: 'Failed to add item to cart' });
      throw new Error('Failed to add item to cart');
    } finally {
      dispatch({ type: 'SET_SYNCING', payload: false });
    }
  };

  const updateItem = async (itemId: string, quantity: number) => {
    if (quantity < 1) {
      await removeItem(itemId);
      return;
    }

    if (!isAuthenticated) {
      dispatch({ type: 'OPTIMISTIC_UPDATE', payload: { itemId, quantity } });
      if (state.cart) {
        saveGuestCart(state.cart);
      }
      return;
    }

    dispatch({ type: 'SET_SYNCING', payload: true });
    try {
      const cart = await cartApi.updateItem(itemId, quantity);
      dispatch({ type: 'SET_CART', payload: cart });
    } catch {
      dispatch({ type: 'SET_ERROR', payload: 'Failed to update cart' });
      throw new Error('Failed to update cart');
    } finally {
      dispatch({ type: 'SET_SYNCING', payload: false });
    }
  };

  const removeItem = async (itemId: string) => {
    if (!isAuthenticated) {
      dispatch({ type: 'OPTIMISTIC_REMOVE', payload: itemId });
      if (state.cart) {
        saveGuestCart(state.cart);
      }
      return;
    }

    dispatch({ type: 'SET_SYNCING', payload: true });
    try {
      const cart = await cartApi.removeItem(itemId);
      dispatch({ type: 'SET_CART', payload: cart });
    } catch {
      dispatch({ type: 'SET_ERROR', payload: 'Failed to remove item' });
      throw new Error('Failed to remove item');
    } finally {
      dispatch({ type: 'SET_SYNCING', payload: false });
    }
  };

  const clearCart = async () => {
    if (!isAuthenticated) {
      dispatch({ type: 'OPTIMISTIC_CLEAR' });
      localStorage.removeItem(STORAGE_KEYS.CART);
      return;
    }

    dispatch({ type: 'SET_SYNCING', payload: true });
    try {
      const cart = await cartApi.clearCart();
      dispatch({ type: 'SET_CART', payload: cart });
    } catch {
      dispatch({ type: 'SET_ERROR', payload: 'Failed to clear cart' });
      throw new Error('Failed to clear cart');
    } finally {
      dispatch({ type: 'SET_SYNCING', payload: false });
    }
  };

  const applyPromoCode = async (code: string) => {
    if (!isAuthenticated) return;
    
    dispatch({ type: 'SET_SYNCING', payload: true });
    try {
      const cart = await cartApi.applyPromoCode(code);
      dispatch({ type: 'SET_CART', payload: cart });
    } catch {
      dispatch({ type: 'SET_ERROR', payload: 'Invalid promo code' });
      throw new Error('Invalid promo code');
    } finally {
      dispatch({ type: 'SET_SYNCING', payload: false });
    }
  };

  const removePromoCode = async () => {
    if (!isAuthenticated) return;
    
    dispatch({ type: 'SET_SYNCING', payload: true });
    try {
      const cart = await cartApi.removePromoCode();
      dispatch({ type: 'SET_CART', payload: cart });
    } catch {
      dispatch({ type: 'SET_ERROR', payload: 'Failed to remove promo code' });
      throw new Error('Failed to remove promo code');
    } finally {
      dispatch({ type: 'SET_SYNCING', payload: false });
    }
  };

  return (
    <CartContext.Provider
      value={{
        ...state,
        fetchCart,
        addItem,
        updateItem,
        removeItem,
        clearCart,
        applyPromoCode,
        removePromoCode,
        syncGuestCart,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
}