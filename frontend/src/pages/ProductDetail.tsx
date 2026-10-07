import { useState } from 'react';
import { useParams, useNavigate, Link as RouterLink } from 'react-router-dom';
import { Box, Grid, Typography, Button, Card, CardMedia, Chip, IconButton, Breadcrumbs, Link, Tabs, Tab, Accordion, AccordionSummary, AccordionDetails, Rating, Alert, Skeleton, Modal, Backdrop, Fade, Zoom, TextField } from '@mui/material';
import { AddShoppingCart, FavoriteBorder, Share, Close, KeyboardArrowLeft, KeyboardArrowRight, ExpandMore } from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { productApi } from '../services/endpoints';
import { formatCurrency } from '../utils/formatters';
import { useCart } from '../hooks/useCart';
import { useNotification } from '../hooks/useNotification';
import { ProductCardComponent } from '@components/Card';


interface ProductDetailProps {
  productId?: string;
}

export function ProductDetail({ productId: propProductId }: ProductDetailProps) {
  const { id: paramId } = useParams<{ id: string }>();
  const productId = propProductId || paramId;
  const navigate = useNavigate();
  const { addItem, isSyncing } = useCart();
  const { showNotification } = useNotification();

  const [selectedImage, setSelectedImage] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [tab, setTab] = useState(0);
  const [showImageModal, setShowImageModal] = useState(false);

  const { data: product, isLoading, error } = useQuery({
    queryKey: ['product', productId],
    queryFn: () => productApi.getProduct(productId!),
    enabled: !!productId,
    staleTime: 1000 * 60 * 5,
  });

  const { data: relatedProducts } = useQuery({
    queryKey: ['products', 'related', productId],
    queryFn: () => productApi.getRelatedProducts(productId!, 4),
    enabled: !!productId,
    staleTime: 1000 * 60 * 10,
  });

  const handleAddToCart = async () => {
    if (!product) return;
    
    try {
      await addItem(product.id, quantity);
      showNotification({
        type: 'success',
        message: `${product.name} added to cart`,
        action: {
          label: 'View Cart',
          onClick: () => navigate('/cart'),
        },
      });
    } catch {
      showNotification({
        type: 'error',
        message: 'Failed to add to cart',
      });
    }
  };

  const handleBuyNow = async () => {
    if (!product) return;
    
    try {
      await addItem(product.id, quantity);
      navigate('/checkout');
    } catch {
      showNotification({
        type: 'error',
        message: 'Failed to add to cart',
      });
    }
  };

  const handleShare = async () => {
    if (navigator.share) {
      try {
        await navigator.share({
          title: product?.name,
          text: product?.description,
          url: window.location.href,
        });
      } catch {
        // User cancelled
      }
    } else {
      navigator.clipboard.writeText(window.location.href);
      showNotification({ type: 'success', message: 'Link copied to clipboard' });
    }
  };

  if (isLoading) {
    return (
      <Box>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Skeleton variant="rectangular" height={500} />
          </Grid>
          <Grid item xs={12} md={6}>
            <Skeleton variant="text" width="40%" />
            <Skeleton variant="text" width="60%" />
            <Skeleton variant="text" width="80%" />
            <Skeleton variant="text" width="100%" />
            <Skeleton variant="text" width="100%" />
          </Grid>
        </Grid>
      </Box>
    );
  }

  if (error || !product) {
    return (
      <Box sx={{ textAlign: 'center', py: 6 }}>
        <Typography variant="h6" color="error" gutterBottom>Product Not Found</Typography>
        <Typography color="text.secondary" paragraph>The product you're looking for doesn't exist or has been removed.</Typography>
        <Button variant="contained" onClick={() => navigate('/products')} sx={{ mt: 2 }}>
          Continue Shopping
        </Button>
      </Box>
    );
  }

  const images = product.images.length > 0 ? product.images : [product.thumbnail];
  const inStock = product.stockQuantity > 0;
  const maxQuantity = Math.min(product.stockQuantity, 99);

  return (
    <Box>
      {/* Breadcrumbs */}
      <Box sx={{ mb: 3 }}>
        <Breadcrumbs separator=">" maxItems={3}>
          <Link component={RouterLink} to="/" underline="hover">Home</Link>
          <Link component={RouterLink} to="/products" underline="hover">Products</Link>
          {product.categoryName && <Link component={RouterLink} to={`/products?category=${product.categoryId}`} underline="hover">{product.categoryName}</Link>}
          <Typography color="text.primary">{product.name}</Typography>
        </Breadcrumbs>
      </Box>

      <Grid container spacing={3}>
        {/* Image Gallery */}
        <Grid item xs={12} md={6} lg={5}>
          <Box sx={{ position: 'sticky', top: 100, height: 'fit-content' }}>
            {/* Main Image */}
            <Card sx={{ borderRadius: 2, overflow: 'hidden', mb: 2 }}>
              <CardMedia
                component="img"
                height={500}
                image={images[selectedImage]}
                alt={product.name}
                sx={{ objectFit: 'contain', backgroundColor: 'background.default', cursor: 'zoom-in', width: '100%' }}
                onClick={() => setShowImageModal(true)}
              />
            </Card>

            {/* Thumbnails */}
            {images.length > 1 && (
              <Box sx={{ display: 'flex', gap: 1, overflowX: 'auto', pb: 1 }}>
                {images.map((image: string, index: number) => (
                  <Box
                    key={index}
                    sx={{
                      width: 80,
                      height: 80,
                      borderRadius: 1,
                      border: selectedImage === index ? 2 : 1,
                      borderColor: selectedImage === index ? 'primary.main' : 'divider',
                      cursor: 'pointer',
                      overflow: 'hidden',
                      flexShrink: 0,
                      transition: 'all 0.2s ease',
                      '&:hover': { borderColor: 'primary.main' },
                    }}
                    onClick={() => setSelectedImage(index)}
                  >
                    <img src={image} alt={`${product.name} - ${index + 1}`} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  </Box>
                ))}
              </Box>
            )}
          </Box>
        </Grid>

        {/* Product Info */}
        <Grid item xs={12} md={6} lg={7}>
          <Box>
            {/* Category */}
            {product.categoryName && (
              <Chip label={product.categoryName} size="small" variant="outlined" color="primary" sx={{ mb: 1 }} />
            )}

            {/* Title */}
            <Typography variant="h3" component="h1" fontWeight={700} gutterBottom>
              {product.name}
            </Typography>

            {/* Rating & Stock */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2, flexWrap: 'wrap' }}>
              <Rating value={4.5} precision={0.5} readOnly size="medium" max={5} />
              <Typography variant="body2" color={inStock ? 'success.main' : 'error.main'} fontWeight={500}>
                {inStock ? `In Stock (${product.stockQuantity} available)` : 'Out of Stock'}
              </Typography>
            </Box>

            {/* Price */}
            <Typography variant="h4" component="div" fontWeight={700} color="primary.main" sx={{ mb: 3 }}>
              {formatCurrency(product.price, product.currency)}
            </Typography>

            {/* Description */}
            <Typography variant="body1" color="text.secondary" paragraph sx={{ mb: 3, lineHeight: 1.7 }}>
              {product.description}
            </Typography>

            {/* Attributes */}
            {product.attributes && Object.keys(product.attributes).length > 0 && (
              <Box sx={{ mb: 3, p: 2, backgroundColor: 'action.hover', borderRadius: 1 }}>
                <Typography variant="subtitle1" fontWeight={600} gutterBottom>Specifications</Typography>
                <Grid container spacing={1}>
                  {Object.entries(product.attributes as Record<string, unknown>).map(([key, value]) => (
                    <Grid item xs={6} key={key}>
                      <Typography variant="body2" color="text.secondary" fontWeight={500}>{key}:</Typography>
                      <Typography variant="body2">{String(value)}</Typography>
                    </Grid>
                  ))}
                </Grid>
              </Box>
            )}

            {/* Quantity & Add to Cart */}
            <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 3, alignItems: 'center' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', border: 1, borderColor: 'divider', borderRadius: 1 }}>
                <IconButton onClick={() => setQuantity(Math.max(1, quantity - 1))} disabled={quantity <= 1} size="small">
                  <KeyboardArrowLeft fontSize="small" />
                </IconButton>
                <TextField
                  value={quantity}
                  onChange={(e) => setQuantity(Math.min(maxQuantity, Math.max(1, Number(e.target.value) || 1)))}
                  inputProps={{ style: { width: 60, textAlign: 'center', border: 'none', outline: 'none' }, min: 1, max: maxQuantity }}
                  size="small"
                  variant="standard"
                  type="number"
                  InputProps={{ disableUnderline: true, sx: { textAlign: 'center' } }}
                />
                <IconButton onClick={() => setQuantity(Math.min(maxQuantity, quantity + 1))} disabled={quantity >= maxQuantity} size="small">
                  <KeyboardArrowRight fontSize="small" />
                </IconButton>
              </Box>
              
              <Button
                variant="contained"
                size="large"
                startIcon={<AddShoppingCart />}
                onClick={handleAddToCart}
                disabled={!inStock || isSyncing}
                sx={{ flex: 1, minWidth: 200 }}
              >
                {isSyncing ? 'Adding...' : 'Add to Cart'}
              </Button>

              <Button
                variant="outlined"
                size="large"
                onClick={handleBuyNow}
                disabled={!inStock || isSyncing}
                sx={{ minWidth: 150 }}
              >
                Buy Now
              </Button>
            </Box>

            {/* Wishlist & Share */}
            <Box sx={{ display: 'flex', gap: 1, mb: 3 }}>
              <Button variant="outlined" startIcon={<FavoriteBorder />} size="small">Save for Later</Button>
              <Button variant="outlined" startIcon={<Share />} size="small" onClick={handleShare}>Share</Button>
            </Box>

            {/* Tabs */}
            <Tabs value={tab} onChange={(_, value) => setTab(value)} variant="standard" sx={{ borderBottom: 1, borderColor: 'divider' }}>
              <Tab label="Description" />
              <Tab label="Specifications" />
              <Tab label="Reviews" />
            </Tabs>

            <Box sx={{ mt: 3 }}>
              {tab === 0 && (
                <Typography variant="body1" color="text.secondary" paragraph lineHeight={1.7}>
                  {product.description}
                </Typography>
              )}

              {tab === 1 && (
                <Accordion>
                  <AccordionSummary expandIcon={<ExpandMore />}>
                    <Typography fontWeight={500}>Product Details</Typography>
                  </AccordionSummary>
                  <AccordionDetails>
                    <Grid container spacing={2}>
                      {Object.entries(product.attributes as Record<string, unknown>).map(([key, value]) => (
                        <Grid item xs={6} key={key}>
                          <Typography variant="body2" color="text.secondary" fontWeight={500}>{key}</Typography>
                          <Typography variant="body2">{String(value)}</Typography>
                        </Grid>
                      ))}
                      <Grid item xs={6}>
                        <Typography variant="body2" color="text.secondary" fontWeight={500}>SKU</Typography>
                        <Typography variant="body2">{product.id}</Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="body2" color="text.secondary" fontWeight={500}>Category</Typography>
                        <Typography variant="body2">{product.categoryName || 'N/A'}</Typography>
                      </Grid>
                      <Grid item xs={6}>
                        <Typography variant="body2" color="text.secondary" fontWeight={500}>Stock</Typography>
                        <Typography variant="body2" color={inStock ? 'success.main' : 'error.main'}>
                          {inStock ? `${product.stockQuantity} in stock` : 'Out of stock'}
                        </Typography>
                      </Grid>
                    </Grid>
                  </AccordionDetails>
                </Accordion>
              )}

              {tab === 2 && (
                <Alert severity="info" variant="outlined">
                  <Typography variant="body1">Customer reviews will be available soon.</Typography>
                </Alert>
              )}
            </Box>
          </Box>
        </Grid>
      </Grid>

      {/* Related Products */}
      {relatedProducts && relatedProducts.length > 0 && (
        <Box sx={{ mt: 6 }}>
          <Typography variant="h4" component="h2" fontWeight={600} gutterBottom>
            You May Also Like
          </Typography>
          <Grid container spacing={3}>
            {relatedProducts.map((product) => (
              <Grid item xs={6} sm={4} md={3} key={product.id}>
                <ProductCardComponent product={product} />
              </Grid>
            ))}
          </Grid>
        </Box>
      )}

      {/* Image Modal */}
      <Modal open={showImageModal} onClose={() => setShowImageModal(false)} closeAfterTransition BackdropComponent={Backdrop} BackdropProps={{ timeout: 500 }}>
        <Fade in={showImageModal}>
          <Box sx={{ position: 'relative', maxWidth: '90vw', maxHeight: '90vh', mx: 'auto', my: 'auto' }}>
            <IconButton
              onClick={() => setShowImageModal(false)}
              sx={{ position: 'absolute', top: -12, right: -12, bgcolor: 'background.paper', boxShadow: 3, zIndex: 1301 }}
              aria-label="Close"
            >
              <Close />
            </IconButton>
            <Zoom in={showImageModal}>
              <img
                src={images[selectedImage]}
                alt={product.name}
                style={{ maxWidth: '100%', maxHeight: '90vh', borderRadius: '8px', boxShadow: '0 4px 20px rgba(0,0,0,0.3)' }}
              />
            </Zoom>
            {images.length > 1 && (
              <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1, mt: 2 }}>
                <IconButton onClick={() => setSelectedImage((prev) => (prev === 0 ? images.length - 1 : prev - 1))}>
                  <KeyboardArrowLeft />
                </IconButton>
                <IconButton onClick={() => setSelectedImage((prev) => (prev === images.length - 1 ? 0 : prev + 1))}>
                  <KeyboardArrowRight />
                </IconButton>
              </Box>
            )}
          </Box>
        </Fade>
      </Modal>
    </Box>
  );
}