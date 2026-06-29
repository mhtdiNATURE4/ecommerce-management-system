import { Navigate, Outlet } from 'react-router-dom';
import { isAdminAuthenticated } from '../services/auth';

function ProtectedRoute() {
  return isAdminAuthenticated() ? <Outlet /> : <Navigate to="/login" replace />;
}

export default ProtectedRoute;
