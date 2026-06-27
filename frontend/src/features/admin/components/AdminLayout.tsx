import { Outlet } from 'react-router-dom';
import AdminNav from './AdminNav';

export default function AdminLayout() {
  return (
    <div className="container py-4">
      <AdminNav />
      <Outlet />
    </div>
  );
}
