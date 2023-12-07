import { Component, OnInit } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Router,NavigationEnd } from '@angular/router';


@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  data : Date = new Date();

  email: string = '';
  pass: string = '';
  error: string = '';
  focus: boolean = false;
  focus1: boolean = false;

  constructor(private authService: AuthService, private router: Router) {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        console.log('Navigated to:', event.url);
      }
    });
   }

  ngOnInit() {
      var navbar = document.getElementsByTagName('nav')[0];
      navbar.classList.add('navbar-transparent');
  }

  ngOnDestroy() {
      var navbar = document.getElementsByTagName('nav')[0];
      navbar.classList.remove('navbar-transparent');
  }

  login() {
    this.authService.login(this.email, this.pass).subscribe({
        next: (res) => {
            console.log("Login successful:", res);

            localStorage.setItem('email', res.email);
            localStorage.setItem('role', res.role);

            this.router.navigate(['/table-list']);
            window.location.href = '/table-list';

            // Redirect to the dashboard
        },
        error: (error) => {
            console.error(error);
            this.error = "Incorrect Credentials";
        }
    });
  }

 

}
