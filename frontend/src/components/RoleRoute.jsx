import { Navigate, Outlet } from 'react-router-dom';
import { getUserRole } from '../services/auth';

function RoleRoute({ allowedRoles, redirectTo = '/login' }) {
  const role = getUserRole();

  const isAllowed = allowedRoles.some((candidate) => {
    const normalized = String(candidate || '').toUpperCase();
    const current = String(role || '').toUpperCase();
    return current === normalized || current === `ROLE_${normalized}`;
  });

  if (!role) {
    return <Navigate to={redirectTo} replace />;
  }

  return isAllowed ? <Outlet /> : <Navigate to={redirectTo} replace />;
}

export default RoleRoute;
