import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act, fireEvent, waitFor } from '@testing-library/react';
import { CartProvider, useCart } from '@context/CartContext';
import { ThemeProvider } from '@mui/material';
import { theme } from '@styles/theme';
import { productApi, cartApi } from '@services/endpoints';
import type { Product, Cart } from '../types/domain';

/*
 * CartProvider calls useAuth(), so the auth context must be available.
 * We mock AuthContext so tests can deterministically choose between the
 * guest (unauthenticated) and authenticated code paths without waiting on
 * the real async auth initialization.
 */
let isAuthenticated = false;
vi.mock('@context/AuthContext', () => ({
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useAuth: () => ({ isAuthenticated }),
}));

const renderWithProviders = (component: React.ReactElement) =>
  render(
    <ThemeProvider theme={theme}>
      <CartProvider>{component}</CartProvider>
    </ThemeProvider>
  );

const mockProduct: Product = {
  id: 'prod-1',
  name: 'Widget',
  description: 'A widget',
  price: 29.99,
  currency: 'USD',
  images: ['https://example.com/w.png'],
  thumbnail: 'https://example.com/w-thumb.png',
  categoryId: 'cat-1',
  stockQuantity: 100,
  isActive: true,
  attributes: {},
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

const emptyGuestCart: Cart = {
  id: 'guest',
  items: [],
  subtotal: 0,
  total: 0,
  currency: 'USD',
  itemCount: 0,
  updatedAt: new Date().toISOString(),
};

describe('CartContext - guest cart prices', () => {
  beforeEach(() => {
    isAuthenticated = false;
    vi.spyOn(productApi, 'getProduct').mockResolvedValue(mockProduct);
    vi.spyOn(cartApi, 'getCart').mockResolvedValue(emptyGuestCart);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('provides an empty guest cart on load', async () => {
    const TestComponent = () => {
      const { cart } = useCart();
      return <span data-testid="item-count">{cart?.itemCount ?? 0}</span>;
    };

    renderWithProviders(<TestComponent />);

    await waitFor(() => {
      expect(screen.getByTestId('item-count')).toHaveTextContent('0');
    });
  });

  it('guest cart items record the product real price and subtotal', async () => {
    const TestComponent = () => {
      const { cart, addItem } = useCart();
      return (
        <div>
          <span data-testid="item-count">{cart?.itemCount ?? 0}</span>
          <span data-testid="unit-price">{cart?.items.find((i) => i.productId === 'prod-1')?.price ?? 0}</span>
          <span data-testid="line-subtotal">{cart?.items.find((i) => i.productId === 'prod-1')?.subtotal ?? 0}</span>
          <span data-testid="cart-total">{cart?.total ?? 0}</span>
          <button onClick={() => addItem('prod-1', 2)} data-testid="add-btn">Add</button>
        </div>
      );
    };

    renderWithProviders(<TestComponent />);

    await waitFor(() => {
      expect(screen.getByTestId('item-count')).toHaveTextContent('0');
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('add-btn'));
    });

    expect(productApi.getProduct).toHaveBeenCalledWith('prod-1');

    await waitFor(() => {
      expect(screen.getByTestId('item-count')).toHaveTextContent('2');
    });
    await waitFor(() => {
      expect(screen.getByTestId('unit-price')).toHaveTextContent('29.99');
    });
    await waitFor(() => {
      expect(screen.getByTestId('line-subtotal')).toHaveTextContent('59.98');
    });
    await waitFor(() => {
      expect(screen.getByTestId('cart-total')).toHaveTextContent('59.98');
    });
  });

  it('cart summary shows non-zero subtotal and total after adding a guest item', async () => {
    const TestComponent = () => {
      const { cart, addItem } = useCart();
      return (
        <div>
          <span data-testid="cart-subtotal">{cart?.subtotal ?? 0}</span>
          <span data-testid="cart-total">{cart?.total ?? 0}</span>
          <button onClick={() => addItem('prod-1', 1)} data-testid="add-btn">Add</button>
        </div>
      );
    };

    renderWithProviders(<TestComponent />);

    await waitFor(() => {
      expect(screen.getByTestId('cart-total')).toHaveTextContent('0');
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('add-btn'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('cart-subtotal')).toHaveTextContent('29.99');
    });
    await waitFor(() => {
      expect(screen.getByTestId('cart-total')).toHaveTextContent('29.99');
    });
  });

  it('recomputes line subtotal and cart total when updating a guest item quantity', async () => {
    const TestComponent = () => {
      const { cart, addItem, updateItem } = useCart();
      const item = cart?.items.find((i) => i.productId === 'prod-1');
      return (
        <div>
          <span data-testid="line-subtotal">{item?.subtotal ?? 0}</span>
          <span data-testid="cart-total">{cart?.total ?? 0}</span>
          <button onClick={() => addItem('prod-1', 1)} data-testid="add-btn">Add</button>
          <button onClick={() => updateItem(item!.id, 3)} data-testid="update-btn">Update</button>
        </div>
      );
    };

    renderWithProviders(<TestComponent />);

    await waitFor(() => {
      expect(screen.getByTestId('cart-total')).toHaveTextContent('0');
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('add-btn'));
    });
    await waitFor(() => {
      expect(screen.getByTestId('line-subtotal')).toHaveTextContent('29.99');
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId('update-btn'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('line-subtotal')).toHaveTextContent('89.97');
    });
    await waitFor(() => {
      expect(screen.getByTestId('cart-total')).toHaveTextContent('89.97');
    });
  });

  it('authenticated path still delegates to cartApi and does not use guest zero pricing', async () => {
    isAuthenticated = true;
    const cartWithItem: Cart = {
      ...emptyGuestCart,
      items: [
        {
          id: 'item-1',
          productId: 'prod-1',
          productName: 'Widget',
          productThumbnail: '',
          price: 29.99,
          currency: 'USD',
          quantity: 2,
          subtotal: 59.98,
          attributes: {},
          maxQuantity: 99,
        },
      ],
      subtotal: 59.98,
      total: 59.98,
      itemCount: 2,
    };
    vi.spyOn(cartApi, 'addItem').mockResolvedValue(cartWithItem);

    const TestComponent = () => {
      const { cart, addItem } = useCart();
      return (
        <div>
          <span data-testid="cart-total">{cart?.total ?? 0}</span>
          <button onClick={() => addItem('prod-1', 2)} data-testid="add-btn">Add</button>
        </div>
      );
    };

    renderWithProviders(<TestComponent />);

    await act(async () => {
      fireEvent.click(screen.getByTestId('add-btn'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('cart-total')).toHaveTextContent('59.98');
    });

    expect(cartApi.addItem).toHaveBeenCalledWith('prod-1', 2, undefined);
    expect(productApi.getProduct).not.toHaveBeenCalled();
  });
});
