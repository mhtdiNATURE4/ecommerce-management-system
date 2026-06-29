import { Navigate, Outlet } from 'react-router-dom';
import { getToken, getUserRole } from '../services/auth';
import { isAdminRole } from '../navigation/navigation';

function MainLayout() {
  const token = getToken();
  const role = getUserRole();

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (isAdminRole(role)) {
    return <Navigate to="/admin/dashboard" replace />;
  }

  return <Outlet />;
}

export default MainLayout;
