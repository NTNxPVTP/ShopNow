import axiosClient from './axiosClient';

export const getOrders = (params) => {
  return axiosClient.get('/api/orders', { params });
};

export const getOrderById = (id) => {
  return axiosClient.get(`/api/orders/${id}`);
};

export const createOrder = (data) => {
  return axiosClient.post('/api/orders', data);
};
