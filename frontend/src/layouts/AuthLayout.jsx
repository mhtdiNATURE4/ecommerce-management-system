import { Navigate, NavLink, Outlet } from 'react-router-dom';
import { getToken, getUserRole } from '../services/auth';
import { isAdminRole, resolveNavigation } from '../navigation/navigation';

function AuthLayout() {
  const token = getToken();
  const role = getUserRole();

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
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>
        </header>

        <main>
          <Outlet />
        </main>
      </div>
    );
  }

  if (isAdminRole(role)) {
    return <Navigate to="/admin/dashboard" replace />;
  }

  return <Navigate to="/" replace />;
}

export default AuthLayout;
