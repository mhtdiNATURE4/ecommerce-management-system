import { isRoleAdmin } from '../services/auth';

export function isAdminRole(role) {
  return isRoleAdmin(role);
}

export function resolveNavigation(role, signedIn) {
  if (!signedIn) {
    return [
      { key: 'home', label: 'Home', to: '/' },
      { key: 'products', label: 'Products', to: '/products' },
      { key: 'login', label: 'Login', to: '/login' },
      { key: 'register', label: 'Register', to: '/register' }
    ];
  }

  if (isAdminRole(role)) {
    return [
      { key: 'dashboard', label: 'Dashboard', to: '/admin/dashboard' },
      { key: 'orders-management', label: 'Orders Management', to: '/admin/orders' },
      { key: 'analytics', label: 'Analytics', to: '/admin/analytics' },
      { key: 'reports', label: 'Reports', to: '/admin/reports' },
      { key: 'logout', label: 'Logout', to: '/login', kind: 'button' }
    ];
  }

  return [
    { key: 'home', label: 'Home', to: '/' },
    { key: 'products', label: 'Products', to: '/products' },
    { key: 'cart', label: 'Cart', to: '/cart' },
    { key: 'orders', label: 'Orders', to: '/orders' },
    { key: 'addresses', label: 'Addresses', to: '/addresses' },
    { key: 'logout', label: 'Logout', to: '/login', kind: 'button' }
  ];
}
