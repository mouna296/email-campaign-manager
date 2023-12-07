import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateCamapignComponent } from './create-camapign.component';

describe('CreateCamapignComponent', () => {
  let component: CreateCamapignComponent;
  let fixture: ComponentFixture<CreateCamapignComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CreateCamapignComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CreateCamapignComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
