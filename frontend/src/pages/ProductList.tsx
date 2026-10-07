import { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Box, Grid, Typography, TextField, InputAdornment, Select, MenuItem, FormControl, InputLabel, Skeleton, Pagination, Chip, Button, IconButton, Drawer, List, ListItem, ListItemText, Divider, Accordion, AccordionSummary, AccordionDetails, Slider, FormControlLabel, Checkbox, Paper } from '@mui/material';
import { Search as SearchIcon, FilterList, Close, ExpandMore } from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { productApi, categoryApi } from '../services/endpoints';
import { ProductCardComponent } from '@components/Card';
import type { ProductFilters } from '../types/domain';

const SORT_OPTIONS = [
  { value: 'popular', label: 'Most Popular' },
  { value: 'newest', label: 'Newest' },
  { value: 'price_asc', label: 'Price: Low to High' },
  { value: 'price_desc', label: 'Price: High to Low' },
  { value: 'name_asc', label: 'Name: A to Z' },
  { value: 'name_desc', label: 'Name: Z to A' },
];

export function ProductList() {
  const [searchParams, setSearchParams] = useSearchParams();
  
  // Filter state
  const [filters, setFilters] = useState<ProductFilters>({
    page: Number(searchParams.get('page')) || 0,
    size: 20,
    sortBy: (searchParams.get('sort') as ProductFilters['sortBy']) || 'popular',
    categoryId: searchParams.get('category') || undefined,
    search: searchParams.get('search') || undefined,
    minPrice: searchParams.get('minPrice') ? Number(searchParams.get('minPrice')) : undefined,
    maxPrice: searchParams.get('maxPrice') ? Number(searchParams.get('maxPrice')) : undefined,
    inStockOnly: searchParams.get('inStock') === 'true',
  });
  
  const [priceRange, setPriceRange] = useState<[number, number]>([0, 1000]);
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);
  
  // Debounced search
  const debouncedSearch = useCallback(
    (value: string) => {
      setTimeout(() => {
        setFilters((prev) => ({ ...prev, search: value || undefined, page: 0 }));
      }, 300);
    },
    []
  );

  // Sync filters with URL
  useEffect(() => {
    const params = new URLSearchParams();
    if (filters.search) params.set('search', filters.search);
    if (filters.categoryId) params.set('category', filters.categoryId);
    if (filters.sortBy) params.set('sort', filters.sortBy);
    if (filters.minPrice) params.set('minPrice', String(filters.minPrice));
    if (filters.maxPrice) params.set('maxPrice', String(filters.maxPrice));
    if (filters.inStockOnly) params.set('inStock', 'true');
    if (filters.page) params.set('page', String(filters.page));
    setSearchParams(params, { replace: true });
  }, [filters, setSearchParams]);

  // Fetch categories for filter sidebar
  const { data: categories } = useQuery({
    queryKey: ['categories', 'tree'],
    queryFn: () => categoryApi.getCategoryTree(),
    staleTime: 1000 * 60 * 30,
  });

  // Fetch products
  const { data: productsResponse, isLoading, error } = useQuery({
    queryKey: ['products', 'list', filters],
    queryFn: () => productApi.getProducts(filters),
    placeholderData: (previousData) => previousData,
  });

  const products = productsResponse?.content || [];
  const totalPages = productsResponse?.totalPages || 0;
  const totalElements = productsResponse?.totalElements || 0;
  const currentPage = productsResponse?.page || 0;

  const handleFilterChange = (newFilters: Partial<ProductFilters>) => {
    setFilters((prev) => ({ ...prev, ...newFilters, page: 0 }));
  };

  const handlePageChange = (page: number) => {
    setFilters((prev) => ({ ...prev, page }));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleSortChange = (sortBy: ProductFilters['sortBy']) => {
    handleFilterChange({ sortBy });
  };

  const handleCategoryChange = (categoryId: string | undefined) => {
    handleFilterChange({ categoryId });
  };

  const handlePriceChange = (_event: unknown, value: number | number[]) => {
    const val = Array.isArray(value) ? (value as [number, number]) : [value, value];
    setPriceRange(val as [number, number]);
    handleFilterChange({ minPrice: val[0] > 0 ? val[0] : undefined, maxPrice: val[1] < 1000 ? val[1] : undefined });
  };

  const handleStockChange = (checked: boolean) => {
    handleFilterChange({ inStockOnly: checked });
  };

  const clearFilters = () => {
    setFilters({
      page: 0,
      size: 20,
      sortBy: 'popular',
    });
    setPriceRange([0, 1000]);
  };

  const hasActiveFilters = filters.categoryId || filters.search || filters.minPrice || filters.maxPrice || filters.inStockOnly;

  const categoryHierarchy = categories?.filter((c) => !c.parentId) || [];

  return (
    <Box sx={{ flexGrow: 1 }}>
      {/* Page Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h3" component="h1" fontWeight={700} gutterBottom>
          Products
        </Typography>
        <Typography variant="body1" color="text.secondary">
          {totalElements} product{totalElements !== 1 ? 's' : ''} found
        </Typography>
      </Box>

      <Grid container spacing={3}>
        {/* Sidebar Filters */}
        <Grid item xs={12} md={3} lg={2}>
          {/* Mobile Filter Button */}
          <Button
            variant="outlined"
            startIcon={<FilterList />}
            fullWidth
            sx={{ mb: 3, display: { md: 'none' } }}
            onClick={() => setMobileFiltersOpen(true)}
          >
            Filters {hasActiveFilters && <Chip label={Object.keys(filters).filter(k => filters[k as keyof ProductFilters]).length} size="small" variant="outlined" />}
          </Button>

          {/* Desktop Filters */}
          <Paper sx={{ p: 2, display: { xs: 'none', md: 'block' }, position: 'sticky', top: 100, height: 'fit-content' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6" fontWeight={600}>Filters</Typography>
              {hasActiveFilters && (
                <Button size="small" onClick={clearFilters} startIcon={<Close />}>Clear All</Button>
              )}
            </Box>

            {/* Search */}
            <TextField
              fullWidth
              size="small"
              placeholder="Search products..."
              value={filters.search || ''}
              onChange={(e) => debouncedSearch(e.target.value)}
              InputProps={{
                startAdornment: <InputAdornment position="start"><SearchIcon color="action" /></InputAdornment>,
              }}
              sx={{ mb: 3 }}
            />

            {/* Categories */}
            <Accordion sx={{ mb: 2 }}>
              <AccordionSummary expandIcon={<ExpandMore />}>
                <Typography variant="subtitle1" fontWeight={600}>Categories</Typography>
              </AccordionSummary>
              <AccordionDetails>
                <List dense>
                  {categoryHierarchy.map((category) => (
                    <ListItem key={category.id} onClick={() => handleCategoryChange(category.id)}>
                      <ListItemText primary={category.name} secondary={category.productCount ? `${category.productCount} products` : undefined} />
                    </ListItem>
                  ))}
                </List>
              </AccordionDetails>
            </Accordion>

            {/* Price Range */}
            <Accordion sx={{ mb: 2 }}>
              <AccordionSummary expandIcon={<ExpandMore />}>
                <Typography variant="subtitle1" fontWeight={600}>Price Range</Typography>
              </AccordionSummary>
              <AccordionDetails>
                <Box sx={{ width: '100%' }}>
                  <Typography variant="caption" color="text.secondary" gutterBottom>
                    ${priceRange[0]} - ${priceRange[1]}
                  </Typography>
                  <Slider
                    value={priceRange}
                    onChange={handlePriceChange}
                    valueLabelDisplay="auto"
                    min={0}
                    max={1000}
                    step={10}
                  />
                </Box>
              </AccordionDetails>
            </Accordion>

            {/* In Stock */}
            <Accordion>
              <AccordionSummary expandIcon={<ExpandMore />}>
                <Typography variant="subtitle1" fontWeight={600}>Availability</Typography>
              </AccordionSummary>
              <AccordionDetails>
                <FormControlLabel
                  control={<Checkbox checked={filters.inStockOnly} onChange={(e) => handleStockChange(e.target.checked)} />}
                  label="In Stock Only"
                />
              </AccordionDetails>
            </Accordion>
          </Paper>
        </Grid>

        {/* Product Grid */}
        <Grid item xs={12} md={9} lg={10}>
          {/* Toolbar */}
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'center', mb: 3, justifyContent: 'space-between' }}>
            <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
              <Typography variant="body2" color="text.secondary">
                Showing {(currentPage * (filters.size || 20)) + 1} - {Math.min((currentPage + 1) * (filters.size || 20), totalElements)} of {totalElements}
              </Typography>
              
              <FormControl size="small" sx={{ minWidth: 180 }}>
                <InputLabel>Sort By</InputLabel>
                <Select
                  value={filters.sortBy}
                  label="Sort By"
                  onChange={(e) => handleSortChange(e.target.value as ProductFilters['sortBy'])}
                >
                  {SORT_OPTIONS.map((option) => (
                    <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>

            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button variant="outlined" size="small" startIcon={<FilterList />} onClick={() => setMobileFiltersOpen(true)}>
                Filters
              </Button>
            </Box>
          </Box>

          {/* Active Filters Chips */}
          {hasActiveFilters && (
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 3 }}>
              {filters.search && (
                <Chip label={`Search: "${filters.search}"`} onDelete={() => handleFilterChange({ search: undefined })} size="small" variant="outlined" />
              )}
              {filters.categoryId && categories && (
                <Chip
                  label={`Category: ${categories.find(c => c.id === filters.categoryId)?.name}`}
                  onDelete={() => handleFilterChange({ categoryId: undefined })}
                  size="small"
                  variant="outlined"
                />
              )}
              {(filters.minPrice || filters.maxPrice) && (
                <Chip
                  label={`Price: $${filters.minPrice || 0} - $${filters.maxPrice || 1000}`}
                  onDelete={() => { handleFilterChange({ minPrice: undefined, maxPrice: undefined }); setPriceRange([0, 1000]); }}
                  size="small"
                  variant="outlined"
                />
              )}
              {filters.inStockOnly && (
                <Chip label="In Stock Only" onDelete={() => handleFilterChange({ inStockOnly: false })} size="small" variant="outlined" />
              )}
            </Box>
          )}

          {/* Product Grid */}
          {isLoading ? (
            <Grid container spacing={3}>
              {[...Array(8)].map((_, i) => (
                <Grid item xs={6} sm={4} md={3} key={i}>
                  <Skeleton variant="rectangular" height={320} />
                </Grid>
              ))}
            </Grid>
          ) : error ? (
            <Box sx={{ textAlign: 'center', py: 6 }}>
              <Typography variant="h6" color="error" gutterBottom>Failed to load products</Typography>
              <Typography color="text.secondary" paragraph>Please try again later.</Typography>
              <Button variant="contained" onClick={() => window.location.reload()}>Retry</Button>
            </Box>
          ) : products.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 6 }}>
              <Typography variant="h6" color="text.secondary" gutterBottom>No products found</Typography>
              <Typography color="text.secondary" paragraph>Try adjusting your filters or search terms.</Typography>
              <Button variant="outlined" onClick={clearFilters} sx={{ mt: 2 }}>Clear Filters</Button>
            </Box>
          ) : (
            <>
              <Grid container spacing={3}>
                {products.map((product: { id: string; name: string; price: number; currency: string; thumbnail: string; slug: string; categoryName?: string; categoryId: string; stockQuantity: number; isActive: boolean }) => (
                  <Grid item xs={6} sm={4} md={3} key={product.id}>
                    <ProductCardComponent product={product} />
                  </Grid>
                ))}
              </Grid>

              {/* Pagination */}
              {totalPages > 1 && (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
                  <Pagination
                    count={totalPages}
                    page={currentPage + 1}
                    onChange={(_, page) => handlePageChange(page - 1)}
                    color="primary"
                    showFirstButton
                    showLastButton
                    boundaryCount={1}
                    siblingCount={1}
                  />
                </Box>
              )}
            </>
          )}
        </Grid>
      </Grid>

      {/* Mobile Filters Drawer */}
      <Drawer
        variant="temporary"
        open={mobileFiltersOpen}
        onClose={() => setMobileFiltersOpen(false)}
        ModalProps={{ keepMounted: true }}
        sx={{
          display: { md: 'none' },
          '& .MuiDrawer-paper': { boxSizing: 'border-box', width: '90vw', maxWidth: 320 },
        }}
      >
        <Box sx={{ p: 2 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" fontWeight={600}>Filters</Typography>
            <IconButton onClick={() => setMobileFiltersOpen(false)}><Close /></IconButton>
          </Box>
          
          <TextField
            fullWidth
            size="small"
            placeholder="Search products..."
            value={filters.search || ''}
            onChange={(e) => debouncedSearch(e.target.value)}
            InputProps={{
              startAdornment: <InputAdornment position="start"><SearchIcon color="action" /></InputAdornment>,
            }}
            sx={{ mb: 3 }}
          />

          <Divider sx={{ mb: 2 }} />
          
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>Categories</Typography>
          <List dense>
            <ListItem onClick={() => { handleCategoryChange(undefined); setMobileFiltersOpen(false); }}>
              <ListItemText primary="All Categories" />
            </ListItem>
            {categoryHierarchy.map((category) => (
              <ListItem key={category.id} onClick={() => { handleCategoryChange(category.id); setMobileFiltersOpen(false); }}>
                <ListItemText primary={category.name} secondary={category.productCount ? `${category.productCount} products` : undefined} />
              </ListItem>
            ))}
          </List>

          <Divider sx={{ my: 2 }} />

          <Typography variant="subtitle1" fontWeight={600} gutterBottom>Price Range</Typography>
          <Box sx={{ width: '100%' }}>
            <Typography variant="caption" color="text.secondary" gutterBottom>
              ${priceRange[0]} - ${priceRange[1]}
            </Typography>
            <Slider
              value={priceRange}
              onChange={handlePriceChange}
              valueLabelDisplay="auto"
              min={0}
              max={1000}
              step={10}
            />
          </Box>

          <Divider sx={{ my: 2 }} />

          <FormControlLabel
            control={<Checkbox checked={filters.inStockOnly} onChange={(e) => handleStockChange(e.target.checked)} />}
            label="In Stock Only"
          />

          <Divider sx={{ my: 2 }} />

          {hasActiveFilters && (
            <Button variant="outlined" fullWidth onClick={clearFilters} startIcon={<Close />}>
              Clear All Filters
            </Button>
          )}
        </Box>
      </Drawer>
    </Box>
  );
}