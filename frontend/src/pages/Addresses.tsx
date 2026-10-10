import { useState, useEffect, useCallback } from 'react';
import {
  Box, Button, Typography, Paper, TextField, Dialog, DialogTitle, DialogContent,
  DialogActions, Grid, IconButton, Alert, AlertTitle, Chip, List, ListItem,
  ListItemText, ListItemSecondaryAction, Divider, FormControl, InputLabel, Select,
  MenuItem, Radio, RadioGroup, FormControlLabel, Snackbar
} from '@mui/material';
import { Add, Edit, Delete, CheckCircle, HomeWork, LocationOn, Close } from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@context/AuthContext';
import { useNotification } from '@context/NotificationContext';
import { addressApi } from '@services/endpoints';
import type { Address } from '@types/domain';

const addressSchema = z.object({
  firstName: z.string().min(1, 'First name is required'),
  lastName: z.string().min(1, 'Last name is required'),
  company: z.string().optional(),
  addressLine1: z.string().min(1, 'Address line 1 is required'),
  addressLine2: z.string().optional(),
  city: z.string().min(1, 'City is required'),
  state: z.string().min(1, 'State/Province is required'),
  postalCode: z.string().min(1, 'Postal code is required'),
  country: z.string().min(1, 'Country is required'),
  phone: z.string().optional(),
  type: z.enum(['billing', 'shipping', 'both']),
  isDefault: z.boolean().default(false),
});

type AddressFormData = z.infer<typeof addressSchema>;

const COUNTRIES = [
  { code: 'US', name: 'United States' },
  { code: 'CA', name: 'Canada' },
  { code: 'GB', name: 'United Kingdom' },
  { code: 'AU', name: 'Australia' },
  { code: 'DE', name: 'Germany' },
  { code: 'FR', name: 'France' },
  { code: 'JP', name: 'Japan' },
];

const US_STATES = [
  'AL', 'AK', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'FL', 'GA',
  'HI', 'ID', 'IL', 'IN', 'IA', 'KS', 'KY', 'LA', 'ME', 'MD',
  'MA', 'MI', 'MN', 'MS', 'MO', 'MT', 'NE', 'NV', 'NH', 'NJ',
  'NM', 'NY', 'NC', 'ND', 'OH', 'OK', 'OR', 'PA', 'RI', 'SC',
  'SD', 'TN', 'TX', 'UT', 'VT', 'VA', 'WA', 'WV', 'WI', 'WY'
];

