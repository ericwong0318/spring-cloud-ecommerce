# E-Commerce Frontend

A modern, responsive e-commerce storefront built with React 19, TypeScript, Material UI, and Vite.

## Features

- **Authentication**: OAuth2 Authorization Code + PKCE flow with Spring Auth Server
- **State Management**: React Context + useReducer (global state) + TanStack Query (server state)
- **Forms**: React Hook Form + Zod validation
- **UI**: Material UI v6 with custom theme, responsive design
- **API Integration**: Axios client with interceptors, auto token refresh, error handling
- **Cart**: Optimistic updates, guest cart with localStorage sync
- **Responsive**: Mobile-first design with MUI breakpoints

## Tech Stack

- **Framework**: React 19 + TypeScript + Vite
- **UI**: Material UI v6 + MUI X (Data Grid, Date Pickers)
- **State**: React Context + useReducer + TanStack Query v5
- **Forms**: React Hook Form + Zod + @hookform/resolvers
- **Routing**: React Router DOM v6
- **HTTP**: Axios with interceptors
- **Date**: Day.js
- **Styling**: MUI theme + CSS

## Project Structure

```
frontend/
├── public/
├── src/
│   ├── components/          # Shared UI components
│   │   ├── Button.tsx       # Styled button variants
│   │   ├── Input.tsx        # Form field components
│   │   ├── Card.tsx         # ProductCard, InfoCard
│   │   ├── Header.tsx       # App header with nav, search, cart
│   │   ├── Footer.tsx       # App footer with links
│   │   └── Layout.tsx       # Layout wrappers
│   ├── hooks/               # Shared custom hooks
│   │   ├── useAuth.ts
│   │   ├── useCart.ts
│   │   ├── useNotification.ts
│   │   ├── useDebounce.ts
│   │   ├── useLocalStorage.ts
│   │   └── useMediaQuery.ts
│   ├── services/            # API layer
│   │   ├── api.ts           # Axios client with interceptors
│   │   ├── endpoints.ts     # API endpoint functions
│   │   └── index.ts
│   ├── types/               # TypeScript types
│   │   ├── api.ts           # API response types
│   │   ├── domain.ts        # Domain models
│   │   └── index.ts
│   ├── context/             # React Context providers
│   │   ├── AuthContext.tsx
│   │   ├── CartContext.tsx
│   │   └── NotificationContext.tsx
│   ├── pages/               # Page components
│   │   ├── Home.tsx
│   │   ├── ProductList.tsx
│   │   ├── ProductDetail.tsx
│   │   ├── Cart.tsx
│   │   ├── Checkout.tsx
│   │   ├── CheckoutSuccess.tsx
│   │   ├── Orders.tsx
│   │   ├── OrderDetail.tsx
│   │   ├── Account.tsx
│   │   ├── Login.tsx
│   │   ├── Register.tsx
│   │   └── Callback.tsx
│   ├── utils/               # Utilities
│   │   ├── auth.ts          # Auth utilities
│   │   ├── formatters.ts    # Formatting helpers
│   │   ├── validators.ts    # Zod schemas
│   │   ├── constants.ts     # App constants
│   │   └── index.ts
│   ├── styles/              # Global styles & theme
│   │   ├── theme.ts
│   │   └── global.css
│   ├── App.tsx              # Root component with routes
│   ├── main.tsx             # Entry point
│   └── vite-env.d.ts
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── .eslintrc.json
├── .prettierrc
└── README.md
```

## Development

### Prerequisites

- Node.js 20+
- npm or yarn

### Installation

```bash
cd frontend
npm install
```

### Available Scripts

```bash
# Development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint
npm run lint

# Format
npm run format

# Type check
npm run typecheck
```

### Environment Variables

Create a `.env` file in the frontend directory:

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_AUTH_SERVER_URL=http://localhost:9000
VITE_CLIENT_ID=frontend-client
VITE_REDIRECT_URI=http://localhost:5173/callback
```

## API Integration

### Endpoints

| Domain | Gateway Path | Service |
|--------|-------------|---------|
| Products | `/api/v1/products/**` | product-service (8081) |
| Categories | `/api/v1/categories/**` | category-service (8082) |
| Orders | `/api/v1/orders/**` | order-service (8083) |
| Inventory | `/api/v1/inventory/**` | inventory-service (8084) |
| Payments | `/api/v1/payments/**` | payment-service (8086) |
| Cart | `/api/v1/cart/**` | order-service |

### Auth Flow (PKCE)

1. `initiateOAuthLogin()` generates PKCE code_verifier/code_challenge
2. Redirects to auth-server at `/oauth2/authorize`
3. User authenticates at auth-server
4. Redirects back to `/callback?code=...&state=...`
5. `handleCallback()` exchanges code for tokens
6. Tokens stored in memory (access) + httpOnly cookie (refresh)
7. Axios interceptor adds Bearer token to API calls
8. Auto-refresh on 401, redirect to login on refresh failure

## State Management

### Global State (Context + useReducer)

- **AuthContext**: User info, tokens, login/logout, role checking
- **CartContext**: Cart items, optimistic updates, guest cart sync
- **NotificationContext**: Toast/snackbar notifications

### Server State (TanStack Query)

- Products (list, detail, search, filters)
- Categories (tree/list)
- Orders (list, detail, create)
- User profile/addresses
- Checkout session

## Forms & Validation

React Hook Form + Zod schemas for:
- Login/Register
- Checkout (shipping address, payment method)
- Profile/Address forms
- Product search filters

## Responsive Design

- **Breakpoints**: MUI default (xs, sm, md, lg, xl)
- **Mobile-first**: Product grid stacks, cart drawer on mobile
- **Accessibility**: Semantic HTML, ARIA labels, focus management, keyboard navigation

## Theme

Custom MUI theme with:
- Light/dark mode support
- Custom primary (#1976d2) and secondary (#9c27b0) colors
- Custom typography (Inter font)
- Consistent spacing, border radius, shadows

## Deployment

```bash
# Build
npm run build

# Output: dist/
# Serve dist/ with any static file server
```

## License

MIT