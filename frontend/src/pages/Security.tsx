import { useState } from 'react';
import {
  Box, Button, Typography, Paper, TextField, Alert, AlertTitle,
  Divider, Grid, FormControlLabel, Switch, IconButton, Accordion,
  AccordionSummary, AccordionDetails, List, ListItem, ListItemText,
  ListItemIcon, Chip, Dialog, DialogTitle, DialogContent, DialogActions
} from '@mui/material';
import { Edit, Visibility, VisibilityOff, Security, Shield, Phone, Email, Key } from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@context/AuthContext';
import { useNotification } from '@context/NotificationContext';
import { authApi } from '@services/endpoints';

const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'Current password is required'),
  newPassword: z.string().min(8, 'New password must be at least 8 characters'),
  confirmPassword: z.string(),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
});

type PasswordFormData = z.infer<typeof passwordSchema>;

const emailSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(1, 'Password is required to confirm'),
});

type EmailFormData = z.infer<typeof emailSchema>;

export function Security() {
  const { user, updateUser } = useAuth();
  const queryClient = useQueryClient();
  const { showNotification } = useNotification();
  const [passwordDialogOpen, setPasswordDialogOpen] = useState(false);
  const [emailDialogOpen, setEmailDialogOpen] = useState(false);
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [showEmailPassword, setShowEmailPassword] = useState(false);

  const { register: registerPassword, handleSubmit: handleSubmitPassword, formState: { errors: passwordErrors }, reset: resetPassword } = useForm<PasswordFormData>({
    resolver: zodResolver(passwordSchema),
  });

  const { register: registerEmail, handleSubmit: handleSubmitEmail, formState: { errors: emailErrors }, reset: resetEmail } = useForm<EmailFormData>({
    resolver: zodResolver(emailSchema),
  });

  const changePasswordMutation = useMutation({
    mutationFn: (data: PasswordFormData) => authApi.changePassword(data.currentPassword, data.newPassword),
    onSuccess: () => {
      showNotification('success', 'Password changed successfully');
      closePasswordDialog();
    },
    onError: (error: any) => {
      if (error.response?.status === 400) {
        showNotification('error', 'Current password is incorrect');
      } else {
        showNotification('error', 'Failed to change password');
      }
    },
  });

  const changeEmailMutation = useMutation({
    mutationFn: (data: EmailFormData) => authApi.changeEmail(data.email, data.password),
    onSuccess: (data) => {
      updateUser({ email: data.email });
      showNotification('success', 'Email changed successfully');
      closeEmailDialog();
    },
    onError: (error: any) => {
      if (error.response?.status === 400) {
        showNotification('error', error.response.data?.message || 'Failed to change email');
      } else {
        showNotification('error', 'Failed to change email');
      }
    },
  });

  const closePasswordDialog = () => {
    setPasswordDialogOpen(false);
    resetPassword();
    setShowCurrentPassword(false);
    setShowNewPassword(false);
    setShowConfirmPassword(false);
  };

  const closeEmailDialog = () => {
    setEmailDialogOpen(false);
    resetEmail();
    setShowEmailPassword(false);
  };

  const handlePasswordSubmit = (data: PasswordFormData) => {
    changePasswordMutation.mutate(data);
  };

  const handleEmailSubmit = (data: EmailFormData) => {
    changeEmailMutation.mutate(data);
  };

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto', py: 3 }}>
      <Typography variant="h4" fontWeight={600} gutterBottom>Account Security</Typography>
      <Typography color="text.secondary" paragraph>
        Manage your password, email, and security settings.
      </Typography>

      {/* Password Section */}
      <Paper elevation={1} sx={{ p: 3, mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Key sx={{ color: 'primary.main' }} />
            <Typography variant="h6" fontWeight={600}>Password</Typography>
          </Box>
          <Button variant="outlined" startIcon={<Edit />} onClick={() => setPasswordDialogOpen(true)}>
            Change Password
          </Button>
        </Box>
        <Typography variant="body2" color="text.secondary">
          Last changed: {user?.passwordChangedAt ? new Date(user.passwordChangedAt).toLocaleDateString() : 'Unknown'}
        </Typography>
      </Paper>

      {/* Email Section */}
      <Paper elevation={1} sx={{ p: 3, mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Email sx={{ color: 'primary.main' }} />
            <Typography variant="h6" fontWeight={600}>Email Address</Typography>
          </Box>
          <Button variant="outlined" startIcon={<Edit />} onClick={() => setEmailDialogOpen(true)}>
            Change Email
          </Button>
        </Box>
        <Typography variant="body1">{user?.email}</Typography>
        {user?.emailVerified ? (
          <Chip icon={<Shield color="success" fontSize="small" />} label="Verified" color="success" size="small" sx={{ mt: 1 }} />
        ) : (
          <Box sx={{ mt: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
            <Chip icon={<Shield color="warning" fontSize="small" />} label="Not Verified" color="warning" size="small" />
            <Button size="small" variant="text" onClick={() => { /* resend verification */ }}>
              Resend Verification
            </Button>
          </Box>
        )}
      </Paper>

      {/* Two-Factor Authentication Section */}
      <Paper elevation={1} sx={{ p: 3, mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Shield sx={{ color: 'primary.main' }} />
            <Typography variant="h6" fontWeight={600}>Two-Factor Authentication</Typography>
          </Box>
          <Chip label="Coming Soon" color="default" variant="outlined" size="small" />
        </Box>
        <Typography variant="body2" color="text.secondary">
          Add an extra layer of security to your account with 2FA.
        </Typography>
        <Accordion sx={{ mt: 2 }}>
          <AccordionSummary expandIcon={<Security />}>
            <Typography variant="subtitle1">About Two-Factor Authentication</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Typography variant="body2" color="text.secondary" paragraph>
              Two-factor authentication (2FA) adds an extra layer of security by requiring a second form of verification
              in addition to your password. When enabled, you'll need to enter a code from an authenticator app
              (like Google Authenticator, Authy, or 1Password) when signing in from a new device.
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              This feature is currently in development. Once available, you'll be able to:
            </Typography>
            <List>
              <ListItem disableGutters>
                <ListItemIcon><Security fontSize="small" /></ListItemIcon>
                <ListItemText primary="Enable/disable 2FA with authenticator apps" />
              </ListItem>
              <ListItem disableGutters>
                <ListItemIcon><Security fontSize="small" /></ListItemIcon>
                <ListItemText primary="Generate backup codes for account recovery" />
              </ListItem>
              <ListItem disableGutters>
                <ListItemIcon><Security fontSize="small" /></ListItemIcon>
                <ListItemText primary="View trusted devices and revoke access" />
              </ListItem>
            </List>
          </AccordionDetails>
        </Accordion>
      </Paper>

      {/* Active Sessions Section */}
      <Paper elevation={1} sx={{ p: 3, mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Phone sx={{ color: 'primary.main' }} />
            <Typography variant="h6" fontWeight={600}>Active Sessions</Typography>
          </Box>
          <Chip label="Coming Soon" color="default" variant="outlined" size="small" />
        </Box>
        <Typography variant="body2" color="text.secondary" paragraph>
          View and manage devices currently signed into your account.
        </Typography>
        <Accordion>
          <AccordionSummary expandIcon={<Phone />}>
            <Typography variant="subtitle1">Session Management (Coming Soon)</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Typography variant="body2" color="text.secondary" paragraph>
              This feature will allow you to:
            </Typography>
            <List>
              <ListItem disableGutters>
                <ListItemIcon><Phone fontSize="small" /></ListItemIcon>
                <ListItemText primary="View all active sessions with device info" />
              </ListItem>
              <ListItem disableGutters>
                <ListItemIcon><Phone fontSize="small" /></ListItemIcon>
                <ListItemText primary="Revoke individual sessions" />
              </ListItem>
              <ListItem disableGutters>
                <ListItemIcon><Phone fontSize="small" /></ListItemIcon>
                <ListItemText primary="Sign out of all other devices" />
              </ListItem>
            </List>
          </AccordionDetails>
        </Accordion>
      </Paper>

      {/* Security Activity */}
      <Paper elevation={1} sx={{ p: 3 }}>
        <Typography variant="h6" fontWeight={600} gutterBottom>Recent Security Activity</Typography>
        <Alert severity="info" variant="filled" sx={{ mb: 2 }}>
          Security activity logging is coming soon. You'll be able to see recent logins, password changes,
          and other security-relevant events here.
        </Alert>
        <Accordion>
          <AccordionSummary expandIcon={<Security />}>
            <Typography variant="subtitle1">What will be logged</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <List>
              <ListItem disableGutters>
                <ListItemIcon><Security fontSize="small" /></ListItemIcon>
                <ListItemText primary="Successful and failed login attempts" />
              </ListItem>
              <ListItem disableGutters>
                <ListItemIcon><Security fontSize="small" /></ListItemIcon>
                <ListItemText primary="Password changes" />
              </ListItem>
              <ListItem disableGutters>
                <ListItemIcon><Security fontSize="small" /></ListItemIcon>
                <ListItemText primary="Email changes" />
              </ListItem>
              <ListItem disableGutters>
                <ListItemIcon><Security fontSize="small" /></ListItemIcon>
                <ListItemText primary="2FA enable/disable events" />
              </ListItem>
              <ListItem disableGutters>
                <ListItemIcon><Security fontSize="small" /></ListItemIcon>
                <ListItemText primary="New device logins" />
              </ListItem>
            </List>
          </AccordionDetails>
        </Accordion>
      </Paper>

      {/* Password Change Dialog */}
      <Dialog
        open={passwordDialogOpen}
        onClose={closePasswordDialog}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Change Password</DialogTitle>
        <form onSubmit={handleSubmitPassword(handlePasswordSubmit)}>
          <DialogContent>
            <Box sx={{ p: 2 }}>
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Current Password *"
                    type={showCurrentPassword ? 'text' : 'password'}
                    {...registerPassword('currentPassword')}
                    error={!!passwordErrors.currentPassword}
                    helperText={passwordErrors.currentPassword?.message}
                    InputProps={{
                      endAdornment: (
                        <IconButton
                          onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                          edge="end"
                        >
                          {showCurrentPassword ? <VisibilityOff /> : <Visibility />}
                        </IconButton>
                      ),
                    }}
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="New Password *"
                    type={showNewPassword ? 'text' : 'password'}
                    {...registerPassword('newPassword')}
                    error={!!passwordErrors.newPassword}
                    helperText={passwordErrors.newPassword?.message}
                    InputProps={{
                      endAdornment: (
                        <IconButton
                          onClick={() => setShowNewPassword(!showNewPassword)}
                          edge="end"
                        >
                          {showNewPassword ? <VisibilityOff /> : <Visibility />}
                        </IconButton>
                      ),
                    }}
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Confirm New Password *"
                    type={showConfirmPassword ? 'text' : 'password'}
                    {...registerPassword('confirmPassword')}
                    error={!!passwordErrors.confirmPassword}
                    helperText={passwordErrors.confirmPassword?.message}
                    InputProps={{
                      endAdornment: (
                        <IconButton
                          onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                          edge="end"
                        >
                          {showConfirmPassword ? <VisibilityOff /> : <Visibility />}
                        </IconButton>
                      ),
                    }}
                  />
                </Grid>
              </Grid>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={closePasswordDialog} disabled={changePasswordMutation.isPending}>
              Cancel
            </Button>
            <Button type="submit" variant="contained" disabled={changePasswordMutation.isPending}>
              {changePasswordMutation.isPending ? 'Changing...' : 'Change Password'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Email Change Dialog */}
      <Dialog
        open={emailDialogOpen}
        onClose={closeEmailDialog}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Change Email Address</DialogTitle>
        <form onSubmit={handleSubmitEmail(handleEmailSubmit)}>
          <DialogContent>
            <Box sx={{ p: 2 }}>
              <Alert severity="info" variant="filled" sx={{ mb: 2 }}>
                A verification email will be sent to the new address. You must verify it before the change takes effect.
              </Alert>
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="New Email Address *"
                    type="email"
                    autoComplete="email"
                    {...registerEmail('email')}
                    error={!!emailErrors.email}
                    helperText={emailErrors.email?.message}
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    label="Current Password *"
                    type={showEmailPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    {...registerEmail('password')}
                    error={!!emailErrors.password}
                    helperText={emailErrors.password?.message}
                    InputProps={{
                      endAdornment: (
                        <IconButton
                          onClick={() => setShowEmailPassword(!showEmailPassword)}
                          edge="end"
                        >
                          {showEmailPassword ? <VisibilityOff /> : <Visibility />}
                        </IconButton>
                      ),
                    }}
                  />
                </Grid>
              </Grid>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={closeEmailDialog} disabled={changeEmailMutation.isPending}>
              Cancel
            </Button>
            <Button type="submit" variant="contained" disabled={changeEmailMutation.isPending}>
              {changeEmailMutation.isPending ? 'Changing...' : 'Change Email'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
}