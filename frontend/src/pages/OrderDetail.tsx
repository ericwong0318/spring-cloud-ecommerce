import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Box, Typography, Card, CardContent, Button, Chip, CircularProgress, Alert, Divider, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Grid } from '@mui/material';
import { ArrowBack, Cancel, Refresh } from '@mui/icons-material';
import { orderApi } from '../services/endpoints';
import { formatCurrency, formatDate, formatOrderStatus, formatPaymentStatus } from '../utils/formatters';

export function OrderDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: order, isLoading, error, refetch } = useQuery({
    queryKey: ['order', id],
    queryFn: () => orderApi.getOrder(id!),
    enabled: !!id,
    staleTime: 1000 * 60 * 5,
  });

  const handleCancel = async () => {
    if (window.confirm('Are you sure you want to cancel this order?')) {
      try {
        await orderApi.cancelOrder(id!);
        refetch();
      } catch {
        // Error handling
      }
    }
  };

  const handleReturn = async () => {
    if (window.confirm('Are you sure you want to return this order?')) {
      try {
        await orderApi.returnOrder(id!, [], 'Change of mind');
        refetch();
      } catch {
        // Error handling
      }
    }
  };

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
        <Typography paragraph>We couldn't find this order.</Typography>
        <Button variant="contained" onClick={() => navigate('/account/orders')}>Back to Orders</Button>
      </Alert>
    );
  }

  const statusInfo = formatOrderStatus(order.status);
  const paymentInfo = formatPaymentStatus(order.paymentStatus);

  return (
    <Box>
      <Button startIcon={<ArrowBack />} onClick={() => navigate('/account/orders')} sx={{ mb: 3 }}>
        Back to Orders
      </Button>

      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexWrap: 'wrap', gap: 2 }}>
        <Typography variant="h4" component="h1" fontWeight={600}>
          Order #{order.orderNumber}
        </Typography>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Chip label={statusInfo.label} color={statusInfo.color as any} variant="filled" />
          <Chip label={paymentInfo.label} color={paymentInfo.color as any} variant="outlined" />
        </Box>
      </Box>

      <Grid container spacing={3}>
        {/* Status Timeline */}
        <Grid item xs={12} md={6}>
          <Card variant="outlined" sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" fontWeight={600} gutterBottom>Status Timeline</Typography>
              <Stack spacing={2} sx={{ py: 1 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Box sx={{ width: 12, height: 12, borderRadius: '50%', bgcolor: order.status !== 'PENDING' ? 'primary.main' : 'grey.400' }} />
                  <Typography variant="body2" fontWeight={500}>Order Placed</Typography>
                  <Typography variant="caption" color="text.secondary">{formatDate(order.createdAt)}</Typography>
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Box sx={{ width: 12, height: 12, borderRadius: '50%', bgcolor: ['CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED'].includes(order.status) ? 'primary.main' : 'grey.400' }} />
                  <Typography variant="body2" fontWeight={500}>Confirmed</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {order.status === 'PENDING' ? 'Pending confirmation' : formatDate(order.createdAt)}
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Box sx={{ width: 12, height: 12, borderRadius: '50%', bgcolor: ['SHIPPED', 'DELIVERED'].includes(order.status) ? 'primary.main' : 'grey.400' }} />
                  <Typography variant="body2" fontWeight={500}>Shipped</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {order.trackingNumber ? `Tracking: ${order.trackingNumber}` : 'Not yet shipped'}
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Box sx={{ width: 12, height: 12, borderRadius: '50%', bgcolor: order.status === 'DELIVERED' ? 'primary.main' : 'grey.400' }} />
                  <Typography variant="body2" fontWeight={500}>Delivered</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {order.status === 'DELIVERED' ? 'Delivered' : 'Pending'}
                  </Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        {/* Order Details */}
        <Grid item xs={12} md={6}>
          <Card variant="outlined" sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" fontWeight={600} gutterBottom>Order Details</Typography>
              <Stack spacing={1}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">Order Date</Typography>
                  <Typography variant="body2">{formatDate(order.createdAt)}</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">Payment Method</Typography>
                  <Typography variant="body2">{order.paymentMethod}</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">Estimated Delivery</Typography>
                  <Typography variant="body2">{order.estimatedDelivery ? formatDate(order.estimatedDelivery) : 'TBD'}</Typography>
                </Box>
                {order.trackingNumber && (
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="text.secondary">Tracking Number</Typography>
                    <Typography variant="body2" fontWeight={500}>{order.trackingNumber}</Typography>
                  </Box>
                )}
                {order.carrier && (
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="text.secondary">Carrier</Typography>
                    <Typography variant="body2">{order.carrier}</Typography>
                  </Box>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        {/* Items */}
        <Grid item xs={12}>
          <Card variant="outlined" sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" fontWeight={600} gutterBottom>Items</Typography>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Product</TableCell>
                      <TableCell>Price</TableCell>
                      <TableCell>Quantity</TableCell>
                      <TableCell>Subtotal</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {order.items.map((item: { id: string; productThumbnail: string; productName: string; quantity: number; subtotal: number; currency: string; price: number }) => (
                      <TableRow key={item.id}>
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Box
                              component="img"
                              src={item.productThumbnail}
                              alt={item.productName}
                              sx={{ width: 40, height: 40, objectFit: 'cover', borderRadius: 1 }}
                            />
                            <Typography variant="body2" fontWeight={500}>{item.productName}</Typography>
                          </Box>
                        </TableCell>
                        <TableCell>{formatCurrency(item.price, item.currency)}</TableCell>
                        <TableCell>{item.quantity}</TableCell>
                        <TableCell><Typography fontWeight={500}>{formatCurrency(item.subtotal, item.currency)}</Typography></TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Addresses */}
        <Grid item xs={12} md={6}>
          <Card variant="outlined" sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" fontWeight={600} gutterBottom>Shipping Address</Typography>
              <Typography variant="body2">{order.shippingAddress.firstName} {order.shippingAddress.lastName}</Typography>
              <Typography variant="body2">{order.shippingAddress.addressLine1}</Typography>
              {order.shippingAddress.addressLine2 && <Typography variant="body2">{order.shippingAddress.addressLine2}</Typography>}
              <Typography variant="body2">{order.shippingAddress.city}, {order.shippingAddress.state} {order.shippingAddress.postalCode}</Typography>
              <Typography variant="body2">{order.shippingAddress.country}</Typography>
              <Typography variant="body2" color="text.secondary">{order.shippingAddress.email}</Typography>
              {order.shippingAddress.phone && <Typography variant="body2" color="text.secondary">{order.shippingAddress.phone}</Typography>}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card variant="outlined" sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" fontWeight={600} gutterBottom>Billing Address</Typography>
              <Typography variant="body2">{order.billingAddress.firstName} {order.billingAddress.lastName}</Typography>
              <Typography variant="body2">{order.billingAddress.addressLine1}</Typography>
              {order.billingAddress.addressLine2 && <Typography variant="body2">{order.billingAddress.addressLine2}</Typography>}
              <Typography variant="body2">{order.billingAddress.city}, {order.billingAddress.state} {order.billingAddress.postalCode}</Typography>
              <Typography variant="body2">{order.billingAddress.country}</Typography>
              <Typography variant="body2" color="text.secondary">{order.billingAddress.email}</Typography>
              {order.billingAddress.phone && <Typography variant="body2" color="text.secondary">{order.billingAddress.phone}</Typography>}
            </CardContent>
          </Card>
        </Grid>

        {/* Pricing */}
        <Grid item xs={12} md={6}>
          <Card variant="outlined" sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" fontWeight={600} gutterBottom>Pricing</Typography>
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
                {order.discount > 0 && (
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2" color="text.secondary">Discount</Typography>
                    <Typography variant="body2" color="success.main">-{formatCurrency(order.discount, order.currency)}</Typography>
                  </Box>
                )}
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
        </Grid>
      </Grid>

      {/* Actions */}
      <Stack direction="row" spacing={2} justifyContent="flex-end" sx={{ mt: 3 }}>
        {order.status === 'PENDING' && (
          <>
            <Button variant="outlined" color="error" startIcon={<Cancel />} onClick={handleCancel}>
              Cancel Order
            </Button>
            <Button variant="outlined" startIcon={<Refresh />} onClick={handleReturn}>
              Return Items
            </Button>
          </>
        )}
        <Button variant="contained" startIcon={<ArrowBack />} onClick={() => navigate('/account/orders')}>
          Back to Orders
        </Button>
      </Stack>
    </Box>
  );
}