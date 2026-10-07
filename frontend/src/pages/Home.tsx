import { Box, Grid, Typography, Button, Card, CardContent, CardMedia, Link, Skeleton } from '@mui/material';
import { ArrowForward, ShoppingCart, LocalShipping, SupportAgent, Verified, Lock } from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { Link as RouterLink } from 'react-router-dom';
import { productApi, categoryApi } from '../services/endpoints';
import { ProductCardComponent, InfoCard, InfoCardIcon, InfoCardTitle, InfoCardDescription } from '@components/Card';
import type { Category } from '../types/domain';

function HeroSection() {
  return (
    <Box
      sx={{
        background: 'linear-gradient(135deg, #1976d2 0%, #9c27b0 100%)',
        color: 'white',
        borderRadius: 3,
        p: { xs: 4, md: 6 },
        textAlign: 'center',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      <Box sx={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.1)' }} />
      <Box sx={{ position: 'relative', zIndex: 1, maxWidth: 700, mx: 'auto' }}>
        <Typography variant="h2" component="h1" fontWeight={700} gutterBottom>
          Discover Amazing Products at Unbeatable Prices
        </Typography>
        <Typography variant="h6" color="inherit" component="p" sx={{ opacity: 0.9, mb: 4 }}>
          Shop from thousands of products across multiple categories. Fast shipping, easy returns, and secure payments.
        </Typography>
        <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center', flexWrap: 'wrap' }}>
          <Button
            variant="contained"
            color="secondary"
            size="large"
            component={RouterLink}
            to="/products"
            startIcon={<ShoppingCart />}
            sx={{ px: 4, py: 1.5 }}
          >
            Shop Now
          </Button>
          <Button
            variant="outlined"
            color="inherit"
            size="large"
            component={RouterLink}
            to="/categories"
            endIcon={<ArrowForward />}
            sx={{ px: 4, py: 1.5, borderColor: 'white', color: 'white', '&:hover': { borderColor: 'white' } }}
          >
            Browse Categories
          </Button>
        </Box>
      </Box>
    </Box>
  );
}

function FeaturesSection() {
  const features = [
    { icon: <LocalShipping />, title: 'Free Shipping', description: 'On orders over $50' },
    { icon: <SupportAgent />, title: '24/7 Support', description: 'Dedicated customer service' },
    { icon: <Verified />, title: 'Quality Guaranteed', description: 'Authentic products only' },
    { icon: <Lock />, title: 'Secure Payments', description: 'SSL encrypted checkout' },
  ];

  return (
    <Box sx={{ py: 4 }}>
      <Grid container spacing={3}>
        {features.map((feature, index) => (
          <Grid item xs={12} sm={6} md={3} key={index}>
            <InfoCard>
              <InfoCardIcon>{feature.icon}</InfoCardIcon>
              <InfoCardTitle>{feature.title}</InfoCardTitle>
              <InfoCardDescription>{feature.description}</InfoCardDescription>
            </InfoCard>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}

function CategorySection() {
  const { data: categories, isLoading, error } = useQuery({
    queryKey: ['categories', 'tree'],
    queryFn: () => categoryApi.getCategoryTree(),
    staleTime: 1000 * 60 * 30, // 30 minutes
  });

  if (isLoading) {
    return (
      <Grid container spacing={3}>
        {[...Array(8)].map((_, i) => (
          <Grid item xs={6} sm={3} key={i}>
            <Skeleton variant="rectangular" height={160} />
          </Grid>
        ))}
      </Grid>
    );
  }

  if (error || !categories?.length) return null;

  const topCategories = categories.filter((c) => !c.parentId).slice(0, 8);

  return (
    <Box sx={{ mb: 6 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Typography variant="h4" component="h2" fontWeight={600}>
          Shop by Category
        </Typography>
        <Link component={RouterLink} to="/categories" variant="body1" color="primary" fontWeight={500}>
          View All Categories
        </Link>
      </Box>
      <Grid container spacing={3}>
        {topCategories.map((category) => (
          <Grid item xs={6} sm={3} key={category.id}>
            <CategoryCard category={category} />
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}

function CategoryCard({ category }: { category: Category }) {
  return (
    <RouterLink to={`/products?category=${category.id}`} style={{ textDecoration: 'none', display: 'block' }}>
      <Card
        sx={{
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          transition: 'all 0.2s ease',
          '&:hover': {
            transform: 'translateY(-4px)',
            boxShadow: 3,
          },
        }}
      >
        <CardMedia
          component="div"
          sx={{ height: 120, backgroundSize: 'cover', backgroundPosition: 'center', backgroundColor: 'action.hover' }}
          image={category.imageUrl || ''}
          title={category.name}
        />
        <CardContent sx={{ p: 2, textAlign: 'center', flexGrow: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
          <Typography variant="subtitle1" fontWeight={600} color="text.primary">
            {category.name}
          </Typography>
          {category.productCount && (
            <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
              {category.productCount} products
            </Typography>
          )}
        </CardContent>
      </Card>
    </RouterLink>
  );
}

function FeaturedProductsSection() {
  const { data: products, isLoading, error } = useQuery({
    queryKey: ['products', 'featured'],
    queryFn: () => productApi.getFeaturedProducts(8),
    staleTime: 1000 * 60 * 10, // 10 minutes
  });

  return (
    <Box sx={{ mb: 6 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Typography variant="h4" component="h2" fontWeight={600}>
          Featured Products
        </Typography>
        <Link component={RouterLink} to="/products" variant="body1" color="primary" fontWeight={500}>
          View All Products
        </Link>
      </Box>
      {isLoading ? (
        <Grid container spacing={3}>
          {[...Array(8)].map((_, i) => (
            <Grid item xs={6} sm={4} md={3} key={i}>
              <Skeleton variant="rectangular" height={320} />
            </Grid>
          ))}
        </Grid>
      ) : error ? (
        <Typography color="text.secondary" align="center">Failed to load featured products</Typography>
      ) : products?.length ? (
        <Grid container spacing={3}>
          {products.map((product) => (
            <Grid item xs={6} sm={4} md={3} key={product.id}>
              <ProductCardComponent product={product} />
            </Grid>
          ))}
        </Grid>
      ) : (
        <Typography color="text.secondary" align="center">No featured products available</Typography>
      )}
    </Box>
  );
}

function CTASection() {
  return (
    <Box
      sx={{
        backgroundColor: 'primary.main',
        color: 'white',
        borderRadius: 3,
        p: { xs: 4, md: 6 },
        textAlign: 'center',
        mt: 4,
      }}
    >
      <Typography variant="h4" component="h2" fontWeight={600} gutterBottom>
        Ready to Start Shopping?
      </Typography>
      <Typography variant="body1" component="p" sx={{ opacity: 0.9, mb: 4, maxWidth: 600, mx: 'auto' }}>
        Join thousands of happy customers. Sign up today and get 10% off your first order!
      </Typography>
      <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center', flexWrap: 'wrap' }}>
        <Button
          variant="contained"
          color="secondary"
          size="large"
          component={RouterLink}
          to="/register"
          sx={{ px: 4, py: 1.5 }}
        >
          Create Free Account
        </Button>
        <Button
          variant="outlined"
          color="inherit"
          size="large"
          component={RouterLink}
          to="/products"
          sx={{ px: 4, py: 1.5, borderColor: 'white', color: 'white', '&:hover': { borderColor: 'white' } }}
        >
          Browse Products
        </Button>
      </Box>
    </Box>
  );
}

export function Home() {
  return (
    <Box>
      <HeroSection />
      <FeaturesSection />
      <CategorySection />
      <FeaturedProductsSection />
      <CTASection />
    </Box>
  );
}