export function Addresses() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const { showNotification } = useNotification();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingAddress, setEditingAddress] = useState<Address | null>(null);
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);

  const { data: addresses, isLoading, error } = useQuery({
    queryKey: ['addresses'],
    queryFn: addressApi.getAddresses,
    enabled: !!user,
  });

  const createMutation = useMutation({
    mutationFn: (data: AddressFormData) => addressApi.createAddress(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['addresses'] });
      showNotification('success', 'Address added successfully');
      closeDialog();
    },
    onError: () => showNotification('error', 'Failed to add address'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<AddressFormData> }) =>
      addressApi.updateAddress(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['addresses'] });
      showNotification('success', 'Address updated successfully');
      closeDialog();
    },
    onError: () => showNotification('error', 'Failed to update address'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => addressApi.deleteAddress(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['addresses'] });
      showNotification('success', 'Address deleted successfully');
      setDeleteConfirm(null);
    },
    onError: () => showNotification('error', 'Failed to delete address'),
  });

  const setDefaultMutation = useMutation({
    mutationFn: (id: string) => addressApi.setDefaultAddress(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['addresses'] });
      showNotification('success', 'Default address updated');
    },
    onError: () => showNotification('error', 'Failed to update default address'),
  });

  const { register, handleSubmit, reset, setValue, formState: { errors } } = useForm<AddressFormData>({
    resolver: zodResolver(addressSchema),
    defaultValues: {
      type: 'shipping',
      isDefault: false,
      country: 'US',
    },
  });

  const openDialog = useCallback((address?: Address) => {
    if (address) {
      setEditingAddress(address);
      reset({
        firstName: address.firstName,
        lastName: address.lastName,
        company: address.company || '',
        addressLine1: address.addressLine1,
        addressLine2: address.addressLine2 || '',
        city: address.city,
        state: address.state,
        postalCode: address.postalCode,
        country: address.country,
        phone: address.phone || '',
        type: address.type,
        isDefault: address.isDefault,
      });
    } else {
      setEditingAddress(null);
      reset({
        type: 'shipping',
        isDefault: false,
        country: 'US',
      });
    }
    setDialogOpen(true);
  }, [reset]);

  const closeDialog = () => {
    setDialogOpen(false);
    setEditingAddress(null);
    reset();
  };

  const handleDeleteClick = (id: string) => {
    setDeleteConfirm(id);
  };

  const confirmDelete = () => {
    if (deleteConfirm) {
      deleteMutation.mutate(deleteConfirm);
    }
  };

  const handleSubmitForm = (data: AddressFormData) => {
    if (editingAddress) {
      updateMutation.mutate({ id: editingAddress.id, data });
    } else {
      createMutation.mutate(data);
    }
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <Typography>Loading addresses...</Typography>
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ mx: 'auto', maxWidth: 800, mb: 3 }}>
        <AlertTitle>Error</AlertTitle>
        Failed to load addresses. Please try again.
      </Alert>
    );
  }

  const billingAddresses = addresses?.filter(a => a.type === 'billing' || a.type === 'both') || [];
  const shippingAddresses = addresses?.filter(a => a.type === 'shipping' || a.type === 'both') || [];

  return (
    <Box sx={{ maxWidth: 900, mx: 'auto', py: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Typography variant="h4" fontWeight={600}>My Addresses</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => openDialog()}>
          Add Address
        </Button>
      </Box>

      {(addresses?.length === 0 || !addresses) && (
        <Paper elevation={1} sx={{ p: 6, textAlign: 'center' }}>
          <LocationOn sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" fontWeight={600} gutterBottom>
            No addresses saved
          </Typography>
          <Typography color="text.secondary" paragraph>
            Add your first address to speed up checkout.
          </Typography>
          <Button variant="contained" startIcon={<Add />} onClick={() => openDialog()} sx={{ mt: 2 }}>
            Add Address
          </Button>
        </Paper>
      )}

      {addresses && addresses.length > 0 && (
        <>
          {(billingAddresses.length > 0) && (
            <Box sx={{ mb: 4 }}>
              <Typography variant="h6" fontWeight={600} gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <HomeWork sx={{ fontSize: 24 }} /> Billing Addresses
              </Typography>
              <List>
                {billingAddresses.map((address) => (
                  <AddressCard
                    key={address.id}
                    address={address}
                    onEdit={openDialog}
                    onDelete={handleDeleteClick}
                    onSetDefault={setDefaultMutation.mutate}
                    isDefault={address.isDefault && address.type !== 'shipping'}
                  />
                ))}
              </List>
            </Box>
          )}

          {(shippingAddresses.length > 0) && (
            <Box>
              <Typography variant="h6" fontWeight={600} gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <LocationOn sx={{ fontSize: 24 }} /> Shipping Addresses
              </Typography>
              <List>
                {shippingAddresses.map((address) => (
                  <AddressCard
                    key={address.id}
                    address={address}
                    onEdit={openDialog}
                    onDelete={handleDeleteClick}
                    onSetDefault={setDefaultMutation.mutate}
                    isDefault={address.isDefault && address.type !== 'billing'}
                  />
                ))}
              </List>
            </Box>
          )}
        </>
      )}

      <AddressDialog
        open={dialogOpen}
        onClose={closeDialog}
        onSubmit={handleSubmitForm}
        isEditing={!!editingAddress}
        loading={createMutation.isPending || updateMutation.isPending}
        register={register}
        errors={errors}
      />

      <Dialog
        open={!!deleteConfirm}
        onClose={() => setDeleteConfirm(null)}
        maxWidth="sm"
      >
        <DialogTitle>Delete Address</DialogTitle>
        <DialogContent>
          <Typography>Are you sure you want to delete this address? This action cannot be undone.</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteConfirm(null)}>Cancel</Button>
          <Button variant="contained" color="error" onClick={confirmDelete} disabled={deleteMutation.isPending}>
            {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={!!deleteConfirm}
        autoHideDuration={6000}
        onClose={() => setDeleteConfirm(null)}
      >
        <Alert severity="warning" variant="filled" onClose={() => setDeleteConfirm(null)}>
          Are you sure you want to delete this address?
          <Button color="inherit" size="small" onClick={confirmDelete} sx={{ ml: 1 }}>
            Yes, Delete
          </Button>
        </Alert>
      </Snackbar>
    </Box>
  );
}

interface AddressCardProps {
  address: Address;
  onEdit: (address: Address) => void;
  onDelete: (id: string) => void;
  onSetDefault: (id: string) => void;
  isDefault: boolean;
}

