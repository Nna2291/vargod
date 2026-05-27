// View layer — корневой компонент: каркас приложения и маршрутизация.

import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { NavBar } from './view/components/NavBar';
import { AuthBootstrap } from './view/components/auth/AuthBootstrap';
import { RequireRole } from './view/components/auth/RequireRole';
import { AdminLayout } from './view/layouts/AdminLayout';
import { CatalogPage } from './view/pages/CatalogPage';
import { ModelPage } from './view/pages/ModelPage';
import { ExperimentsPage } from './view/pages/ExperimentsPage';
import { NewExperimentPage } from './view/pages/NewExperimentPage';
import { ExperimentPage } from './view/pages/ExperimentPage';
import { ComparisonsPage } from './view/pages/ComparisonsPage';
import { ComparisonPage } from './view/pages/ComparisonPage';
import { LoginPage } from './view/pages/auth/LoginPage';
import { RegisterPage } from './view/pages/auth/RegisterPage';
import { AdminDashboardPage } from './view/pages/admin/AdminDashboardPage';
import { AdminUsersPage } from './view/pages/admin/AdminUsersPage';
import { AdminModelsPage } from './view/pages/admin/AdminModelsPage';
import { AdminValidatorsPage } from './view/pages/admin/AdminValidatorsPage';
import { AdminExperimentsPage } from './view/pages/admin/AdminExperimentsPage';
import { AdminAuditPage } from './view/pages/admin/AdminAuditPage';

export function App() {
  return (
    <BrowserRouter>
      <AuthBootstrap />
      <div className="min-h-screen bg-background">
        <NavBar />
        <Routes>
          {/* Публичные/пользовательские */}
          <Route path="/" element={<CatalogPage />} />
          <Route path="/models/:id" element={<ModelPage />} />
          <Route path="/experiments" element={<ExperimentsPage />} />
          <Route path="/experiments/new" element={<NewExperimentPage />} />
          <Route path="/experiments/:id" element={<ExperimentPage />} />
          <Route path="/comparisons" element={<ComparisonsPage />} />
          <Route path="/comparisons/:id" element={<ComparisonPage />} />

          {/* Аутентификация */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Админ-панель */}
          <Route
            path="/admin"
            element={
              <RequireRole role="ADMIN">
                <AdminLayout />
              </RequireRole>
            }
          >
            <Route index element={<AdminDashboardPage />} />
            <Route path="users" element={<AdminUsersPage />} />
            <Route path="models" element={<AdminModelsPage />} />
            <Route path="validators" element={<AdminValidatorsPage />} />
            <Route path="experiments" element={<AdminExperimentsPage />} />
            <Route path="audit" element={<AdminAuditPage />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}
