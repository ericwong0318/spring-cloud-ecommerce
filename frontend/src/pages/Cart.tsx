import { Box, Grid, Typography, Button, Card, CardMedia, IconButton, TextField, Divider, Alert, Stack, Skeleton, Paper } from '@mui/material';
import { Delete, Add, Remove, LocalOffer } from '@mui/icons-material';
import { useNavigate, Link } from 'react-router-dom';
import { useState } from 'react';
import { useCart } from '../hooks/useCart';
import { useNotification } from '../hooks/useNotification';
import { formatCurrency } from '../utils/formatters';

export function Cart() {
  const navigate = useNavigate();
  const { cart, isLoading, isSyncing, updateItem, removeItem, clearCart, applyPromoCode, removePromoCode, error } = useCart();
  const { showNotification } = useNotification();

  const [promoCode, setPromoCode] = useState('');

  const handleQuantityChange = async (itemId: string, newQuantity: number) => {
    if (newQuantity < 1) {
      await handleRemoveItem(itemId);
      return;
    }
    try {
      await updateItem(itemId, newQuantity);
    } catch {
      showNotification({ type: 'error', message: 'Failed to update quantity' });
    }
  };

  const handleRemoveItem = async (itemId: string) => {
    try {
      await removeItem(itemId);
      showNotification({ type: 'success', message: 'Item removed from cart' });
    } catch {
      showNotification({ type: 'error', message: 'Failed to remove item' });
    }
  };

  const handleClearCart = async () => {
    if (window.confirm('Are you sure you want to clear your cart?')) {
      try {
        await clearCart();
        showNotification({ type: 'success', message: 'Cart cleared' });
      } catch {
        showNotification({ type: 'error', message: 'Failed to clear cart' });
      }
    }
  };

  const handleApplyPromo = async () => {
    if (!promoCode.trim()) return;
    try {
      await applyPromoCode(promoCode.trim());
      showNotification({ type: 'success', message: 'Promo code applied' });
      setPromoCode('');
    } catch {
      showNotification({ type: 'error', message: 'Invalid promo code' });
    }
  };

  const handleRemovePromo = async () => {
    try {
      await removePromoCode();
      showNotification({ type: 'success', message: 'Promo code removed' });
    } catch {
      showNotification({ type: 'error', message: 'Failed to remove promo code' });
    }
  };

  if (isLoading) {
    return (
      <Box>
        <Typography variant="h4" component="h1" fontWeight={600} gutterBottom>Shopping Cart</Typography>
        <Grid container spacing={2}>
          {[...Array(3)].map((_, i) => (
            <Grid item xs={12} key={i}>
              <Skeleton variant="rectangular" height={120} />
            </Grid>
          ))}
        </Grid>
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ mb: 3 }}>
        {error}
      </Alert>
    );
  }

  const items = cart?.items || [];
  const isEmpty = items.length === 0;

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4, flexWrap: 'wrap', gap: 2 }}>
        <Typography variant="h4" component="h1" fontWeight={600}>Shopping Cart</Typography>
        {items.length > 0 && (
          <Button variant="outlined" startIcon={<Delete />} onClick={handleClearCart} color="error">
            Clear Cart
          </Button>
        )}
      </Box>

      {isEmpty ? (
        <Paper elevation={0} variant="outlined" sx={{ p: 6, textAlign: 'center' }}>
          <Delete sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
          <Typography variant="h5" fontWeight={600} gutterBottom>Your cart is empty</Typography>
          <Typography color="text.secondary" paragraph>Looks like you haven't added any products yet.</Typography>
          <Button variant="contained" size="large" component={Link} to="/products" sx={{ mt: 2 }}>
            Continue Shopping
          </Button>
        </Paper>
      ) : (
        <>
          <Grid container spacing={3}>
            {/* Cart Items */}
            <Grid item xs={12} lg={8}>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                {items.map((item) => (
                  <Card key={item.id} variant="outlined" sx={{ display: 'flex', gap: 2, p: 2 }}>
                    <CardMedia
                      component="img"
                      image={item.productThumbnail}
                      alt={item.productName}
                      sx={{ width: 100, height: 100, objectFit: 'cover', borderRadius: 1, flexShrink: 0 }}
                    />
                    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                        <Typography variant="subtitle1" fontWeight={600} noWrap>{item.productName}</Typography>
                        <IconButton size="small" onClick={() => handleRemoveItem(item.id)} aria-label="Remove item">
                          <Delete fontSize="small" color="error" />
                        </IconButton>
                      </Box>
                      
                      {Object.keys(item.attributes).length > 0 && (
                        <Typography variant="caption" color="text.secondary" sx={{ mb: 1 }}>
                          {Object.entries(item.attributes).map(([k, v]) => `${k}: ${v}`).join(', ')}
                        </Typography>
                      )}

                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 'auto' }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', border: 1, borderColor: 'divider', borderRadius: 1 }}>
                          <IconButton size="small" onClick={() => handleQuantityChange(item.id, item.quantity - 1)} disabled={item.quantity <= 1}>
                            <Remove fontSize="small" />
                          </IconButton>
                          <TextField
                            value={item.quantity}
                            onChange={(e) => handleQuantityChange(item.id, Number(e.target.value) || 1)}
                            inputProps={{ style: { width: 50, textAlign: 'center', border: 'none', outline: 'none' }, disabled: isSyncing }}
                            size="small"
                            variant="standard"
                          />
                          <IconButton size="small" onClick={() => handleQuantityChange(item.id, item.quantity + 1)} disabled={item.quantity >= item.maxQuantity}>
                            <Add fontSize="small" />
                          </IconButton>
                        </Box>
                        <Typography variant="h6" fontWeight={700} color="primary.main">
                          {formatCurrency(item.subtotal, item.currency)}
                        </Typography>
                      </Box>
                    </Box>
                  </Card>
                ))}
              </Box>
            </Grid>

            {/* Order Summary */}
            <Grid item xs={12} lg={4}>
              <Paper elevation={0} variant="outlined" sx={{ p: 3, position: 'sticky', top: 100, height: 'fit-content' }}>
                <Typography variant="h6" fontWeight={600} gutterBottom>Order Summary</Typography>
                <Divider sx={{ mb: 2 }} />

                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="text.secondary">Subtotal ({cart?.itemCount || 0} items)</Typography>
                    <Typography variant="body2" fontWeight={500}>{formatCurrency(cart?.subtotal || 0, cart?.currency || 'USD')}</Typography>
                  </Box>

                  {cart && cart.items.length !== 0 && (
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <LocalOffer fontSize="small" />
                        <Typography variant="body2" color="success.main">Promo applied</Typography>
                      </Box>
                      <Typography variant="body2" color="success.main" fontWeight={500}>
                        -{formatCurrency(cart.subtotal * 0.1, cart.currency || 'USD')}
                      </Typography>
                    </Box>
                  )}

                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="text.secondary">Estimated Shipping</Typography>
                    <Typography variant="body2" fontWeight={500}>
                      {(cart?.subtotal || 0) >= 5000 ? 'Free' : formatCurrency(999, cart?.currency || 'USD')}
                    </Typography>
                  </Box>

                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="text.secondary">Estimated Tax</Typography>
                    <Typography variant="body2" fontWeight={500}>
                      {formatCurrency(Math.round((cart?.subtotal || 0) * 0.08), cart?.currency || 'USD')}
                    </Typography>
                  </Box>

                  <Divider sx={{ my: 1 }} />

                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="h6" fontWeight={700}>Order Total</Typography>
                    <Typography variant="h6" fontWeight={700} color="primary.main">
                      {formatCurrency(cart?.total || 0, cart?.currency || 'USD')}
                    </Typography>
                  </Box>
                </Box>

                {/* Promo Code Input */}
                <Divider sx={{ my: 3 }} />
                <Box sx={{ display: 'flex', gap: 1, mb: 3 }}>
                  <TextField
                    fullWidth
                    size="small"
                    placeholder="Promo code"
                    value={promoCode}
                    onChange={(e) => setPromoCode(e.target.value)}
                    InputProps={{
                      startAdornment: <LocalOffer color="action" />,
                    }}
                  />
                  <Button
                    variant="outlined"
                    size="small"
                    onClick={handleApplyPromo}
                    disabled={isSyncing || !promoCode.trim()}
              >
                Apply
              </Button>
                </Box>

                {cart && cart.items.length !== 0 && (
                  <Button variant="text" size="small" color="error" onClick={handleRemovePromo} fullWidth>
                    Remove promo code
                  </Button>
                )}

                <Divider sx={{ my: 3 }} />

                <Typography variant="body2" color="text.secondary" paragraph>
                  Shipping, taxes, and discounts calculated at checkout.
                </Typography>

                <Stack spacing={2} direction="column">
                  <Button
                    variant="contained"
                    size="large"
                    fullWidth
                    onClick={() => navigate('/checkout')}
                    disabled={isSyncing || isEmpty}
                    sx={{ py: 1.5 }}
                  >
                    Proceed to Checkout
                  </Button>
                  <Button variant="outlined" fullWidth onClick={() => navigate('/products')}>
                    Continue Shopping
                  </Button>
                </Stack>

                <Box sx={{ mt: 3, pt: 3, borderTop: 1, borderColor: 'divider', display: 'flex', flexDirection: 'column', gap: 1 }}>
                  <Typography variant="caption" color="text.secondary" display="flex" alignItems="center" gap={0.5}>
                    <LocalOffer fontSize="small" /> Secure checkout
                  </Typography>
                  <Typography variant="caption" color="text.secondary" display="flex" alignItems="center" gap={0.5}>
                    <LocalOffer fontSize="small" /> 30-day returns
                  </Typography>
                  <Typography variant="caption" color="text.secondary" display="flex" alignItems="center" gap={0.5}>
                    <LocalOffer fontSize="small" /> Free shipping on $50+
                  </Typography>
                </Box>
              </Paper>
            </Grid>
          </Grid>
        </>
      )}
    </Box>
  );
}