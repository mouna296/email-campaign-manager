import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpHeaders, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const email = localStorage.getItem('email');
    const role = localStorage.getItem('role');

    let headers = new HttpHeaders();
    if (email) {
      headers = headers.set('X-User-Email', email);
    }
    if (role) {
      headers = headers.set('X-User-Role', role);
    }

    const authReq = req.clone({ headers });
    return next.handle(authReq);
  }
}
