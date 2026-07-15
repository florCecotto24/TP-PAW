import { Suspense } from 'react';
import { Outlet } from 'react-router-dom';
import AdminNav from './AdminNav';
import { LoadingBlock } from '../../../components/ryden';

export default function AdminLayout() {
  return (
    <>
      <div className="admin-layout-band">
        <div className="container">
          <AdminNav />
        </div>
      </div>
      <div className="container pb-4">
        {/* Suspense boundary para las sub-páginas lazy del área admin: mantiene el
            shell + nav montados mientras carga el chunk de la sección activa. */}
        <Suspense fallback={<LoadingBlock variant="page" className="py-4" />}>
          <Outlet />
        </Suspense>
      </div>
    </>
  );
}
