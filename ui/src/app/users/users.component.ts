import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

interface User {
  id: number;
  email: string;
  role: string;
}


@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css']
})
export class UsersComponent implements OnInit {

  users: User[] = []; // To store the user data

  constructor(private http: HttpClient) { }

  ngOnInit(): void {
    this.fetchUsers();
  }

  fetchUsers() {

    const email = localStorage.getItem('email');
    const role = localStorage.getItem('role');

    let headers = new HttpHeaders();
    if (email) {
      headers = headers.set('X-User-Email', email);
    }
  
    if (role) {
      headers = headers.set('X-User-Role', role);
    }

    const apiUrl = 'https://40e2-2601-646-a100-cbf0-9cd8-4759-366f-faf1.ngrok-free.app/users'; // Replace with your API endpoint
    this.http.get<User[]>(apiUrl, { headers: headers }).subscribe(
      data => {
        this.users = data;
      },
      error => {
        console.error('Error fetching users:', error);
      }
    );
  }
  mapUserRole(role: string): string {
    switch (role) {
      case 'campaign_supervisor':
        return 'Campaign Supervisor';
      case 'campaign_manager':
        return 'Campaign Manager';
      case 'super_user':
        return 'Super User';
      default:
        return 'Unknown Role';
    }
  }

  deleteUser(userId: number) {
    const deleteApiUrl = `YOUR_API_ENDPOINT_FOR_DELETING_USER/${userId}`; // Replace with your API endpoint
    this.http.delete(deleteApiUrl).subscribe(
      response => {
        console.log('User deleted:', response);
        // Optionally, refresh the user list after deletion
        this.fetchUsers();
      },
      error => {
        console.error('Error deleting user:', error);
      }
    );
  }

}
