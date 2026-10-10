import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Grid, Typography, Stepper, Step, StepLabel, StepContent, Button, Paper, Divider, Alert, CircularProgress, Chip, TextField, FormControlLabel, Checkbox, Radio, RadioGroup, Card, CardContent, Stack } from '@mui/material';
import { KeyboardArrowLeft, KeyboardArrowRight } from '@mui/icons-material';
import { useQuery, useMutation } from '@tanstack/react-query';
import { orderApi, addressApi } from '@services/endpoints';
import { useCart } from '../hooks/useCart';
import { useAuth } from '../hooks/useAuth';
import { useNotification } from '../hooks/useNotification';
import { checkoutSchema, type CheckoutFormData } from '../utils/validators';
import { formatCurrency } from '../utils/formatters';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';



export function Checkout() {
  const navigate = useNavigate();
  const { cart, isSyncing } = useCart();
  const { isAuthenticated } = useAuth();
  const { showNotification } = useNotification();

  const [activeStep, setActiveStep] = useState(0);
  const [orderId, setOrderId] = useState<string | null>(null);
  const [paymentUrl, setPaymentUrl] = useState<string | null>(null);

  const methods = useForm<CheckoutFormData>({
    resolver: zodResolver(checkoutSchema),
    defaultValues: {
      shippingAddress: {
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        addressLine1: '',
        addressLine2: '',
        city: '',
        state: '',
        postalCode: '',
        country: 'US',
        isDefault: true,
        type: 'SHIPPING',
      },
      billingAddress: {
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        addressLine1: '',
        addressLine2: '',
        city: '',
        state: '',
        postalCode: '',
        country: 'US',
        type: 'BILLING',
      },
      paymentMethod: 'card',
      notes: '',
    },
  });

  const { setValue, handleSubmit } = methods;

  // Fetch user addresses
  const { data: addresses } = useQuery({
    queryKey: ['addresses'],
    queryFn: () => addressApi.getAddresses(),
    enabled: isAuthenticated,
  });

  // Create order mutation
  const createOrderMutation = useMutation({
    mutationFn: (data: CheckoutFormData) => orderApi.createOrder(data),
    onSuccess: (data) => {
      setOrderId(data.orderId);
      setPaymentUrl(data.paymentUrl ?? null);
      setActiveStep(3); // Confirm step
    },
    onError: (error) => {
      showNotification({ type: 'error', message: 'Failed to create order' });
      console.error('Order creation error:', error);
    },
  });

  const onSubmit = (data: CheckoutFormData) => {
    createOrderMutation.mutate(data);
  };

  const handleNext = () => {
    handleSubmit(onSubmit)();
    setActiveStep((prev) => prev + 1);
  };

  const handleBack = () => {
    setActiveStep((prev) => prev - 1);
  };

  const handleAddressSelect = (address: unknown, type: 'SHIPPING' | 'BILLING') => {
    const { id, isDefault, ...addressData } = address as { id?: string; isDefault?: boolean; [key: string]: unknown };
    setValue(type === 'SHIPPING' ? 'shippingAddress' : 'billingAddress', addressData as CheckoutFormData['shippingAddress']);
  };

  if (!isAuthenticated) {
    return (
      <Box sx={{ textAlign: 'center', py: 6 }}>
        <Alert severity="info" sx={{ maxWidth: 400, mx: 'auto' }}>
          <Typography variant="h6" gutterBottom>Please sign in</Typography>
          <Typography variant="body1" paragraph>You need to be signed in to checkout.</Typography>
          <Button variant="contained" onClick={() => navigate('/login')}>
            Sign In
          </Button>
        </Alert>
      </Box>
    );
  }

  if (isSyncing || !cart) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (cart.items.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 6 }}>
        <Typography variant="h5" gutterBottom>Your cart is empty</Typography>
        <Typography color="text.secondary" paragraph>Add some products before checking out.</Typography>
        <Button variant="contained" size="large" onClick={() => navigate('/products')}>
          Continue Shopping
        </Button>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h3" component="h1" fontWeight={700} gutterBottom>
        Checkout
      </Typography>

      <Stepper activeStep={activeStep} orientation="vertical" sx={{ py: 3 }}>
        {/* Shipping Step */}
        <Step key="shipping">
          <StepLabel>Shipping Address</StepLabel>
          <StepContent>
            <ShippingForm
              methods={methods}
              addresses={addresses}
              onAddressSelect={handleAddressSelect}
              onNext={handleNext}
              onBack={handleBack}
              loading={createOrderMutation.isPending}
            />
          </StepContent>
        </Step>

        {/* Payment Step */}
        <Step key="payment">
          <StepLabel>Payment Method</StepLabel>
          <StepContent>
            <PaymentForm
              onNext={handleNext}
              onBack={handleBack}
              loading={createOrderMutation.isPending}
            />
          </StepContent>
        </Step>

        {/* Review Step */}
        <Step key="review">
          <StepLabel>Review Order</StepLabel>
          <StepContent>
            <ReviewOrder
              methods={methods}
              onNext={handleNext}
              onBack={handleBack}
              loading={createOrderMutation.isPending}
            />
          </StepContent>
        </Step>

        {/* Confirm Step */}
        <Step key="confirm">
          <StepLabel>Order Confirmed</StepLabel>
          <StepContent>
            <ConfirmOrder
              orderId={orderId}
              paymentUrl={paymentUrl}
              onBack={handleBack}
            />
          </StepContent>
        </Step>
      </Stepper>
    </Box>
  );
}

