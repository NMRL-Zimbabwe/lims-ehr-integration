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
      {
        path: 'client',
        data: { pageTitle: 'limsEhrIntegrationApp.client.home.title' },
        loadChildren: () => import('./client/client.module').then(m => m.ClientModule),
      },
      /* jhipster-needle-add-entity-route - JHipster will add entity modules routes here */
    ]),
  ],
})
export class EntityRoutingModule {}
