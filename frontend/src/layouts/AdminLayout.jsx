import { Navigate, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { clearToken, getToken, getUserRole } from '../services/auth';
import { isAdminRole, resolveNavigation } from '../navigation/navigation';
import { LayoutDashboard, ClipboardList, BarChart2, FileText, LogOut } from 'lucide-react';

function AdminLayout() {
  const navigate = useNavigate();
  const role = getUserRole();
  const navigation = resolveNavigation(role, Boolean(getToken()));

  const navIcons = {
    dashboard: <LayoutDashboard size={15} />,
    orders: <ClipboardList size={15} />,
    analytics: <BarChart2 size={15} />,
    reports: <FileText size={15} />,
    logout: <LogOut size={15} />
  };

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
            <h1>Admin Panel</h1>
            <span className="admin-badge">Admin</span>
          </div>

          <div className="header-actions">
            <p className="header-greeting">Management workspace</p>
            <nav className="header-nav admin-nav">
              {navigation.map((item) => {
                if (item.kind === 'button') {
                  return (
                    <button key={item.key} type="button" onClick={handleLogout} className="nav-link nav-link-button admin-logout-button">
                      <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                        {navIcons[item.key]}
                        {item.label}
                      </span>
                    </button>
                  );
                }

                return (
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
                );
              })}
            </nav>
          </div>
        </div>
      </header>

      <main>
        <Outlet />
      </main>

      <footer style={{ padding: '1rem', borderTop: '1px solid var(--border)', background: 'var(--surface)' }}>
        <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--muted)' }}>iStore © 2025</p>
      </footer>
    </div>
  );
}

export default AdminLayout;