function ShippingForm({ methods, addresses, onAddressSelect, onNext, onBack, loading }: {
  methods: ReturnType<typeof useForm<CheckoutFormData>>;
  addresses: unknown;
  onAddressSelect: (address: unknown, type: 'SHIPPING' | 'BILLING') => void;
  onNext: () => void;
  onBack: () => void;
  loading: boolean;
}) {
  const { register, formState: { errors }, handleSubmit } = methods;
  const [sameAsShipping, setSameAsShipping] = useState(true);

  const handleShippingSubmit = handleSubmit(() => {
    if (!sameAsShipping) {
      onNext();
    } else {
      // If same as shipping, skip to payment step
      onNext();
    }
  });

  return (
    <form onSubmit={handleShippingSubmit}>
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <TextField
            label="First Name"
            {...register('shippingAddress.firstName')}
            error={!!errors.shippingAddress?.firstName}
            helperText={errors.shippingAddress?.firstName?.message}
            fullWidth
            required
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            label="Last Name"
            {...register('shippingAddress.lastName')}
            error={!!errors.shippingAddress?.lastName}
            helperText={errors.shippingAddress?.lastName?.message}
            fullWidth
            required
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            label="Email"
            type="email"
            {...register('shippingAddress.email')}
            error={!!errors.shippingAddress?.email}
            helperText={errors.shippingAddress?.email?.message}
            fullWidth
            required
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            label="Phone"
            {...register('shippingAddress.phone')}
            error={!!errors.shippingAddress?.phone}
            helperText={errors.shippingAddress?.phone?.message}
            fullWidth
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            label="Address Line 1"
            {...register('shippingAddress.addressLine1')}
            error={!!errors.shippingAddress?.addressLine1}
            helperText={errors.shippingAddress?.addressLine1?.message}
            fullWidth
            required
          />
        </Grid>
        <Grid item xs={12}>
          <TextField
            label="Address Line 2 (optional)"
            {...register('shippingAddress.addressLine2')}
            error={!!errors.shippingAddress?.addressLine2}
            helperText={errors.shippingAddress?.addressLine2?.message}
            fullWidth
          />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField
            label="City"
            {...register('shippingAddress.city')}
            error={!!errors.shippingAddress?.city}
            helperText={errors.shippingAddress?.city?.message}
            fullWidth
            required
          />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField
            label="State"
            {...register('shippingAddress.state')}
            error={!!errors.shippingAddress?.state}
            helperText={errors.shippingAddress?.state?.message}
            fullWidth
          />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField
            label="Postal Code"
            {...register('shippingAddress.postalCode')}
            error={!!errors.shippingAddress?.postalCode}
            helperText={errors.shippingAddress?.postalCode?.message}
            fullWidth
            required
          />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField
            label="Country"
            {...register('shippingAddress.country')}
            error={!!errors.shippingAddress?.country}
            helperText={errors.shippingAddress?.country?.message}
            fullWidth
            required
          />
        </Grid>
      </Grid>

      <Divider sx={{ my: 3 }} />

      {/* Existing Addresses */}
      {addresses != null && (addresses as unknown as Array<{ id: string }>).length > 0 && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>Saved Addresses</Typography>
          <Grid container spacing={2}>
            {(addresses as Array<{ id: string; firstName: string; lastName: string; email: string; addressLine1: string; city: string; state?: string; postalCode: string; country: string; type?: string }>).map((address) => (
              <Grid item xs={12} sm={6} key={address.id}>
                <Card
                  variant="outlined"
                  sx={{
                    cursor: 'pointer',
                    borderColor: sameAsShipping ? 'primary.main' : 'divider',
                    '&:hover': { borderColor: 'primary.main' },
                  }}
                  onClick={() => onAddressSelect(address, 'SHIPPING')}
                >
                  <CardContent sx={{ py: 1.5 }}>
                    <Typography variant="body2" fontWeight={500}>
                      {address.firstName} {address.lastName}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {address.addressLine1}, {address.city}, {address.state} {address.postalCode}
                    </Typography>
                    {address.type === 'BILLING' && <Chip label="Billing" size="small" variant="outlined" sx={{ ml: 1 }} />}
                    {address.type === 'SHIPPING' && <Chip label="Shipping" size="small" color="primary" sx={{ ml: 1 }} />}
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Box>
      )}

      <Divider sx={{ my: 3 }} />

      {/* Billing Address */}
      <FormControlLabel
        control={<Checkbox checked={sameAsShipping} onChange={(e) => setSameAsShipping(e.target.checked)} />}
        label="Billing address same as shipping"
        sx={{ mb: 2 }}
      />

      {!sameAsShipping && (
        <BillingForm methods={methods} />
      )}

      <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
        <Button variant="outlined" onClick={onBack} startIcon={<KeyboardArrowLeft />}>
          Back to Cart
        </Button>
        <Button type="submit" variant="contained" disabled={loading} endIcon={<KeyboardArrowRight />}>
          {loading ? 'Processing...' : 'Continue to Payment'}
        </Button>
      </Stack>
    </form>
  );
}

function BillingForm({ methods }: { methods: ReturnType<typeof useForm<CheckoutFormData>> }) {
  const { register } = methods;

  return (
    <Box sx={{ mt: 2 }}>
      <Typography variant="subtitle1" fontWeight={600} gutterBottom>Billing Address</Typography>
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <TextField label="First Name" {...register('billingAddress.firstName')} fullWidth required />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField label="Last Name" {...register('billingAddress.lastName')} fullWidth required />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField label="Email" type="email" {...register('billingAddress.email')} fullWidth required />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField label="Phone" {...register('billingAddress.phone')} fullWidth />
        </Grid>
        <Grid item xs={12}>
          <TextField label="Address Line 1" {...register('billingAddress.addressLine1')} fullWidth required />
        </Grid>
        <Grid item xs={12}>
          <TextField label="Address Line 2 (optional)" {...register('billingAddress.addressLine2')} fullWidth />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField label="City" {...register('billingAddress.city')} fullWidth required />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField label="State" {...register('billingAddress.state')} fullWidth />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField label="Postal Code" {...register('billingAddress.postalCode')} fullWidth required />
        </Grid>
        <Grid item xs={12} md={4}>
          <TextField label="Country" {...register('billingAddress.country')} fullWidth required />
        </Grid>
      </Grid>
    </Box>
  );
}

function PaymentForm({ onNext, onBack, loading }: {
  onNext: () => void;
  onBack: () => void;
  loading: boolean;
}) {
  const [paymentMethod, setPaymentMethod] = useState<'card' | 'paypal'>('card');

  const handlePaymentMethodChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPaymentMethod(e.target.value as 'card' | 'paypal');
  };

  const handleContinue = () => {
    onNext();
  };

  return (
    <Box>
      <Typography variant="subtitle1" fontWeight={600} gutterBottom>Payment Method</Typography>
      <Divider sx={{ mb: 3 }} />

      <RadioGroup value={paymentMethod} onChange={handlePaymentMethodChange}>
        <FormControlLabel value="card" control={<Radio />} label="Credit/Debit Card" />
        <FormControlLabel value="paypal" control={<Radio />} label="PayPal" />
      </RadioGroup>

      {paymentMethod === 'card' && (
        <Alert severity="info" sx={{ mt: 2 }}>
          <Typography variant="body2">
            Payment processing is handled securely by our payment provider.
            Your card details are encrypted and never stored on our servers.
          </Typography>
        </Alert>
      )}

      <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
        <Button variant="outlined" onClick={onBack} startIcon={<KeyboardArrowLeft />}>
          Back to Shipping
        </Button>
        <Button variant="contained" onClick={handleContinue} disabled={loading}>
          Review Order
        </Button>
      </Stack>
    </Box>
  );
}

function ReviewOrder({ methods, onNext, onBack, loading }: {
  methods: ReturnType<typeof useForm<CheckoutFormData>>;
  onNext: () => void;
  onBack: () => void;
  loading: boolean;
}) {
  const { getValues } = methods;
  const { cart } = useCart();
  const watchShipping = getValues('shippingAddress');
  const watchBilling = getValues('billingAddress') || { firstName: '', lastName: '', addressLine1: '', addressLine2: '', city: '', state: '', postalCode: '', country: 'US' };
  const watchPaymentMethod = getValues('paymentMethod');

  const handleConfirm = () => {
    onNext();
  };

  return (
    <Box>
      <Typography variant="subtitle1" fontWeight={600} gutterBottom>Review Your Order</Typography>
      <Divider sx={{ mb: 3 }} />

      <Grid container spacing={3}>
        {/* Shipping Summary */}
        <Grid item xs={12} md={6}>
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>Shipping Address</Typography>
            <Typography variant="body2">
              {watchShipping.firstName} {watchShipping.lastName}
            </Typography>
            <Typography variant="body2">{watchShipping.addressLine1}</Typography>
            {watchShipping.addressLine2 && <Typography variant="body2">{watchShipping.addressLine2}</Typography>}
            <Typography variant="body2">{watchShipping.city}, {watchShipping.state} {watchShipping.postalCode}</Typography>
            <Typography variant="body2">{watchShipping.country}</Typography>
            <Typography variant="body2" color="text.secondary">{watchShipping.email}</Typography>
            {watchShipping.phone && <Typography variant="body2" color="text.secondary">{watchShipping.phone}</Typography>}
          </Paper>
        </Grid>

        {/* Billing Summary */}
        <Grid item xs={12} md={6}>
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>Billing Address</Typography>
            <Typography variant="body2">
              {watchBilling.firstName} {watchBilling.lastName}
            </Typography>
            <Typography variant="body2">{watchBilling.addressLine1}</Typography>
            {watchBilling.addressLine2 && <Typography variant="body2">{watchBilling.addressLine2}</Typography>}
            <Typography variant="body2">{watchBilling.city}, {watchBilling.state} {watchBilling.postalCode}</Typography>
            <Typography variant="body2">{watchBilling.country}</Typography>
          </Paper>
        </Grid>

        {/* Payment Method */}
        <Grid item xs={12}>
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>Payment Method</Typography>
            <Chip label={watchPaymentMethod === 'card' ? 'Credit/Debit Card' : 'PayPal'} color="primary" variant="outlined" />
          </Paper>
        </Grid>

        {/* Order Items */}
        <Grid item xs={12}>
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>Order Items</Typography>
            <Stack spacing={1}>
              {cart?.items.map((item) => (
                <Box key={item.id} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography variant="body2">{item.productName} x {item.quantity}</Typography>
                  <Typography variant="body2" fontWeight={500}>{formatCurrency(item.subtotal, item.currency)}</Typography>
                </Box>
              ))}
            </Stack>
          </Paper>
        </Grid>

        {/* Order Total */}
        <Grid item xs={12}>
          <Paper variant="outlined" sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
              <Typography>Subtotal</Typography>
              <Typography fontWeight={500}>{formatCurrency(cart?.subtotal || 0, cart?.currency || 'USD')}</Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
              <Typography>Shipping</Typography>
              <Typography fontWeight={500}>{(cart?.subtotal || 0) >= 5000 ? 'Free' : formatCurrency(999, cart?.currency || 'USD')}</Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography fontWeight={600}>Total</Typography>
              <Typography variant="h6" fontWeight={700} color="primary.main">
                {formatCurrency(cart?.total || 0, cart?.currency || 'USD')}
              </Typography>
            </Box>
          </Paper>
        </Grid>
      </Grid>

      <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
        <Button variant="outlined" onClick={onBack} startIcon={<KeyboardArrowLeft />}>
          Back to Payment
        </Button>
        <Button variant="contained" onClick={handleConfirm} disabled={loading}>
          {loading ? 'Placing Order...' : 'Place Order'}
        </Button>
      </Stack>
    </Box>
  );
}

function ConfirmOrder({ orderId, paymentUrl }: {
  orderId: string | null;
  paymentUrl: string | null;
  onBack?: () => void;
}) {
  const navigate = useNavigate();

  const handleContinueShopping = () => {
    navigate('/');
  };

  const handleViewOrders = () => {
    navigate('/account/orders');
  };

  return (
    <Box sx={{ textAlign: 'center', py: 4 }}>
      <Alert severity="success" sx={{ maxWidth: 500, mx: 'auto', mb: 3 }}>
        <Typography variant="h5" fontWeight={600} gutterBottom>
          Order Placed Successfully!
        </Typography>
        <Typography variant="body1" paragraph>
          Your order has been placed and is being processed.
        </Typography>
        {orderId && (
          <Typography variant="body2" color="text.secondary">
            Order ID: <strong>{orderId}</strong>
          </Typography>
        )}
      </Alert>

      {paymentUrl && (
        <Alert severity="info" sx={{ maxWidth: 500, mx: 'auto', mb: 3 }}>
          <Typography variant="body2" paragraph>
            You will be redirected to complete your payment.
          </Typography>
          <Button variant="contained" href={paymentUrl} target="_blank" rel="noopener noreferrer">
            Complete Payment
          </Button>
        </Alert>
      )}

      <Stack direction="row" spacing={2} justifyContent="center">
        <Button variant="contained" onClick={handleContinueShopping}>
          Continue Shopping
        </Button>
        <Button variant="outlined" onClick={handleViewOrders}>
          View My Orders
        </Button>
      </Stack>
    </Box>
  );
}