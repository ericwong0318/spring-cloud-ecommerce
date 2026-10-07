import { Box, Typography, Link, Divider, Grid, IconButton } from '@mui/material';
import { Facebook, Twitter, Instagram, LinkedIn } from '@mui/icons-material';
import { Link as RouterLink } from 'react-router-dom';

export function Footer() {
  const currentYear = new Date().getFullYear();

  const footerLinks = {
    shop: [
      { label: 'All Products', href: '/products' },
      { label: 'Categories', href: '/categories' },
      { label: 'New Arrivals', href: '/products?sort=newest' },
      { label: 'Best Sellers', href: '/products?sort=popular' },
      { label: 'Sale', href: '/products?sale=true' },
    ],
    support: [
      { label: 'Contact Us', href: '/contact' },
      { label: 'FAQs', href: '/faq' },
      { label: 'Shipping Info', href: '/shipping' },
      { label: 'Returns & Exchanges', href: '/returns' },
      { label: 'Track Order', href: '/track-order' },
    ],
    company: [
      { label: 'About Us', href: '/about' },
      { label: 'Careers', href: '/careers' },
      { label: 'Press', href: '/press' },
      { label: 'Blog', href: '/blog' },
      { label: 'Affiliates', href: '/affiliates' },
    ],
    legal: [
      { label: 'Privacy Policy', href: '/privacy' },
      { label: 'Terms of Service', href: '/terms' },
      { label: 'Cookie Policy', href: '/cookies' },
      { label: 'Accessibility', href: '/accessibility' },
    ],
  };

  const socialLinks = [
    { icon: <Facebook />, href: 'https://facebook.com', label: 'Facebook' },
    { icon: <Twitter />, href: 'https://twitter.com', label: 'Twitter' },
    { icon: <Instagram />, href: 'https://instagram.com', label: 'Instagram' },
    { icon: <LinkedIn />, href: 'https://linkedin.com', label: 'LinkedIn' },
  ];

  return (
    <Box component="footer" sx={{ backgroundColor: 'background.default', borderTop: 1, borderColor: 'divider', mt: 'auto' }}>
      <Box component="footer" sx={{ py: 6, px: { xs: 3, md: 6 } }}>
        <Grid container spacing={4}>
          {/* Brand Section */}
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="h6" component="h2" fontWeight={700} color="primary.main" gutterBottom>
              E-Shop
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              Your trusted online marketplace for quality products at great prices.
              Fast shipping, easy returns, and exceptional customer service.
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
              {socialLinks.map((social) => (
                <IconButton
                  key={social.label}
                  size="small"
                  component={RouterLink}
                  to={social.href}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={social.label}
                  sx={{ color: 'text.secondary', '&:hover': { color: 'primary.main' } }}
                >
                  {social.icon}
                </IconButton>
              ))}
            </Box>
          </Grid>

          {/* Shop Links */}
          <Grid item xs={6} sm={3}>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              Shop
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              {footerLinks.shop.map((link) => (
                <Link key={link.href} component={RouterLink} to={link.href} variant="body2" color="text.secondary" sx={{ '&:hover': { color: 'primary.main' } }}>
                  {link.label}
                </Link>
              ))}
            </Box>
          </Grid>

          {/* Support Links */}
          <Grid item xs={6} sm={3}>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              Support
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              {footerLinks.support.map((link) => (
                <Link key={link.href} component={RouterLink} to={link.href} variant="body2" color="text.secondary" sx={{ '&:hover': { color: 'primary.main' } }}>
                  {link.label}
                </Link>
              ))}
            </Box>
          </Grid>

          {/* Company Links */}
          <Grid item xs={6} sm={3}>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              Company
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              {footerLinks.company.map((link) => (
                <Link key={link.href} component={RouterLink} to={link.href} variant="body2" color="text.secondary" sx={{ '&:hover': { color: 'primary.main' } }}>
                  {link.label}
                </Link>
              ))}
            </Box>
          </Grid>

          {/* Legal Links */}
          <Grid item xs={6} sm={3}>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              Legal
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              {footerLinks.legal.map((link) => (
                <Link key={link.href} component={RouterLink} to={link.href} variant="body2" color="text.secondary" sx={{ '&:hover': { color: 'primary.main' } }}>
                  {link.label}
                </Link>
              ))}
            </Box>
          </Grid>

          {/* Newsletter */}
          <Grid item xs={12} sm={6} md={4}>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              Newsletter
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              Subscribe to get special offers, free giveaways, and once-in-a-lifetime deals.
            </Typography>
            <Box component="form" sx={{ display: 'flex', gap: 1, mt: 2, maxWidth: 300 }}>
              <input
                type="email"
                placeholder="Enter your email"
                required
                style={{
                  flex: 1,
                  padding: '8px 12px',
                  borderRadius: '8px 0 0 8px',
                  border: '1px solid',
                  borderColor: 'divider',
                  backgroundColor: 'background.paper',
                  color: 'text.primary',
                  outline: 'none',
                }}
              />
              <button
                type="submit"
                style={{
                  padding: '8px 16px',
                  borderRadius: '0 8px 8px 0',
                  border: 'none',
                  backgroundColor: 'primary.main',
                  color: 'white',
                  fontWeight: 500,
                  cursor: 'pointer',
                }}
              >
                Subscribe
              </button>
            </Box>
          </Grid>

          {/* Payment Methods */}
          <Grid item xs={12} sm={6} md={4}>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              Payment Methods
            </Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mt: 1 }}>
              {['Visa', 'Mastercard', 'American Express', 'PayPal', 'Apple Pay', 'Google Pay'].map((method) => (
                <Box
                  key={method}
                  sx={{
                    px: 2,
                    py: 1,
                    border: 1,
                    borderColor: 'divider',
                    borderRadius: 1,
                    fontSize: '0.75rem',
                    fontWeight: 500,
                    color: 'text.secondary',
                    backgroundColor: 'background.paper',
                  }}
                >
                  {method}
                </Box>
              ))}
            </Box>
          </Grid>

          {/* Security Badges */}
          <Grid item xs={12} sm={6} md={4}>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              Secure Shopping
            </Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mt: 1 }}>
              {['SSL Secured', 'PCI DSS Compliant', '256-bit Encryption', 'Money Back Guarantee'].map((badge) => (
                <Box
                  key={badge}
                  sx={{
                    px: 2,
                    py: 1,
                    border: 1,
                    borderColor: 'success.light',
                    borderRadius: 1,
                    fontSize: '0.75rem',
                    fontWeight: 500,
                    color: 'success.main',
                    backgroundColor: 'success.light',
                    opacity: 0.1,
                  }}
                >
                  {badge}
                </Box>
              ))}
            </Box>
          </Grid>
        </Grid>

        <Divider sx={{ my: 4 }} />

        {/* Bottom Bar */}
        <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', gap: 2 }}>
          <Typography variant="body2" color="text.secondary">
            © {currentYear} E-Shop. All rights reserved.
          </Typography>
          <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
            <Link component={RouterLink} to="/privacy" variant="body2" color="text.secondary">
              Privacy Policy
            </Link>
            <Link component={RouterLink} to="/terms" variant="body2" color="text.secondary">
              Terms of Service
            </Link>
            <Link component={RouterLink} to="/cookies" variant="body2" color="text.secondary">
              Cookie Settings
            </Link>
            <Link component={RouterLink} to="/accessibility" variant="body2" color="text.secondary">
              Accessibility
            </Link>
          </Box>
        </Box>
      </Box>
    </Box>
  );
}