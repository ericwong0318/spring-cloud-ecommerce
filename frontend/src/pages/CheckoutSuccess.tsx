import { useParams, useNavigate } from 'react-router-dom';
import { Box, Typography, Button, Card, CardContent, Chip, CircularProgress, Alert, Divider, Stack, Grid } from '@mui/material';
import { CheckCircle, LocalShipping, Payment, Schedule, Home } from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { orderApi } from '../services/endpoints';
import { formatCurrency, formatDate, formatOrderStatus, formatPaymentStatus } from '../utils/formatters';

export function CheckoutSuccess() {
  const { orderId } = useParams<{ orderId: string }>();
  const navigate = useNavigate();

  const { data: order, isLoading, error } = useQuery({
    queryKey: ['order', orderId],
    queryFn: () => orderApi.getOrder(orderId!),
    enabled: !!orderId,
    staleTime: 1000 * 60 * 5,
  });

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !order) {
    return (
      <Alert severity="error" sx={{ mb: 3 }}>
        <Typography variant="h6" gutterBottom>Order Not Found</Typography>
        <Typography paragraph>We couldn't find your order. It may have been cancelled or the link may be invalid.</Typography>
        <Button variant="contained" onClick={() => navigate('/')}>Back to Home</Button>
      </Alert>
    );
  }

  const statusInfo = formatOrderStatus(order.status);
  const paymentInfo = formatPaymentStatus(order.paymentStatus);

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto' }}>
      {/* Success Header */}
      <Box sx={{ textAlign: 'center', py: 4 }}>
        <CheckCircle sx={{ fontSize: 72, color: 'success.main', mb: 2 }} />
        <Typography variant="h3" component="h1" fontWeight={700} gutterBottom>
          Order Confirmed!
        </Typography>
        <Typography variant="body1" color="text.secondary" paragraph>
          Thank you for your order. We've sent a confirmation email to {order.shippingAddress.email}.
        </Typography>
        <Chip label={`Order #${order.orderNumber}`} size="small" variant="outlined" />
      </Box>

      <Divider sx={{ my: 4 }} />

      {/* Order Summary */}
      <Card variant="outlined" sx={{ mb: 4 }}>
        <CardContent>
          <Typography variant="h5" fontWeight={600} gutterBottom>Order Summary</Typography>
          <Grid container spacing={3}>
            <Grid item xs={12} md={4}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <Schedule color="action" />
                <Typography variant="body2" color="text.secondary">Date</Typography>
              </Box>
              <Typography variant="body1" fontWeight={500}>
                {formatDate(order.createdAt)}
              </Typography>
            </Grid>
            <Grid item xs={12} md={4}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <Payment color="action" />
                <Typography variant="body2" color="text.secondary">Payment</Typography>
              </Box>
              <Chip label={paymentInfo.label} size="small" color={paymentInfo.color as any} variant="outlined" />
            </Grid>
            <Grid item xs={12} md={4}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <LocalShipping color="action" />
                <Typography variant="body2" color="text.secondary">Status</Typography>
              </Box>
              <Chip label={statusInfo.label} size="small" color={statusInfo.color as any} variant="outlined" />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Items */}
      <Card variant="outlined" sx={{ mb: 4 }}>
        <CardContent>
          <Typography variant="h6" fontWeight={600} gutterBottom>Items</Typography>
          <Stack spacing={2}>
            {order.items.map((item: { id: string; productThumbnail: string; productName: string; quantity: number; subtotal: number; currency: string }) => (
              <Box key={item.id} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Box
                    component="img"
                    src={item.productThumbnail}
                    alt={item.productName}
                    sx={{ width: 60, height: 60, objectFit: 'cover', borderRadius: 1 }}
                  />
                  <Box>
                    <Typography variant="body1" fontWeight={500}>{item.productName}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      Qty: {item.quantity}
                    </Typography>
                  </Box>
                </Box>
                <Typography variant="body1" fontWeight={500}>
                  {formatCurrency(item.subtotal, item.currency)}
                </Typography>
              </Box>
            ))}
          </Stack>
        </CardContent>
      </Card>

      {/* Pricing */}
      <Card variant="outlined" sx={{ mb: 4 }}>
        <CardContent>
          <Stack spacing={1}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2" color="text.secondary">Subtotal</Typography>
              <Typography variant="body2">{formatCurrency(order.subtotal, order.currency)}</Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2" color="text.secondary">Tax</Typography>
              <Typography variant="body2">{formatCurrency(order.tax, order.currency)}</Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2" color="text.secondary">Shipping</Typography>
              <Typography variant="body2">{formatCurrency(order.shipping, order.currency)}</Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2" color="text.secondary">Discount</Typography>
              <Typography variant="body2" color="success.main">-{formatCurrency(order.discount, order.currency)}</Typography>
            </Box>
            <Divider />
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="h6" fontWeight={600}>Total</Typography>
              <Typography variant="h6" fontWeight={600} color="primary.main">
                {formatCurrency(order.total, order.currency)}
              </Typography>
            </Box>
          </Stack>
        </CardContent>
      </Card>

      {/* Tracking */}
      {order.trackingNumber && (
        <Card variant="outlined" sx={{ mb: 4 }}>
          <CardContent>
            <Typography variant="h6" fontWeight={600} gutterBottom>Tracking</Typography>
            <Stack spacing={1}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2" color="text.secondary">Carrier</Typography>
                <Typography variant="body2" fontWeight={500}>{order.carrier}</Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2" color="text.secondary">Tracking Number</Typography>
                <Typography variant="body2" fontWeight={500}>{order.trackingNumber}</Typography>
              </Box>
            </Stack>
          </CardContent>
        </Card>
      )}

      {/* Estimated Delivery */}
      {order.estimatedDelivery && (
        <Alert severity="info" sx={{ mb: 4 }}>
          <Typography variant="body2">
            <strong>Estimated Delivery:</strong> {formatDate(order.estimatedDelivery)}
          </Typography>
        </Alert>
      )}

      {/* Actions */}
      <Stack direction="row" spacing={2} justifyContent="center" flexWrap="wrap">
        <Button variant="contained" startIcon={<Home />} onClick={() => navigate('/')}>
          Continue Shopping
        </Button>
        <Button variant="outlined" onClick={() => navigate('/account/orders')}>
          View My Orders
        </Button>
      </Stack>
    </Box>
  );
}