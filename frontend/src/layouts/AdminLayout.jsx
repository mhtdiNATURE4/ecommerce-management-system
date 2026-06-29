import { Navigate, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { clearToken, getToken, getUserRole } from '../services/auth';
import { isAdminRole, resolveNavigation } from '../navigation/navigation';

function AdminLayout() {
  const navigate = useNavigate();
  const role = getUserRole();
  const navigation = resolveNavigation(role, Boolean(getToken()));

  if (!isAdminRole(role)) {
    return <Navigate to="/" replace />;
  }

  function handleLogout() {
    clearToken();
    navigate('/login', { replace: true });
  }

  return (
    <div>
      <header className="header-root">
        <div className="header-inner">
          <div className="header-brand">
            <img className="header-logo" src="/favicon.svg" alt="iStore logo" />
            <div className="admin-title-group">
              <h1>Admin Panel</h1>
              <span className="admin-badge">Admin</span>
            </div>
          </div>

          <nav className="header-nav admin-nav">
            {navigation.map((item) => {
              if (item.kind === 'button') {
                return (
                  <button key={item.key} type="button" onClick={handleLogout} className="nav-link nav-link-button admin-logout-button">
                    {item.label}
                  </button>
                );
              }

              return (
                <NavLink
                  key={item.key}
                  to={item.to}
                  className={({ isActive }) => `nav-link admin-nav-link ${isActive ? 'active' : ''}`}
                >
                  {item.label}
                </NavLink>
              );
            })}
          </nav>
        </div>
      </header>

      <main>
        <Outlet />
      </main>

      <footer style={{ padding: '1rem', borderTop: '1px solid #ddd', background: '#fff' }}>
        <p>Administrative tools.</p>
      </footer>
    </div>
  );
}

export default AdminLayout;
