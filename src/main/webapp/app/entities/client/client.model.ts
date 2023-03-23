export interface IClient {
  id: number;
  streetAddress?: string | null;
  postalCode?: string | null;
  city?: string | null;
  stateProvince?: string | null;
  name?: string | null;
  clientId: string | null;
}

export type NewClient = Omit<IClient, 'id'> & { id: null };