function AddressCard({ address, onEdit, onDelete, onSetDefault, isDefault }: AddressCardProps) {
  const formatAddress = (addr: Address) => {
    const lines = [addr.addressLine1];
    if (addr.addressLine2) lines.push(addr.addressLine2);
    lines.push(`${addr.city}, ${addr.state} ${addr.postalCode}`);
    lines.push(addr.country);
    return lines.join(', ');
  };

  return (
    <Paper elevation={1} sx={{ mb: 2 }} variant="outlined">
      <ListItem secondaryAction={
        <IconButton edge="end" aria-label="edit" onClick={() => onEdit(address)}>
          <Edit />
        </IconButton>
      }>
        <ListItemText
          primary={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
              <Typography variant="subtitle1" fontWeight={600}>
                {address.firstName} {address.lastName}
              </Typography>
              {address.company && (
                <Chip label={address.company} size="small" variant="outlined" />
              )}
              {isDefault && (
                <Chip
                  icon={<CheckCircle fontSize="small" />}
                  label="Default"
                  size="small"
                  color="primary"
                />
              )}
              <Chip
                label={address.type === 'both' ? 'Billing & Shipping' : address.type.charAt(0).toUpperCase() + address.type.slice(1)}
                size="small"
                variant="outlined"
              />
            </Box>
          }
          secondary={formatAddress(address)}
        />
        <ListItemSecondaryAction>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, minWidth: 120 }}>
            {!isDefault && (
              <Button
                size="small"
                variant="outlined"
                onClick={() => onSetDefault(address.id)}
              >
                Set as Default
              </Button>
            )}
            <IconButton
              color="error"
              onClick={() => onDelete(address.id)}
              aria-label="delete"
            >
              <Delete />
            </IconButton>
          </Box>
        </ListItemSecondaryAction>
      </ListItem>
      {address.addressLine2 && <Divider variant="inset" component="div" />}
    </Paper>
  );
}

interface AddressDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: AddressFormData) => void;
  isEditing: boolean;
  loading: boolean;
  register: ReturnType<typeof useForm<AddressFormData>['register']>;
  errors: ReturnType<typeof useForm<AddressFormData>['formState']>['errors'];
}

function AddressDialog({ open, onClose, onSubmit, isEditing, loading, register, errors }: AddressDialogProps) {
  const [country, setCountry] = useState('US');
  const [state, setState] = useState('');

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>{isEditing ? 'Edit Address' : 'Add New Address'}</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)}>
        <DialogContent>
          <Box sx={{ p: 2 }}>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="First Name *"
                  {...register('firstName')}
                  error={!!errors.firstName}
                  helperText={errors.firstName?.message}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Last Name *"
                  {...register('lastName')}
                  error={!!errors.lastName}
                  helperText={errors.lastName?.message}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Company"
                  {...register('company')}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Address Line 1 *"
                  {...register('addressLine1')}
                  error={!!errors.addressLine1}
                  helperText={errors.addressLine1?.message}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Address Line 2"
                  {...register('addressLine2')}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="City *"
                  {...register('city')}
                  error={!!errors.city}
                  helperText={errors.city?.message}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControl fullWidth error={!!errors.state}>
                  <InputLabel>State/Province *</InputLabel>
                  <Select
                    {...register('state')}
                    label="State/Province"
                    value={state}
                    onChange={(e) => { setState(e.target.value); register('state').onChange(e); }}
                  >
                    {US_STATES.map((s) => (
                      <MenuItem key={s} value={s}>{s}</MenuItem>
                    ))}
                  </Select>
                  {errors.state && <Typography color="error" variant="caption">{errors.state.message}</Typography>}
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Postal Code *"
                  {...register('postalCode')}
                  error={!!errors.postalCode}
                  helperText={errors.postalCode?.message}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControl fullWidth error={!!errors.country}>
                  <InputLabel>Country *</InputLabel>
                  <Select
                    {...register('country')}
                    label="Country"
                    value={country}
                    onChange={(e) => { setCountry(e.target.value); register('country').onChange(e); }}
                  >
                    {COUNTRIES.map((c) => (
                      <MenuItem key={c.code} value={c.code}>{c.name}</MenuItem>
                    ))}
                  </Select>
                  {errors.country && <Typography color="error" variant="caption">{errors.country.message}</Typography>}
                </FormControl>
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Phone"
                  type="tel"
                  {...register('phone')}
                />
              </Grid>
              <Grid item xs={12}>
                <Typography variant="subtitle2" gutterBottom>Address Type</Typography>
                <RadioGroup {...register('type')} row sx={{ display: 'flex', gap: 3 }}>
                  <FormControlLabel value="shipping" control={<Radio />} label="Shipping" />
                  <FormControlLabel value="billing" control={<Radio />} label="Billing" />
                  <FormControlLabel value="both" control={<Radio />} label="Both" />
                </RadioGroup>
              </Grid>
              <Grid item xs={12}>
                <FormControlLabel
                  control={
                    <Box component="fieldset" sx={{ border: 'none', padding: 0 }}>
                      <input
                        type="checkbox"
                        {...register('isDefault')}
                      />
                    </Box>
                  }
                  label="Set as default address"
                />
              </Grid>
            </Grid>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={loading}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={loading}>
            {loading ? 'Saving...' : isEditing ? 'Update' : 'Add Address'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}