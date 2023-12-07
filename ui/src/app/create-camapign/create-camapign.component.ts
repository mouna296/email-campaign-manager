import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router,NavigationEnd } from '@angular/router';


@Component({
  selector: 'app-create-camapign',
  templateUrl: './create-camapign.component.html',
  styleUrls: ['./create-camapign.component.css']
})
export class CreateCamapignComponent implements OnInit {

  errorMessages: any = {};

  campaign = {
    title: '',
    about: '',
    start_at: '',
    end_at: '',
    repeat_period: 'P1Y6M4DT1230M5S',
    sendgrid_domain: 'gmail.com',
    repeat_threshold: 0,
    sql: '',
    mail: {
      mail_type: 'static',
      subject: '',
      content: '',
      content_type: 'html',
      from: { name: '', email: 'teamuntitled.272@gmail.com' },
      reply_to: { name: '', email: 'teamuntitled.272@gmail.com' }
    }
  };


  constructor(private http: HttpClient, private router: Router) {}
  navigate(){
  this.router.navigate(['/table-list']);
  // window.location.href = '/table-list';
  }

  onSubmit() {
    const apiUrl = 'https://40e2-2601-646-a100-cbf0-9cd8-4759-366f-faf1.ngrok-free.app/campaigns';
  
    this.http.post(apiUrl, this.campaign).subscribe(
      response => {
        console.log('Campaign created:', response);
  
        // Redirecting to the campaign list after successful response
        this.router.navigate(['/table-list']);
      },
      error => {
        console.error('Error creating campaign:', error);
  
        // Handling error, like showing an error message
        this.handleError(error);
      }
    );
  }
  
  handleError(error: any) {
    // Assuming the error response is in JSON format and has a field 'errors'
    if (error && error.errors) {
      this.errorMessages = error.errors;
    } else {
      this.errorMessages.general = 'An error occurred while creating the campaign.';
    }
  }

  ngOnInit(): void {
  }

}
