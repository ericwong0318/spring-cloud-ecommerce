import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import React from 'react';
import { ProductList } from '@pages/ProductList';
import { productApi } from '@services/endpoints';
import { useQuery } from '@tanstack/react-query';
import type { ProductSummary } from '@types/domain';
import { ThemeProvider } from '@mui/material';
import { theme } from '@styles/theme';
import { CartProvider } from '@context/CartContext';

vi.mock('@services/endpoints', () => ({
  productApi: {
    getProducts: vi.fn(),
    searchProducts: vi.fn(),
  },
  categoryApi: {
    getCategoryTree: vi.fn().mockResolvedValue([]),
  },
}));

vi.mock('@context/AuthContext', () => ({
  useAuth: () => ({ isAuthenticated: false }),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

const mockFull: ProductSummary[] = [
  {
    id: '1',
    name: 'Wireless Mouse',
    price: 2999,
    currency: 'USD',
    thumbnail: 'https://example.com/mouse.jpg',
    slug: 'wireless-mouse',
    categoryId: 'cat-1',
    stockQuantity: 10,
    isActive: true,
  },
  {
    id: '2',
    name: 'Mechanical Keyboard',
    price: 8999,
    currency: 'USD',
    thumbnail: 'https://example.com/keyboard.jpg',
    slug: 'mechanical-keyboard',
    categoryId: 'cat-1',
    stockQuantity: 5,
    isActive: true,
  },
];

const mockSearchResults: ProductSummary[] = [
  {
    id: '1',
    name: 'Wireless Mouse',
    price: 2999,
    currency: 'USD',
    thumbnail: 'https://example.com/mouse.jpg',
    slug: 'wireless-mouse',
    categoryId: 'cat-1',
    stockQuantity: 10,
    isActive: true,
  },
];

const renderWithProviders = (ui: React.ReactElement) =>
  render(
    <ThemeProvider theme={theme}>
      <CartProvider>
        {ui}
      </CartProvider>
    </ThemeProvider>
  );

const mockUseQuery = vi.mocked(useQuery);

const searchInput = () => screen.getAllByPlaceholderText('Search products...')[0];

describe('ProductList search', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Mock useQuery so it drives the component with real React state, invoking queryFn asynchronously.
    mockUseQuery.mockImplementation((options: any) => {
      const [data, setData] = React.useState<unknown>(undefined);
      React.useEffect(() => {
        let cancelled = false;
        if (options.queryFn) {
          options.queryFn().then((result) => {
            if (!cancelled) setData(result);
          });
        }
        return () => {
          cancelled = true;
        };
      }, [options.queryFn]);
      return { data, isLoading: data === undefined, error: null };
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const advanceDebounce = () =>
    act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 400));
    });

  it('calls searchProducts with the typed term and displays matching products', async () => {
    (productApi.getProducts as ReturnType<typeof vi.fn>).mockResolvedValue(mockFull);
    (productApi.searchProducts as ReturnType<typeof vi.fn>).mockResolvedValue(mockSearchResults);

    renderWithProviders(<ProductList />);

    // Initially full list loads via getProducts
    expect(productApi.getProducts).toHaveBeenCalled();
    expect(productApi.searchProducts).not.toHaveBeenCalled();

    fireEvent.change(searchInput(), { target: { value: 'mouse' } });
    await advanceDebounce();

    expect(productApi.searchProducts).toHaveBeenCalledWith('mouse', expect.anything());

    await waitFor(() => {
      expect(screen.getByText('Wireless Mouse')).toBeInTheDocument();
    });
    expect(screen.queryByText('Mechanical Keyboard')).not.toBeInTheDocument();
  });

  it('clearing the search returns the full product list', async () => {
    (productApi.getProducts as ReturnType<typeof vi.fn>).mockResolvedValue(mockFull);
    (productApi.searchProducts as ReturnType<typeof vi.fn>).mockResolvedValue(mockSearchResults);

    renderWithProviders(<ProductList />);

    fireEvent.change(searchInput(), { target: { value: 'mouse' } });
    await advanceDebounce();
    expect(productApi.searchProducts).toHaveBeenCalled();

    // Clear the search field
    fireEvent.change(searchInput(), { target: { value: '' } });
    await advanceDebounce();

    expect(productApi.getProducts).toHaveBeenCalledTimes(2);
    await waitFor(() => {
      expect(screen.getByText('Wireless Mouse')).toBeInTheDocument();
      expect(screen.getByText('Mechanical Keyboard')).toBeInTheDocument();
    });
  });
});
