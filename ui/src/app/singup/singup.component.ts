import { Component, OnInit } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';


@Component({
  selector: 'app-singup',
  templateUrl: './singup.component.html',
  styleUrls: ['./singup.component.css']
})
export class SingupComponent implements OnInit {


  focus: boolean;
  focus1: boolean;
  email: string;
  firstName: string;
  lastName: string;
  pass: string;
  error: string;

  constructor(private auth: AuthService, private router: Router) { }

  ngOnInit() {
      var navbar = document.getElementsByTagName('nav')[0];
      navbar.classList.add('navbar-transparent');
  }

  ngOnDestroy() {
      var navbar = document.getElementsByTagName('nav')[0];
      navbar.classList.remove('navbar-transparent');
  }

  signup() {
      this.error = '';
      this.auth.signup(this.email, this.pass, this.firstName, this.lastName).subscribe({
          next: (res) => {
            localStorage.setItem('email', res.email);
            localStorage.setItem('role', res.role);

            this.router.navigate(['/table-list']);
            window.location.href = '/table-list';
          },
          error: (err) => {
              console.error(err);
              this.error = "Error Registering User"; // Error handling
          }
      });
  }
}
