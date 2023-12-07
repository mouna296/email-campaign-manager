import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { tap } from 'rxjs/operators';


@Injectable({
  providedIn: 'root'
})
export class AuthService {
  public loggedin = false;

  constructor(private http: HttpClient) {}


  login(email: string, password: string) {
    return this.http.post<any>('https://40e2-2601-646-a100-cbf0-9cd8-4759-366f-faf1.ngrok-free.app/users/login-and-get-details', { email, password })
      .pipe(
        tap(res => {
          if (res && res.email) {
            this.loggedin = true;
          }
        })
      );
  }
  logout() {
    this.loggedin = false; // Reset the loggedin status
    // Perform other logout actions like clearing tokens or redirecting
  }
  signup(email: string, password: string, firstName: string, lastName: string) {
    return this.http.post<any>('https://40e2-2601-646-a100-cbf0-9cd8-4759-366f-faf1.ngrok-free.app/users/signup-and-get-details', { email, password, firstName, lastName});
  }


}
