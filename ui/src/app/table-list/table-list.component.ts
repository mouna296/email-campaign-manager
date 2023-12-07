import { Component, OnInit } from '@angular/core';
import { HttpClient , HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr'; 


@Component({
  selector: 'app-table-list',
  templateUrl: './table-list.component.html',
  styleUrls: ['./table-list.component.css']
})
export class TableListComponent implements OnInit {
  campaigns: any[] = [];
  currentPage: number = 1;
  pageSize: number = 10; // Change as needed
  totalCampaigns: number = 0;
  Math: any = Math;

  hover = false;

  constructor(private http: HttpClient, private router: Router,private toastr: ToastrService) {
    this.router.events.subscribe((event) => {
      console.log(event);
    });
  }

  ngOnInit() {
    this.fetchCampaigns(this.currentPage, this.pageSize );
  }

  goToCampaignAnalytics(campaignId: string) {
    this.router.navigate(['/analytics', campaignId]);
  }

  navigateToCreateCampaign() {
    this.router.navigate(['/create-campaign']); // Adjust the route as per your app's routing configuration
  }



  fetchCampaigns(page: number, pageSize: number) {
  const fetchapiUrl='https://40e2-2601-646-a100-cbf0-9cd8-4759-366f-faf1.ngrok-free.app/campaigns';

  const email = localStorage.getItem('email');
  const role = localStorage.getItem('role');

  let headers = new HttpHeaders();
  if (email) {
    headers = headers.set('X-User-Email', email);
  }
  
  if (role) {
    headers = headers.set('X-User-Role', role);
  }

   this.http.get<any>(fetchapiUrl, { headers: headers }).subscribe(
      (response) => {
        this.campaigns = response; // Adjust according to your API response structure
        this.totalCampaigns = this.campaigns.length; // Total number of campaigns, for pagination calculation
        this.campaigns = this.campaigns.sort((a, b) => a.id - b.id);

      },
      (error) => {
        console.error('Error fetching data:', error);
      }
    );
  }
  onPageChange(newPage: number) {
    this.currentPage = newPage;
    this.fetchCampaigns(this.currentPage, this.pageSize);
  }
  publishCampaign(event: Event, campaign: any) {
    const checkbox = event.target as HTMLInputElement;


    const email = localStorage.getItem('email');
    const role = localStorage.getItem('role');

    let headers = new HttpHeaders();
    if (email) {
        headers = headers.set('X-User-Email', email);
    } 
  
    if (role) {
      headers = headers.set('X-User-Role', role);
    }


    if (checkbox.checked) {
      const apiUrl = `https://40e2-2601-646-a100-cbf0-9cd8-4759-366f-faf1.ngrok-free.app/schedulers/schedule-campaign/${campaign.id}`;
      this.http.post(apiUrl,null, { headers: headers }).subscribe(
        (response) => {
          console.log('Campaign published:', response);
          // Handle successful publishing here (e.g., show a success message)
        },
        (error) => {
          console.error('Error publishing campaign:', error);
          // Handle errors here (e.g., show an error message)
        }
      );
    }
    else{
      const apiUrl = `https://40e2-2601-646-a100-cbf0-9cd8-4759-366f-faf1.ngrok-free.app/schedulers/unschedule-campaign/${campaign.id}`;
      this.http.post(apiUrl,null, { headers: headers }).subscribe(
        (response) => {
          console.log('Campaign published:', response);
          // Handle successful publishing here (e.g., show a success message)
        },
        (error) => {
          console.error('Error publishing campaign:', error);
          // Handle errors here (e.g., show an error message)
        }
      );
    }
  }
  archiveCampaign(campaign: any) {
    const email = localStorage.getItem('email');
    const role = localStorage.getItem('role');

    let headers = new HttpHeaders();
    if (email) {
        headers = headers.set('X-User-Email', email);
    } 
  
    if (role) {
      headers = headers.set('X-User-Role', role);
    }


    const archiveApiUrl = `https://40e2-2601-646-a100-cbf0-9cd8-4759-366f-faf1.ngrok-free.app/campaigns/archive/${campaign.id}`;
    this.http.post(archiveApiUrl, null, { headers: headers }).subscribe(
      (response) => {
        console.log('Campaign archived:', response);
        // Handle successful archiving here (e.g., show a success message or refresh the list)
        this.fetchCampaigns(this.currentPage, this.pageSize);
         this.toastr.success('Campaign successfully archived', 'Success'); 
      },
      (error) => {
        console.error('Error archiving campaign:', error);
        // Handle errors here (e.g., show an error message)
      }
    );
  }

  approveCampaign(campaign: any) {
    const email = localStorage.getItem('email');
    const role = localStorage.getItem('role');

    let headers = new HttpHeaders();
    if (email) {
        headers = headers.set('X-User-Email', email);
    } 
  
    if (role) {
      headers = headers.set('X-User-Role', role);
    }

    const approveApiUrl = `https://40e2-2601-646-a100-cbf0-9cd8-4759-366f-faf1.ngrok-free.app/campaigns/approve/${campaign.id}`;
    this.http.post(approveApiUrl, null, { headers: headers }).subscribe(
      (response) => {
        console.log('Campaign approved:', response);
        // Handle successful approval here (e.g., show a success message or update the status)
      },
      (error) => {
        console.error('Error approving campaign:', error);
        // Handle errors here (e.g., show an error message)
      }
    );
  }

  executeCampaignNow(campaign: any) {
    const email = localStorage.getItem('email');
    const role = localStorage.getItem('role');

    let headers = new HttpHeaders();
    if (email) {
        headers = headers.set('X-User-Email', email);
    } 
  
    if (role) {
      headers = headers.set('X-User-Role', role);
    }

    const executeCampaignNowUrl = `https://40e2-2601-646-a100-cbf0-9cd8-4759-366f-faf1.ngrok-free.app/schedulers/execute-campaign-now/${campaign.id}`;
    this.http.post(executeCampaignNowUrl, null, { headers: headers }).subscribe(
      (response) => {
        console.log('Campaign Executing Now:', response);
        this.fetchCampaigns(this.currentPage, this.pageSize);
        // Handle successful archiving here (e.g., show a success message or refresh the list)
      },
      (error) => {
        console.error('Error Executing Campaign:', error);
        // Handle errors here (e.g., show an error message)
      }
    );
  }

  isCampaignSupervisor(): boolean {
    return localStorage.getItem('role') !== null && localStorage.getItem('role') == 'campaign_supervisor';
  }
}
