import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ProductCardComponent, ProductImage } from '@components/Card';
import type { ProductSummary } from '@types/domain';
import { ThemeProvider } from '@mui/material';
import { theme } from '@styles/theme';
import { CartProvider } from '@context/CartContext';

const mockProduct: ProductSummary = {
  id: '1',
  name: 'Test Product',
  description: 'A test product description',
  price: 2999,
  currency: 'USD',
  thumbnail: 'https://example.com/image.jpg',
  categoryId: 'cat-1',
  categoryName: 'Electronics',
  variantCount: 3,
  stockQuantity: 10,
  averageRating: 4.5,
  reviewCount: 10,
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

describe('ProductCardComponent', () => {
  it('renders product name', () => {
    renderWithProviders(<ProductCardComponent product={mockProduct} />);
    expect(screen.getByTestId('product-name')).toHaveTextContent('Test Product');
  });

  it('renders formatted price', () => {
    renderWithProviders(<ProductCardComponent product={mockProduct} />);
    expect(screen.getByTestId('product-price')).toHaveTextContent('$29.99');
  });

  it('renders product image', () => {
    renderWithProviders(<ProductCardComponent product={mockProduct} />);
    const image = screen.getByTestId('product-image');
    expect(image).toHaveAttribute('src', 'https://example.com/image.jpg');
    expect(image).toHaveAttribute('alt', 'Test Product');
  });

  it('shows Add to Cart button', () => {
    renderWithProviders(<ProductCardComponent product={mockProduct} />);
    expect(screen.getByTestId('add-to-cart-btn')).toBeInTheDocument();
    expect(screen.getByTestId('add-to-cart-btn')).not.toBeDisabled();
    expect(screen.getByTestId('add-to-cart-btn')).toHaveTextContent('Add to Cart');
  });

  it('shows Out of Stock when stock is 0', () => {
    const outOfStockProduct = { ...mockProduct, stockQuantity: 0 };
    renderWithProviders(<ProductCardComponent product={outOfStockProduct} />);
    expect(screen.getByTestId('add-to-cart-btn')).toBeDisabled();
    expect(screen.getByTestId('add-to-cart-btn')).toHaveTextContent('Out of Stock');
  });

  it('renders product card with correct test id', () => {
    renderWithProviders(<ProductCardComponent product={mockProduct} />);
    expect(screen.getByTestId('product-card')).toBeInTheDocument();
  });
});

describe('ProductImage', () => {
  it('renders with correct background image', () => {
    renderWithProviders(<ProductImage src="https://example.com/image.jpg" data-testid="product-image" />);
    const div = screen.getByTestId('product-image');
    expect(div).toHaveStyleRule('background-image', 'url("https://example.com/image.jpg")');
  });
});