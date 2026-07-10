import { Outlet } from 'react-router-dom';
import AdminNav from './AdminNav';

export default function AdminLayout() {
  return (
    <>
      <div className="admin-layout-band">
        <div className="container">
          <AdminNav />
        </div>
      </div>
      <div className="container pb-4">
        <Outlet />
      </div>
    </>
  );
}
