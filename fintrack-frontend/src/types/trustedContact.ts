export interface TrustedContact {
  id: number;
  name: string;
  email: string;
  tags: string | null;
  note: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTrustedContactRequest {
  name: string;
  email: string;
  tags?: string;
  note?: string;
}

export interface UpdateTrustedContactRequest {
  name?: string;
  email?: string;
  tags?: string;
  note?: string;
}
