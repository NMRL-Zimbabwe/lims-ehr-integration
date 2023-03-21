import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: 'developer',
        data: { pageTitle: 'limsEhrIntegrationApp.developer.home.title' },
        loadChildren: () => import('./developer/developer.module').then(m => m.DeveloperModule),
      },
      /* jhipster-needle-add-entity-route - JHipster will add entity modules routes here */
    ]),
  ],
})
export class EntityRoutingModule {}
