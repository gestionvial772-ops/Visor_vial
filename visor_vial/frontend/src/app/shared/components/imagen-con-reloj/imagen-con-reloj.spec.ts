import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ImagenConReloj } from './imagen-con-reloj';

describe('ImagenConReloj', () => {
  let component: ImagenConReloj;
  let fixture: ComponentFixture<ImagenConReloj>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImagenConReloj],
    }).compileComponents();

    fixture = TestBed.createComponent(ImagenConReloj);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
