import { Routes } from '@angular/router';
import { AuthShellComponent } from './auth/auth-shell.component';
import { LoginComponent } from './auth/login.component';
import { RecuperarComponent } from './auth/recuperar.component';
import { RegistroComponent } from './auth/registro.component';
import { ResetPasswordComponent } from './auth/reset-password.component';
import { VerificarRegistroComponent } from './auth/verificar-registro.component';
import { authGuard } from './core/auth.guard';
import { ComisionistaDashboardComponent } from './comisionista/comisionista-dashboard.component';
import { DashboardComponent } from './dashboard/dashboard.component';

export const routes: Routes = [
  {
    path: '',
    component: AuthShellComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'login' },
      { path: 'login', component: LoginComponent },
      { path: 'registro', component: RegistroComponent },
      { path: 'verificar-registro', component: VerificarRegistroComponent },
      { path: 'recuperar', component: RecuperarComponent },
      { path: 'reset-password', component: ResetPasswordComponent },
    ],
  },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'comisionista', component: ComisionistaDashboardComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: 'login' },
];
