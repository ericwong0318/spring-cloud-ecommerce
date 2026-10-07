import { Card as MuiCard, CardContent, CardActions, Typography, IconButton, Box } from '@mui/material';
import { styled } from '@mui/material/styles';
import { forwardRef } from 'react';
import { FavoriteBorder, Favorite } from '@mui/icons-material';
import { useCart } from '../hooks/useCart';
import { formatCurrency } from '../utils/formatters';
import type { ProductSummary } from '../types/domain';

export const ProductCard = styled(MuiCard)`
  display: flex;
  flex-direction: column;
  height: 100%;
  transition: all 0.2s ease;
  
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
  }
`;

export const ProductImage = styled('div')<{ src: string }>`
  width: 100%;
  aspect-ratio: 1;
  background-image: url(${(props) => props.src});
  background-size: cover;
  background-position: center;
  position: relative;
`;

export const ProductInfo = styled(CardContent)`
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
`;

export const ProductName = styled(Typography)`
  font-weight: 600;
  font-size: 0.875rem;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
`;

export const ProductPrice = styled(Typography)`
  font-weight: 700;
  font-size: 1.125rem;
  color: #1976d2;
  margin-bottom: 12px;
`;

export const ProductActions = styled(CardActions)`
  padding: 0 16px 16px;
  justify-content: space-between;
`;

export const AddToCartButton = styled('button')`
  background-color: #1976d2;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-weight: 500;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover:not(:disabled) {
    background-color: #1565c0;
  }
  
  &:disabled {
    background-color: #ccc;
    color: #888;
    cursor: not-allowed;
  }
`;

export const WishlistButton = styled(IconButton)`
  position: absolute;
  top: 8px;
  right: 8px;
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
  
  &:hover {
    background-color: white;
  }
`;

interface ProductCardProps {
  product: ProductSummary;
  showWishlist?: boolean;
  onWishlistToggle?: (productId: string) => void;
  isInWishlist?: boolean;
}

export const ProductCardComponent = forwardRef<HTMLDivElement, ProductCardProps>(
  ({ product, showWishlist = true, onWishlistToggle, isInWishlist = false }, ref) => {
    const { addItem, isSyncing } = useCart();
    
    const handleAddToCart = async () => {
      try {
        await addItem(product.id, 1);
      } catch (error) {
        console.error('Failed to add to cart:', error);
      }
    };
    
    return (
      <ProductCard ref={ref} elevation={0} variant="outlined">
        <ProductImage src={product.thumbnail} />
        {showWishlist && (
          <WishlistButton
            onClick={() => onWishlistToggle?.(product.id)}
            aria-label={isInWishlist ? 'Remove from wishlist' : 'Add to wishlist'}
          >
            {isInWishlist ? <Favorite color="error" /> : <FavoriteBorder />}
          </WishlistButton>
        )}
        <ProductInfo>
          <ProductName>{product.name}</ProductName>
          <ProductPrice>{formatCurrency(product.price, product.currency)}</ProductPrice>
        </ProductInfo>
        <ProductActions>
          <AddToCartButton onClick={handleAddToCart} disabled={isSyncing || product.stockQuantity === 0}>
            {product.stockQuantity === 0 ? 'Out of Stock' : 'Add to Cart'}
          </AddToCartButton>
        </ProductActions>
      </ProductCard>
    );
  }
);

ProductCardComponent.displayName = 'ProductCardComponent';

export const InfoCard = styled(MuiCard)`
  padding: 24px;
  text-align: center;
  height: 100%;
`;

export const InfoCardIcon = styled(Box)`
  font-size: 2.5rem;
  color: #1976d2;
  margin-bottom: 16px;
`;

export const InfoCardTitle = styled(Typography)`
  font-weight: 600;
  font-size: 1.125rem;
  margin-bottom: 8px;
`;

export const InfoCardDescription = styled(Typography)`
  color: #666;
  font-size: 0.875rem;
`;