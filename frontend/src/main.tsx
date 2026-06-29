import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
// Estilos globales: Bootstrap 5 + íconos, luego el tema Ryden (sobrescribe Bootstrap).
// Mismo stack/orden que el sitio JSP original (header.jsp).
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap-icons/font/bootstrap-icons.css';
// Flatpickr base styles ANTES de components.css, que trae los overrides del tema
// Ryden para los calendarios inline (galería de precios por día, días reservados…).
import 'flatpickr/dist/flatpickr.css';
import 'bootstrap/dist/js/bootstrap.bundle.min.js';
import './styles/components.css';
import './styles/ryden-theme.css';
import './styles/counterparty-profile.css';
import './styles/publish-car-gallery.css';
import App from './App';
import './i18n';

const container = document.getElementById('root');
if (!container) throw new Error('Root container #root not found');

createRoot(container).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
