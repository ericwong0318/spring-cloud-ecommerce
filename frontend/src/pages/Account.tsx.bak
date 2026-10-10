import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Box, Typography, Card, CardContent, Button, TextField, Alert, CircularProgress, Tabs, Tab, Stack, Divider, List, ListItem, ListItemText, ListItemIcon, IconButton, Grid, Chip, Avatar } from '@mui/material';
import { Person, Email, Phone, LocationOn, Security, Edit, Save, Cancel, History, Add, Delete } from '@mui/icons-material';
import { useAuth } from '../hooks/useAuth';
import { addressApi } from '../services/endpoints';
import type { User, Address } from '../types/domain';

export function Account() {
  const { user, updateProfile, changePassword } = useAuth();
  const [tab, setTab] = useState(0);
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState<Partial<User>>({});
  const [passwordData, setPasswordData] = useState({ currentPassword: '', newPassword: '', confirmNewPassword: '' });

  const { data: addresses, isLoading } = useQuery({
    queryKey: ['addresses'],
    queryFn: () => addressApi.getAddresses(),
  });

  const updateProfileMutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: () => {
      setIsEditing(false);
    },
  });

  const changePasswordMutation = useMutation({
    mutationFn: ({ currentPassword, newPassword }: { currentPassword: string; newPassword: string }) => changePassword(currentPassword, newPassword),
    onSuccess: () => {
      setPasswordData({ currentPassword: '', newPassword: '', confirmNewPassword: '' });
    },
  });

  const handleProfileUpdate = () => {
    updateProfileMutation.mutate(formData);
  };

  const handlePasswordChange = () => {
    changePasswordMutation.mutate(passwordData);
  };

  const handleAddressAction = (action: 'edit' | 'delete' | 'set-default', addressId: string) => {
    switch (action) {
      case 'edit':
        window.location.href = `/account/addresses/${addressId}`;
        break;
      case 'delete':
        if (window.confirm('Are you sure you want to delete this address?')) {
          addressApi.deleteAddress(addressId);
        }
        break;
      case 'set-default':
        addressApi.setDefaultAddress(addressId, 'SHIPPING');
        break;
    }
  };

  return (
    <Box>
      <Typography variant="h4" component="h1" fontWeight={600} gutterBottom>
        My Account
      </Typography>

      <Tabs value={tab} onChange={(_, newValue) => setTab(newValue)} sx={{ mb: 3 }}>
        <Tab icon={<Person />} label="Profile" />
        <Tab icon={<LocationOn />} label="Addresses" />
        <Tab icon={<History />} label="Order History" />
        <Tab icon={<Security />} label="Security" />
      </Tabs>

      {/* Profile Tab */}
      {tab === 0 && (
        <Card variant="outlined">
          <CardContent>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
              <Typography variant="h6" fontWeight={600}>Profile Information</Typography>
              {!isEditing ? (
                <Button variant="outlined" startIcon={<Edit />} onClick={() => { setIsEditing(true); setFormData({ firstName: user?.firstName, lastName: user?.lastName, email: user?.email, phone: user?.phone }); }}>
                  Edit Profile
                </Button>
              ) : (
                <Stack direction="row" spacing={1}>
                  <Button variant="outlined" startIcon={<Cancel />} onClick={() => { setIsEditing(false); setFormData({}); }}>
                    Cancel
                  </Button>
                  <Button variant="contained" startIcon={<Save />} onClick={handleProfileUpdate} disabled={updateProfileMutation.isPending}>
                    {updateProfileMutation.isPending ? 'Saving...' : 'Save'}
                  </Button>
                </Stack>
              )}
            </Stack>

            {updateProfileMutation.isError && (
              <Alert severity="error" sx={{ mb: 2 }}>Failed to update profile. Please try again.</Alert>
            )}

            {isEditing ? (
              <Stack spacing={2}>
                <Grid container spacing={2}>
                  <Grid item xs={12} md={6}>
                    <TextField label="First Name" value={formData.firstName || ''} onChange={(e) => setFormData({ ...formData, firstName: e.target.value })} fullWidth required />
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <TextField label="Last Name" value={formData.lastName || ''} onChange={(e) => setFormData({ ...formData, lastName: e.target.value })} fullWidth required />
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <TextField label="Email" type="email" value={formData.email || ''} onChange={(e) => setFormData({ ...formData, email: e.target.value })} fullWidth required />
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <TextField label="Phone" value={formData.phone || ''} onChange={(e) => setFormData({ ...formData, phone: e.target.value })} fullWidth />
                  </Grid>
                </Grid>
              </Stack>
            ) : (
              <Stack spacing={2}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Avatar sx={{ width: 64, height: 64, fontSize: '1.5rem' }}>
                    {user?.avatar ? (
                      <img src={user.avatar} alt="" style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
                    ) : (
                      `${user?.firstName?.charAt(0)}${user?.lastName?.charAt(0)}`.toUpperCase()
                    )}
                  </Avatar>
                  <Box>
                    <Typography variant="h6">{user?.firstName} {user?.lastName}</Typography>
                    <Typography variant="body2" color="text.secondary">{user?.email}</Typography>
                    <Chip label={user?.emailVerified ? 'Verified' : 'Unverified'} size="small" color={user?.emailVerified ? 'success' : 'warning'} variant="outlined" sx={{ mt: 0.5 }} />
                  </Box>
                </Box>
                <Divider />
                <Stack spacing={1}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Email fontSize="small" color="action" />
                    <Typography variant="body2">{user?.email}</Typography>
                  </Box>
                  {user?.phone && (
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Phone fontSize="small" color="action" />
                      <Typography variant="body2">{user?.phone}</Typography>
                    </Box>
                  )}
                </Stack>
              </Stack>
            )}
          </CardContent>
        </Card>
      )}

      {/* Addresses Tab */}
      {tab === 1 && (
        <Card variant="outlined">
          <CardContent>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
              <Typography variant="h6" fontWeight={600}>Saved Addresses</Typography>
              <Button variant="contained" startIcon={<Add />} onClick={() => window.location.href = '/account/addresses/new'}>
                Add Address
              </Button>
            </Stack>

            {isLoading ? (
              <CircularProgress />
            ) : !addresses || (addresses as Address[]).length === 0 ? (
              <Alert severity="info">No saved addresses. Add one for faster checkout.</Alert>
            ) : (
              <List>
                {(addresses as Address[]).map((address) => (
                  <ListItem key={address.id} divider>
                    <ListItemIcon>
                      <LocationOn color="action" />
                    </ListItemIcon>
                    <ListItemText
                      primary={`${address.firstName} ${address.lastName}`}
                      secondary={
                        <>
                          <Typography variant="body2" color="text.secondary">{address.addressLine1}</Typography>
                          {address.addressLine2 && <Typography variant="body2" color="text.secondary">{address.addressLine2}</Typography>}
                          <Typography variant="body2" color="text.secondary">{address.city}, {address.state} {address.postalCode}, {address.country}</Typography>
                          <Typography variant="body2" color="text.secondary">{address.phone}</Typography>
                          <Chip label={address.type || 'Shipping'} size="small" variant="outlined" sx={{ mt: 0.5, mr: 0.5 }} />
                          {address.isDefault && <Chip label="Default" size="small" color="primary" variant="outlined" sx={{ mt: 0.5 }} />}
                        </>
                      }
                    />
                    <IconButton onClick={() => handleAddressAction('edit', address.id!)}><Edit fontSize="small" /></IconButton>
                    <IconButton onClick={() => handleAddressAction('delete', address.id!)}><Delete fontSize="small" color="error" /></IconButton>
                  </ListItem>
                ))}
              </List>
            )}
          </CardContent>
        </Card>
      )}

      {/* Order History Tab */}
      {tab === 2 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="h6" fontWeight={600} gutterBottom>Order History</Typography>
            <Alert severity="info">
              <Typography variant="body1">View your complete order history and track shipments.</Typography>
              <Button variant="outlined" size="small" onClick={() => window.location.href = '/account/orders'} sx={{ mt: 1 }}>
                View All Orders
              </Button>
            </Alert>
          </CardContent>
        </Card>
      )}

      {/* Security Tab */}
      {tab === 3 && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="h6" fontWeight={600} gutterBottom>Change Password</Typography>
            <Stack spacing={2} sx={{ maxWidth: 400 }}>
              <TextField label="Current Password" type="password" value={passwordData.currentPassword} onChange={(e) => setPasswordData({ ...passwordData, currentPassword: e.target.value })} fullWidth required />
              <TextField label="New Password" type="password" value={passwordData.newPassword} onChange={(e) => setPasswordData({ ...passwordData, newPassword: e.target.value })} fullWidth required helperText="Minimum 8 characters" />
              <TextField label="Confirm New Password" type="password" value={passwordData.confirmNewPassword} onChange={(e) => setPasswordData({ ...passwordData, confirmNewPassword: e.target.value })} fullWidth required />
              <Button variant="contained" startIcon={<Save />} onClick={handlePasswordChange} disabled={changePasswordMutation.isPending}>
                {changePasswordMutation.isPending ? 'Updating...' : 'Update Password'}
              </Button>
            </Stack>
          </CardContent>
        </Card>
      )}
    </Box>
  );
}