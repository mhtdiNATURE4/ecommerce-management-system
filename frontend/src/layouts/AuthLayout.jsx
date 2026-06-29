import { Navigate, Outlet } from 'react-router-dom';
import { getToken, getUserRole } from '../services/auth';
import { isAdminRole } from '../navigation/navigation';

function AuthLayout() {
  const token = getToken();
  const role = getUserRole();

  if (!token) {
    return <Outlet />;
  }

  if (isAdminRole(role)) {
    return <Navigate to="/admin/dashboard" replace />;
  }

  return <Navigate to="/" replace />;
}

export default AuthLayout;
