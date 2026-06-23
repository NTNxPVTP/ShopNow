import axiosClient from './axiosClient';

export const processPayment = (data) => {
  return axiosClient.post('/api/payments', data);
};
