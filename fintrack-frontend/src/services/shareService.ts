import { apiService } from './api';
import { Share } from '../types/share';

export const fetchMyShares = async (): Promise<Share[]> => {
  try {
    const response = await apiService.getMyShares();
    
    // Transformar a resposta para o formato esperado
    if (response && response.shares) {
      return response.shares.map((share: any) => ({
        id: share.shareId,
        creditCardId: share.creditCardId,
        creditCardName: share.creditCardName,
        ownerName: share.creditCardOwnerName,
        invoiceItemDescription: share.itemDescription,
        invoiceMonth: share.invoiceMonth || 'N/A',
        amount: share.myAmount,
        status: share.invoiceStatus || 'PENDING',
        createdAt: share.shareCreatedAt
      }));
    }
    
    return [];
  } catch (error) {
    console.error('Error fetching shares:', error);
    throw error;
  }
}; 