import { NgModule } from '@angular/core';
import { CommonModule, } from '@angular/common';
import { BrowserModule  } from '@angular/platform-browser';
import { Routes, RouterModule } from '@angular/router';
import { LoginComponent } from './login/login.component';

import { AdminLayoutComponent } from './layouts/admin-layout/admin-layout.component';
import { SingupComponent } from './singup/singup.component';
import { LandingComponent } from './landing/landing.component';

const routes: Routes =[
  {
    path: '',
    redirectTo: 'table-liist',
    pathMatch: 'full',
  }, 
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
        {
      path: '',
      loadChildren: () => import('./layouts/admin-layout/admin-layout.module').then(x=>x.AdminLayoutModule)
  }]},
  {
    path: '**',
    redirectTo: 'dashboard'
  },
  {
    path: 'login',
    component: LoginComponent
  
  },
  {
    path: 'signup',
    component: SingupComponent
  
  },
  {
    path: 'landing',
    component: LandingComponent
  }

];

@NgModule({
  imports: [
    CommonModule,
    BrowserModule,
    RouterModule.forRoot(routes)
  ],
  exports: [
  ],
})
export class AppRoutingModule { }
