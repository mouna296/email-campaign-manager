import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpHeaders, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const user = localStorage.getItem('user');
    const role = localStorage.getItem('role');

    let headers = new HttpHeaders();
    if (user) {
      headers = headers.set('User', user);
    }
    if (role) {
      headers = headers.set('Role', role);
    }

    const authReq = req.clone({ headers });
    return next.handle(authReq);
  }
}
