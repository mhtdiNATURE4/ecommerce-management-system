import { Navigate, Outlet } from 'react-router-dom';
import { getUserRole, isRoleMatch } from '../services/auth';

function RoleRoute({ allowedRoles, redirectTo = '/login' }) {
  const role = getUserRole();

  const isAllowed = allowedRoles.some((candidate) => isRoleMatch(candidate, role));

  if (!role) {
    return <Navigate to={redirectTo} replace />;
  }

  return isAllowed ? <Outlet /> : <Navigate to={redirectTo} replace />;
}

export default RoleRoute;
