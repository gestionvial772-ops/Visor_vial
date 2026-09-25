import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CampoPassword } from './campo-password';

describe('CampoPassword', () => {
  let component: CampoPassword;
  let fixture: ComponentFixture<CampoPassword>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CampoPassword],
    }).compileComponents();

    fixture = TestBed.createComponent(CampoPassword);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
