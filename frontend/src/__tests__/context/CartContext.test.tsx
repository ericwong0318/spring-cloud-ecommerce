import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, act, waitFor } from '@testing-library/react';
import { CartProvider, useCart } from '@context/CartContext';
import { ThemeProvider } from '@mui/material';
import { theme } from '@styles/theme';

const mockCart = {
  id: 'cart-1',
  items: [],
  subtotal: 0,
  tax: 0,
  shipping: 0,
  total: 0,
  itemCount: 0,
  currency: 'USD',
};

const mockCartWithItem = {
  ...mockCart,
  items: [
    {
      id: 'item-1',
      productId: 'prod-1',
      productName: 'Test Product',
      productImage: 'https://example.com/image.jpg',
      quantity: 2,
      unitPrice: 2999,
      totalPrice: 5998,
      currency: 'USD',
      attributes: { color: 'Red', size: 'M' },
    },
  ],
  subtotal: 5998,
  tax: 479.84,
  shipping: 0,
  total: 6477.84,
  itemCount: 2,
  currency: 'USD',
};

const renderWithProviders = (component: React.ReactElement) => {
  return render(
    <ThemeProvider theme={theme}>
      <CartProvider>
        {component}
      </CartProvider>
    </ThemeProvider>
  );
};

describe('CartContext', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('provides cart state and actions', () => {
    const TestComponent = () => {
      const cart = useCart();
      return (
        <div>
          <span data-testid="item-count">{cart.cart?.itemCount ?? 0}</span>
          <button onClick={() => cart.addItem('prod-1', 1)} data-testid="add-btn">Add</button>
        </div>
      );
    };

    renderWithProviders(<TestComponent />);
    expect(screen.getByTestId('item-count')).toHaveTextContent('0');
  });

  it('adds item to cart', async () => {
    const TestComponent = () => {
      const { cart, addItem, isLoading } = useCart();
      return (
        <div>
          <span data-testid="item-count">{cart?.itemCount ?? 0}</span>
          <span data-testid="loading">{isLoading.toString()}</span>
          <button
            onClick={() => addItem('prod-1', 1, { color: 'Red' })}
            data-testid="add-btn"
            disabled={isLoading}
          >
            Add
          </button>
        </div>
      );
    };

    // Mock the API call
    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockCartWithItem,
    } as Response);

    renderWithProviders(<TestComponent />);

    await act(async () => {
      fireEvent.click(screen.getByTestId('add-btn'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('item-count')).toHaveTextContent('2');
    });
  });

  it('updates item quantity', async () => {
    const TestComponent = () => {
      const { cart, updateQuantity } = useCart();
      return (
        <div>
          <span data-testid="item-count">{cart?.itemCount ?? 0}</span>
          <button onClick={() => updateQuantity('item-1', 3)} data-testid="update-btn">Update</button>
        </div>
      );
    };

    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => ({ ...mockCartWithItem, items: [{ ...mockCartWithItem.items[0], quantity: 3, totalPrice: 8997 }], itemCount: 3, subtotal: 8997, total: 9716.76 }),
    } as Response);

    renderWithProviders(<TestComponent />);

    await act(async () => {
      fireEvent.click(screen.getByTestId('update-btn'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('item-count')).toHaveTextContent('3');
    });
  });

  it('removes item from cart', async () => {
    const TestComponent = () => {
      const { cart, removeItem } = useCart();
      return (
        <div>
          <span data-testid="item-count">{cart?.itemCount ?? 0}</span>
          <button onClick={() => removeItem('item-1')} data-testid="remove-btn">Remove</button>
        </div>
      );
    };

    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockCart,
    } as Response);

    renderWithProviders(<TestComponent />);

    await act(async () => {
      fireEvent.click(screen.getByTestId('remove-btn'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('item-count')).toHaveTextContent('0');
    });
  });

  it('clears cart', async () => {
    const TestComponent = () => {
      const { cart, clearCart } = useCart();
      return (
        <div>
          <span data-testid="item-count">{cart?.itemCount ?? 0}</span>
          <button onClick={() => clearCart()} data-testid="clear-btn">Clear</button>
        </div>
      );
    };

    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockCart,
    } as Response);

    renderWithProviders(<TestComponent />);

    await act(async () => {
      fireEvent.click(screen.getByTestId('clear-btn'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('item-count')).toHaveTextContent('0');
    });
  });

  it('syncs guest cart on login', async () => {
    localStorage.setItem('guest_cart', JSON.stringify(mockCartWithItem));

    const TestComponent = () => {
      const { syncGuestCart, isSyncing } = useCart();
      return (
        <div>
          <span data-testid="syncing">{isSyncing.toString()}</span>
          <button onClick={() => syncGuestCart()} data-testid="sync-btn" disabled={isSyncing}>
            Sync
          </button>
        </div>
      );
    };

    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockCartWithItem,
    } as Response);

    renderWithProviders(<TestComponent />);

    await act(async () => {
      fireEvent.click(screen.getByTestId('sync-btn'));
    });

    await waitFor(() => {
      expect(screen.getByTestId('syncing')).toHaveTextContent('false');
    });
  });
});

import { fireEvent } from '@testing-library/react';