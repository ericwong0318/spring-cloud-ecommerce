import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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

// Prices stay within the slider's 0-1000 range so the slider can actually narrow the list.
const products: ProductSummary[] = [
  {
    id: '1',
    name: 'Cheap Widget',
    price: 199,
    currency: 'USD',
    thumbnail: 'https://example.com/cheap.jpg',
    slug: 'cheap-widget',
    categoryId: 'c1',
    stockQuantity: 10,
    isActive: true,
  },
  {
    id: '2',
    name: 'Mid Widget',
    price: 599,
    currency: 'USD',
    thumbnail: 'https://example.com/mid.jpg',
    slug: 'mid-widget',
    categoryId: 'c1',
    stockQuantity: 0,
    isActive: true,
  },
  {
    id: '3',
    name: 'Pricey Widget',
    price: 999,
    currency: 'USD',
    thumbnail: 'https://example.com/pricey.jpg',
    slug: 'pricey-widget',
    categoryId: 'c1',
    stockQuantity: 8,
    isActive: true,
  },
];

const renderWithProviders = (ui: React.ReactElement) =>
  render(
    <ThemeProvider theme={theme}>
      <CartProvider>{ui}</CartProvider>
    </ThemeProvider>
  );

const mockUseQuery = vi.mocked(useQuery);
let user: ReturnType<typeof userEvent.setup>;

// Desktop slider/checkbox render first in the DOM (sidebar filters precede the hidden drawer).
const maxThumb = () => document.querySelectorAll('input[type="range"]')[1];
const inStockCheckbox = () => document.querySelectorAll('input[type="checkbox"]')[0];

describe('ProductList price and in-stock filters', () => {
  beforeEach(() => {
    user = userEvent.setup();
    vi.clearAllMocks();
    // Drive the component with real React state so queryFn (=> productApi.getProducts) is invoked as filters change.
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
    // Mirror the backend filtering contract so a narrowed filter set really narrows what renders.
    (productApi.getProducts as ReturnType<typeof vi.fn>).mockImplementation((filters: any) => {
      let result = products;
      if (filters.minPrice !== undefined) result = result.filter((p) => p.price >= filters.minPrice);
      if (filters.maxPrice !== undefined) result = result.filter((p) => p.price <= filters.maxPrice);
      if (filters.inStockOnly) result = result.filter((p) => p.stockQuantity > 0);
      return Promise.resolve(result);
    });
  });

  const advance = () =>
    act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 400));
    });

  it('loads all products with no price or in-stock filter by default', async () => {
    renderWithProviders(<ProductList />);

    await waitFor(() => {
      expect(screen.getByText('Cheap Widget')).toBeInTheDocument();
      expect(screen.getByText('Mid Widget')).toBeInTheDocument();
      expect(screen.getByText('Pricey Widget')).toBeInTheDocument();
    });

    expect(productApi.getProducts).toHaveBeenCalledTimes(1);
    expect((productApi.getProducts as ReturnType<typeof vi.fn>).mock.calls[0][0]).toMatchObject({
      minPrice: undefined,
      maxPrice: undefined,
      inStockOnly: false,
    });
  });

  it('moving the price slider max to 500 narrows results to products at or below 500', async () => {
    renderWithProviders(<ProductList />);
    await waitFor(() => expect(screen.getByText('Cheap Widget')).toBeInTheDocument());

    act(() => {
      fireEvent.change(maxThumb()!, { target: { value: 500 } });
    });
    await advance();

    // getProducts is re-fetched with maxPrice = 500
    const lastCall = (productApi.getProducts as ReturnType<typeof vi.fn>).mock.calls.at(-1)![0];
    expect(lastCall).toMatchObject({ minPrice: undefined, maxPrice: 500 });

    await waitFor(() => {
      expect(screen.getByText('Cheap Widget')).toBeInTheDocument();
      expect(screen.queryByText('Mid Widget')).not.toBeInTheDocument();
      expect(screen.queryByText('Pricey Widget')).not.toBeInTheDocument();
    });
    expect(screen.getByText(/1 product found/)).toBeInTheDocument();
  });

  it('toggling "In Stock Only" narrows results to products with stock available', async () => {
    renderWithProviders(<ProductList />);
    await waitFor(() => expect(screen.getByText('Mid Widget')).toBeInTheDocument());

    await act(async () => {
      await user.click(inStockCheckbox()!);
    });
    await advance();

    const lastCall = (productApi.getProducts as ReturnType<typeof vi.fn>).mock.calls.at(-1)![0];
    expect(lastCall).toMatchObject({ inStockOnly: true });

    await waitFor(() => {
      expect(screen.getByText('Cheap Widget')).toBeInTheDocument();
      expect(screen.getByText('Pricey Widget')).toBeInTheDocument();
      expect(screen.queryByText('Mid Widget')).not.toBeInTheDocument();
    });
    expect(screen.getByText(/2 products found/)).toBeInTheDocument();
  });

  it('combines price and in-stock filters and clears them together', async () => {
    renderWithProviders(<ProductList />);
    await waitFor(() => expect(screen.getByText('Pricey Widget')).toBeInTheDocument());

    // Apply both filters: max price 500 + in stock only -> only Cheap Widget remains
    await act(async () => {
      fireEvent.change(maxThumb()!, { target: { value: 500 } });
      await user.click(inStockCheckbox()!);
    });
    await advance();

    const lastCall = (productApi.getProducts as ReturnType<typeof vi.fn>).mock.calls.at(-1)![0];
    expect(lastCall).toMatchObject({ minPrice: undefined, maxPrice: 500, inStockOnly: true });

    await waitFor(() => {
      expect(screen.getByText('Cheap Widget')).toBeInTheDocument();
      expect(screen.queryByText('Mid Widget')).not.toBeInTheDocument();
      expect(screen.queryByText('Pricey Widget')).not.toBeInTheDocument();
    });

    // Clearing filters restores the full list
    const clearButton = screen.getAllByRole('button', { name: /Clear All/ })[0];
    act(() => {
      fireEvent.click(clearButton);
    });
    await advance();

    await waitFor(() => {
      expect(screen.getByText('Cheap Widget')).toBeInTheDocument();
      expect(screen.getByText('Mid Widget')).toBeInTheDocument();
      expect(screen.getByText('Pricey Widget')).toBeInTheDocument();
    });
    expect(screen.getByText(/3 products found/)).toBeInTheDocument();
  });
});
