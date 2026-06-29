export function isAdminRole(role) {
  const normalized = String(role || '').toUpperCase();
  return normalized === 'ADMIN' || normalized === 'ROLE_ADMIN';
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
      { key: 'reports', label: 'Reports', to: '/admin/reports' },
      { key: 'recommendations', label: 'Recommendation Analytics', to: '/admin/recommendations' },
      { key: 'segmentation', label: 'Customer Segmentation', to: '/admin/segments' },
      { key: 'logout', label: 'Logout', to: '/login', kind: 'button' }
    ];
  }

  return [
    { key: 'home', label: 'Home', to: '/' },
    { key: 'products', label: 'Products', to: '/products' },
    { key: 'cart', label: 'Cart', to: '/cart' },
    { key: 'orders', label: 'Orders', to: '/orders' },
    { key: 'logout', label: 'Logout', to: '/login', kind: 'button' }
  ];
}
