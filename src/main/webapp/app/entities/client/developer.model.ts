export interface IDeveloper {
  id: number;
  streetAddress?: string | null;
  postalCode?: string | null;
  city?: string | null;
  stateProvince?: string | null;
}

export type NewDeveloper = Omit<IDeveloper, 'id'> & { id: null };
