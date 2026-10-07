import { AppBar, Toolbar, Typography, IconButton, Menu, MenuItem, Avatar, Box, TextField, InputAdornment, Badge, Drawer, List, ListItem, ListItemIcon, ListItemText, Divider, useMediaQuery, useTheme } from '@mui/material';
import { Menu as MenuIcon, Search as SearchIcon, ShoppingCart as CartIcon, Person as PersonIcon, Logout as LogoutIcon, Settings as SettingsIcon, Brightness4 } from '@mui/icons-material';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../hooks';
import { formatCurrency } from '@utils/formatters';
import { PrimaryButton, GhostButton } from './Button';

export function Header() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [cartDrawerOpen, setCartDrawerOpen] = useState(false);
  
  const { user, isAuthenticated, logout } = useAuth();
  const { cart } = useCart();
  const navigate = useNavigate();

  const handleSearch = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && searchQuery.trim()) {
      navigate(`/products`, { state: { search: searchQuery.trim() } });
      setSearchQuery('');
    }
  };

  const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleMenuItemClick = (path: string) => {
    handleMenuClose();
    navigate(path);
  };

  const handleLogout = async () => {
    handleMenuClose();
    await logout();
  };

  const handleMobileMenuToggle = () => {
    setMobileMenuOpen(!mobileMenuOpen);
  };

  const handleCartToggle = () => {
    setCartDrawerOpen(!cartDrawerOpen);
  };

  const userInitials = user ? `${user.firstName[0]}${user.lastName[0]}`.toUpperCase() : '';

  return (
    <>
      <AppBar position="sticky" elevation={0} sx={{ zIndex: 1200 }}>
        <Toolbar>
          {/* Mobile Menu Button */}
          <IconButton
            color="inherit"
            edge="start"
            onClick={handleMobileMenuToggle}
            sx={{ mr: 2, display: { md: 'none' } }}
            aria-label="Menu"
          >
            <MenuIcon />
          </IconButton>

          {/* Logo */}
          <Link to="/" style={{ textDecoration: 'none', flexGrow: 1 }}>
            <Typography variant="h6" component="h1" sx={{ fontWeight: 700, color: 'primary.main' }}>
              E-Shop
            </Typography>
          </Link>

          {/* Search Bar - Desktop */}
          {!isMobile && (
            <Box sx={{ flexGrow: 1, maxWidth: 600, mx: 4 }}>
              <TextField
                placeholder="Search products..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyDown={handleSearch}
                size="small"
                variant="outlined"
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon color="action" />
                    </InputAdornment>
                  ),
                }}
                sx={{
                  '& .MuiOutlinedInput-root': {
                    backgroundColor: 'background.paper',
                    borderRadius: '24px',
                  },
                }}
              />
            </Box>
          )}

          {/* Actions */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {/* Cart */}
            <IconButton
              color="inherit"
              onClick={handleCartToggle}
              aria-label={`Shopping cart, ${cart?.itemCount} items`}
              sx={{ position: 'relative' }}
            >
              <Badge badgeContent={cart?.itemCount} color="error" overlap="circular">
                <CartIcon />
              </Badge>
            </IconButton>

            {/* Theme Toggle */}
            <IconButton color="inherit" aria-label="Toggle theme">
              <Brightness4 />
            </IconButton>

            {/* User Menu */}
            {isAuthenticated ? (
              <IconButton
                color="inherit"
                onClick={handleProfileMenuOpen}
                aria-label="Account"
              >
                <Avatar sx={{ width: 36, height: 36, fontSize: '0.875rem' }}>
                  {user?.avatar ? (
                    <img src={user.avatar} alt="" style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
                  ) : (
                    userInitials
                  )}
                </Avatar>
              </IconButton>
            ) : (
              <Box sx={{ display: 'flex', gap: 1, ml: 1 }}>
                <GhostButton onClick={() => navigate('/login')}>Sign In</GhostButton>
                <PrimaryButton onClick={() => navigate('/register')}>Sign Up</PrimaryButton>
              </Box>
            )}
          </Box>
        </Toolbar>
      </AppBar>

      {/* Mobile Search Bar */}
      {isMobile && mobileMenuOpen && (
        <Toolbar>
          <TextField
            placeholder="Search products..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={handleSearch}
            size="small"
            variant="outlined"
            fullWidth
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon color="action" />
                </InputAdornment>
              ),
            }}
            sx={{
              '& .MuiOutlinedInput-root': {
                backgroundColor: 'background.paper',
                borderRadius: '24px',
              },
            }}
          />
        </Toolbar>
      )}

      {/* Mobile Drawer */}
      <Drawer
        variant="temporary"
        open={mobileMenuOpen}
        onClose={handleMobileMenuToggle}
        ModalProps={{ keepMounted: true }}
        sx={{
          display: { md: 'none' },
          '& .MuiDrawer-paper': { boxSizing: 'border-box', width: 280 },
        }}
      >
        <Box sx={{ p: 2 }}>
          <Typography variant="h6" fontWeight={700} color="primary.main" gutterBottom>
            Menu
          </Typography>
          <Divider sx={{ mb: 2 }} />
          <List>
            <ListItem component={Link} to="/products" onClick={() => handleMobileMenuToggle()}>
              <ListItemIcon><SearchIcon /></ListItemIcon>
              <ListItemText primary="Products" />
            </ListItem>
            <ListItem component={Link} to="/categories" onClick={() => handleMobileMenuToggle()}>
              <ListItemIcon><PersonIcon /></ListItemIcon>
              <ListItemText primary="Categories" />
            </ListItem>
            {isAuthenticated && (
              <>
                <Divider sx={{ my: 1 }} />
                <ListItem component={Link} to="/account" onClick={() => handleMobileMenuToggle()}>
                  <ListItemIcon><PersonIcon /></ListItemIcon>
                  <ListItemText primary="My Account" />
                </ListItem>
                <ListItem component={Link} to="/account/orders" onClick={() => handleMobileMenuToggle()}>
                  <ListItemIcon><SettingsIcon /></ListItemIcon>
                  <ListItemText primary="My Orders" />
                </ListItem>
                <ListItem component={Link} to="/login" onClick={handleLogout}>
                  <ListItemIcon><LogoutIcon /></ListItemIcon>
                  <ListItemText primary="Sign Out" />
                </ListItem>
              </>
            )}
            {!isAuthenticated && (
              <>
                <Divider sx={{ my: 1 }} />
                <ListItem component={Link} to="/login" onClick={() => handleMobileMenuToggle()}>
                  <ListItemIcon><PersonIcon /></ListItemIcon>
                  <ListItemText primary="Sign In" />
                </ListItem>
                <ListItem component={Link} to="/register" onClick={() => handleMobileMenuToggle()}>
                  <ListItemIcon><PersonIcon /></ListItemIcon>
                  <ListItemText primary="Sign Up" />
                </ListItem>
              </>
            )}
          </List>
        </Box>
      </Drawer>

      {/* Profile Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
      >
        <Box sx={{ px: 2, py: 1 }}>
          <Typography variant="subtitle1" fontWeight={600}>{user?.firstName} {user?.lastName}</Typography>
          <Typography variant="body2" color="text.secondary">{user?.email}</Typography>
        </Box>
        <Divider />
        <MenuItem onClick={() => handleMenuItemClick('/account')}>
          <ListItemIcon><PersonIcon fontSize="small" /></ListItemIcon>
          My Account
        </MenuItem>
        <MenuItem onClick={() => handleMenuItemClick('/account/orders')}>
          <ListItemIcon><SettingsIcon fontSize="small" /></ListItemIcon>
          My Orders
        </MenuItem>
        <MenuItem onClick={() => handleMenuItemClick('/account/addresses')}>
          <ListItemIcon><PersonIcon fontSize="small" /></ListItemIcon>
          Addresses
        </MenuItem>
        <Divider />
        <MenuItem onClick={handleLogout}>
          <ListItemIcon><LogoutIcon fontSize="small" color="error" /></ListItemIcon>
          Sign Out
        </MenuItem>
      </Menu>

      {/* Cart Drawer */}
      <Drawer
        anchor="right"
        open={cartDrawerOpen}
        onClose={handleCartToggle}
        sx={{
          width: { xs: '100%', sm: 400 },
          '& .MuiDrawer-paper': { boxSizing: 'border-box' },
        }}
      >
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
          <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">Shopping Cart ({cart?.itemCount})</Typography>
            <IconButton onClick={handleCartToggle}><MenuIcon /></IconButton>
          </Box>
          
          <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
            {cart?.items.length === 0 ? (
              <Box sx={{ textAlign: 'center', py: 4 }}>
                <CartIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
                <Typography variant="body1" color="text.secondary">Your cart is empty</Typography>
                <PrimaryButton onClick={() => { navigate('/products'); handleCartToggle(); }} sx={{ mt: 2 }}>
                  Continue Shopping
                </PrimaryButton>
              </Box>
            ) : (
              <List dense>
                {cart?.items.map((item: any) => (
                  <ListItem key={item.id} sx={{ py: 1 }} disablePadding>
                    <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                      <Box component="img" src={item.productThumbnail} alt={item.productName} sx={{ width: 60, height: 60, objectFit: 'cover', borderRadius: 1 }} />
                      <Box sx={{ flex: 1, minWidth: 0 }}>
                        <Typography variant="body2" fontWeight={500} noWrap>{item.productName}</Typography>
                        <Typography variant="caption" color="text.secondary">{formatCurrency(item.price, item.currency)} x {item.quantity}</Typography>
                      </Box>
                      <Typography variant="body2" fontWeight={500}>{formatCurrency(item.subtotal, item.currency)}</Typography>
                    </Box>
                  </ListItem>
                ))}
              </List>
            )}
          </Box>
          
          {cart && cart.items.length > 0 && (
            <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography>Subtotal</Typography>
                <Typography fontWeight={500}>{formatCurrency(cart.subtotal, cart.currency)}</Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                <Typography>Estimated Total</Typography>
                <Typography variant="h6" fontWeight={700} color="primary.main">{formatCurrency(cart.total, cart.currency)}</Typography>
              </Box>
              <PrimaryButton fullWidth onClick={() => { navigate('/cart'); handleCartToggle(); }}>
                View Cart
              </PrimaryButton>
              <GhostButton fullWidth sx={{ mt: 1 }} onClick={() => { navigate('/checkout'); handleCartToggle(); }}>
                Checkout
              </GhostButton>
            </Box>
          )}
        </Box>
      </Drawer>
    </>
  );
}