import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { DeveloperFormService, DeveloperFormGroup } from './developer-form.service';
import { IDeveloper } from '../developer.model';
import { DeveloperService } from '../service/developer.service';

@Component({
  selector: 'jhi-developer-update',
  templateUrl: './developer-update.component.html',
})
export class DeveloperUpdateComponent implements OnInit {
  isSaving = false;
  developer: IDeveloper | null = null;

  editForm: DeveloperFormGroup = this.developerFormService.createDeveloperFormGroup();

  constructor(
    protected developerService: DeveloperService,
    protected developerFormService: DeveloperFormService,
    protected activatedRoute: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ developer }) => {
      this.developer = developer;
      if (developer) {
        this.updateForm(developer);
      }
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const developer = this.developerFormService.getDeveloper(this.editForm);
    if (developer.id !== null) {
      this.subscribeToSaveResponse(this.developerService.update(developer));
    } else {
      this.subscribeToSaveResponse(this.developerService.create(developer));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IDeveloper>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(developer: IDeveloper): void {
    this.developer = developer;
    this.developerFormService.resetForm(this.editForm, developer);
  }
}
