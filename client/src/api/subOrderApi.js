import axiosClient from './axiosClient';

export const getSubOrders = (params) => {
  return axiosClient.get('/api/subOrders', { params });
};

export const getSubOrderById = (id) => {
  return axiosClient.get(`/api/subOrders/${id}`);
};
