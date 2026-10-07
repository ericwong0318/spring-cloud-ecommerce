import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Box, Typography, Card, CardContent, CardActions, Button, Chip, Pagination, Skeleton, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Stack, Alert, IconButton } from '@mui/material';
import { Visibility, Cancel } from '@mui/icons-material';
import { orderApi } from '../services/endpoints';
import { formatCurrency, formatDate, formatOrderStatus } from '../utils/formatters';
import type { OrderStatus } from '../types/domain';

export function Orders() {
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<OrderStatus | 'ALL'>('ALL');

  const { data: ordersResponse, isLoading, error, refetch } = useQuery({
    queryKey: ['orders', page, statusFilter],
    queryFn: () => orderApi.getOrders(page, 10, statusFilter === 'ALL' ? undefined : statusFilter),
    placeholderData: (previousData) => previousData,
  });

  const orders = ordersResponse?.content || [];
  const totalPages = ordersResponse?.totalPages || 0;

  const handleCancelOrder = async (orderId: string) => {
    try {
      await orderApi.cancelOrder(orderId);
      refetch();
    } catch {
      // Error handling
    }
  };

  if (error) {
    return (
      <Alert severity="error" sx={{ mb: 3 }}>
        Failed to load orders. Please try again.
      </Alert>
    );
  }

  return (
    <Box>
      <Typography variant="h4" component="h1" fontWeight={600} gutterBottom>
        My Orders
      </Typography>

      {/* Status Filter Tabs */}
      <Box sx={{ mb: 3, borderBottom: 1, borderColor: 'divider' }}>
        <Stack direction="row" spacing={1} flexWrap="wrap">
          {['ALL', 'PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED', 'REFUNDED'].map((status) => (
            <Chip
              key={status}
              label={status === 'ALL' ? 'All Orders' : status}
              onClick={() => setStatusFilter(status as OrderStatus | 'ALL')}
              color={statusFilter === status ? 'primary' : 'default'}
              variant={statusFilter === status ? 'filled' : 'outlined'}
              size="small"
              sx={{ cursor: 'pointer' }}
            />
          ))}
        </Stack>
      </Box>

      {isLoading ? (
        <Stack spacing={2}>
          {[...Array(5)].map((_, i) => (
            <Skeleton key={i} variant="rectangular" height={80} />
          ))}
        </Stack>
      ) : orders.length === 0 ? (
        <Alert severity="info">
          <Typography variant="h6" gutterBottom>No orders found</Typography>
          <Typography paragraph>You haven't placed any orders yet.</Typography>
          <Button variant="contained" onClick={() => window.location.href = '/products'}>
            Start Shopping
          </Button>
        </Alert>
      ) : (
        <>
          {/* Mobile Cards */}
          <Stack spacing={2} sx={{ display: { xs: 'block', md: 'none' } }}>
            {orders.map((order: { id: string; orderNumber: string; status: string; paymentStatus: string; createdAt: string; total: number; currency: string }) => (
              <Card key={order.id} variant="outlined">
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="subtitle1" fontWeight={600}>#{order.orderNumber}</Typography>
                    <Chip label={formatOrderStatus(order.status).label} size="small" color={formatOrderStatus(order.status).color as any} />
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    {formatDate(order.createdAt)}
                  </Typography>
                  <Typography variant="body2" fontWeight={500} sx={{ mt: 1 }}>
                    {formatCurrency(order.total, order.currency)}
                  </Typography>
                </CardContent>
                <CardActions>
                  <Button size="small" onClick={() => window.location.href = `/account/orders/${order.id}`}>
                    View Details
                  </Button>
                  {order.status === 'PENDING' && (
                    <Button size="small" color="error" onClick={() => handleCancelOrder(order.id)}>
                      Cancel
                    </Button>
                  )}
                </CardActions>
              </Card>
            ))}
          </Stack>

          {/* Desktop Table */}
          <TableContainer component={Paper} sx={{ display: { xs: 'none', md: 'block' } }}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Order #</TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Payment</TableCell>
                  <TableCell>Total</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {orders.map((order: { id: string; orderNumber: string; status: string; paymentStatus: string; createdAt: string; total: number; currency: string }) => (
                  <TableRow key={order.id} hover>
                    <TableCell>
                      <Typography variant="body2" fontWeight={500}>{order.orderNumber}</Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {formatDate(order.createdAt)}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip label={formatOrderStatus(order.status).label} size="small" color={formatOrderStatus(order.status).color as any} variant="outlined" />
                    </TableCell>
                    <TableCell>
                      <Chip label={order.paymentStatus} size="small" variant="outlined" />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" fontWeight={500}>
                        {formatCurrency(order.total, order.currency)}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <IconButton size="small" onClick={() => window.location.href = `/account/orders/${order.id}`}>
                        <Visibility fontSize="small" />
                      </IconButton>
                      {order.status === 'PENDING' && (
                        <IconButton size="small" color="error" onClick={() => handleCancelOrder(order.id)}>
                          <Cancel fontSize="small" />
                        </IconButton>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>

          {/* Pagination */}
          {totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
              <Pagination
                count={totalPages}
                page={page + 1}
                onChange={(_, page) => setPage(page - 1)}
                color="primary"
                showFirstButton
                showLastButton
              />
            </Box>
          )}
        </>
      )}
    </Box>
  );
}