import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ChartData, ChartOptions, ChartType } from 'chart.js';
import { ApiService } from '../services/api.service';


@Component({
  selector: 'app-analytics',
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.css']
})
export class AnalyticsComponent implements OnInit {


  pieChartData: ChartData;
  barChartData: ChartData;
  deliveryBreakdownChartData: ChartData;
  deliveryBreakdownChartOptions: ChartOptions;
  barChartOptions: ChartOptions;
  campaignId = ''; 

  constructor(private apiservice: ApiService, private route: ActivatedRoute) { }

  ngOnInit() {
    this.route.params.subscribe(params => {
      this.campaignId = params['campaignId']; // make sure 'campaignId' matches the name of the parameter defined in your route
      if (this.campaignId) {
        this.loadPieChartData(this.campaignId);
        this.loadBarChartData(this.campaignId);
      }
    });
  }

  loadPieChartData(campaignId: string) {
    this.apiservice.getDeliveryBreakdown(campaignId).subscribe(data => {
      if (data) {
        console.log(data);
        this.deliveryBreakdownChartData = {
          labels: ['Open', 'Delivered', 'Sent', 'Failed'],
          datasets: [
            {
              data: [data.open, data.delivered, data.sent, data.other],
              backgroundColor: ['#FF6384', '#FF6384', '#FF6384', '#FF6384'],
              label: 'Delivery Status'
            }
          ]
        };
        this.deliveryBreakdownChartOptions = {
          scales: {
            y: {
              beginAtZero: true,
              max: 20
            
            }
          }
        };
      }
    });
  }

  loadBarChartData(campaignId: string) {
  //   this.apiservice.getSuccessFailureBreakdown(campaignId).subscribe(data => {
  //     console.log (data)
  //     this.barChartData = {
       
  //       labels: ['Success', 'Failure'],
  //       datasets: [{
  //         label: 'Count',
  //         data: [data.successCount, data.failureCount]
  //       }]
  //     };
  //   });
  // }
  this.apiservice.getSuccessFailureBreakdown(campaignId).subscribe(data => {
    console.log(data);
    this.barChartData = {
      labels: ['Metrics'],
      datasets: [
        {
          label: 'Success',
          data: [data.successCount],
          backgroundColor: 'green',
          stack: 'Stack 0',
        },
        {
          label: 'Failure',
          data: [data.failureCount],
          backgroundColor: 'red',
          stack: 'Stack 0',
        },
        {
          label: 'Total',
          data: [data.total],
          backgroundColor: 'blue',
          stack: 'Stack 1',
        }
      ]
    };

    this.barChartOptions = {
      scales: {
        x: {
          stacked: true,
        },
        y: {
          stacked: true,
          max: 40
        }
      }
    };
  });
}
}