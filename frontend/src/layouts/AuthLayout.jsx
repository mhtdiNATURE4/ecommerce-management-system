import { Navigate, NavLink, Outlet } from 'react-router-dom';
import { getToken, getUserRole } from '../services/auth';
import { isAdminRole, resolveNavigation } from '../navigation/navigation';
import { Home, ShoppingBag, LogIn, UserPlus } from 'lucide-react';

function AuthLayout() {
  const token = getToken();
  const role = getUserRole();

  const navIcons = {
    home: <Home size={15} />,
    products: <ShoppingBag size={15} />,
    login: <LogIn size={15} />,
    register: <UserPlus size={15} />
  };

  if (!token) {
    const navigation = resolveNavigation(role, false);

    return (
      <div>
        <header className="header-root">
          <div className="header-inner">
            <div className="header-brand">
              <img className="header-logo" src="/favicon.svg" alt="iStore logo" />
              <h1>iStore</h1>
            </div>

            <nav className="header-nav">
              {navigation.map((item) => (
                <NavLink
                  key={item.key}
                  to={item.to}
                  className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                >
                  <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                    {navIcons[item.key]}
                    {item.label}
                  </span>
                </NavLink>
              ))}
            </nav>
          </div>
        </header>

        <main>
          <Outlet />
        </main>

        <footer style={{ padding: '1rem', borderTop: '1px solid var(--border)', background: 'var(--surface)' }}>
          <p>iStore © 2025</p>
        </footer>
      </div>
    );
  }

  if (isAdminRole(role)) {
    return <Navigate to="/admin/dashboard" replace />;
  }

  return <Navigate to="/" replace />;
}

export default AuthLayout;
