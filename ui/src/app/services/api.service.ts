import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

interface DeliveryBreakdown {
  open: number;
  delivered: number;
  sent: number;
  other: number;
}

interface SuccessFailureData {
  successCount: number;
  failureCount: number;
  total: number;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  constructor(private http: HttpClient) { }

  getDeliveryBreakdown(campaignId: string) {
    console.log("API hit success");
    return this.http.get<DeliveryBreakdown>(`https://40e2-2601-646-a100-cbf0-9cd8-4759-366f-faf1.ngrok-free.app/campaigns/${campaignId}/analytics/delivery-breakdown`);
  }

  getSuccessFailureBreakdown(campaignId: string) {
    return this.http.get<SuccessFailureData>(`https://40e2-2601-646-a100-cbf0-9cd8-4759-366f-faf1.ngrok-free.app/campaigns/${campaignId}/analytics/success-failure-breakdown`);
  }

  getCampaignById(campaignId: string): Observable<any> {
    console.log(campaignId)
    return this.http.get(`https://40e2-2601-646-a100-cbf0-9cd8-4759-366f-faf1.ngrok-free.app/campaigns/${campaignId}`);
  }
}
