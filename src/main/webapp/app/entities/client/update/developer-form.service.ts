import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IDeveloper, NewDeveloper } from '../developer.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IDeveloper for edit and NewDeveloperFormGroupInput for create.
 */
type DeveloperFormGroupInput = IDeveloper | PartialWithRequiredKeyOf<NewDeveloper>;

type DeveloperFormDefaults = Pick<NewDeveloper, 'id'>;

type DeveloperFormGroupContent = {
  id: FormControl<IDeveloper['id'] | NewDeveloper['id']>;
  streetAddress: FormControl<IDeveloper['streetAddress']>;
  postalCode: FormControl<IDeveloper['postalCode']>;
  city: FormControl<IDeveloper['city']>;
  stateProvince: FormControl<IDeveloper['stateProvince']>;
};

export type DeveloperFormGroup = FormGroup<DeveloperFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class DeveloperFormService {
  createDeveloperFormGroup(developer: DeveloperFormGroupInput = { id: null }): DeveloperFormGroup {
    const developerRawValue = {
      ...this.getFormDefaults(),
      ...developer,
    };
    return new FormGroup<DeveloperFormGroupContent>({
      id: new FormControl(
        { value: developerRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        }
      ),
      streetAddress: new FormControl(developerRawValue.streetAddress),
      postalCode: new FormControl(developerRawValue.postalCode),
      city: new FormControl(developerRawValue.city),
      stateProvince: new FormControl(developerRawValue.stateProvince),
    });
  }

  getDeveloper(form: DeveloperFormGroup): IDeveloper | NewDeveloper {
    return form.getRawValue() as IDeveloper | NewDeveloper;
  }

  resetForm(form: DeveloperFormGroup, developer: DeveloperFormGroupInput): void {
    const developerRawValue = { ...this.getFormDefaults(), ...developer };
    form.reset(
      {
        ...developerRawValue,
        id: { value: developerRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */
    );
  }

  private getFormDefaults(): DeveloperFormDefaults {
    return {
      id: null,
    };
  }
}
