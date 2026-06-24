import axiosClient from './axiosClient';

export const getMyShops = () => {
  return axiosClient.get('/api/shops');
};

export const createShop = (data) => {
  return axiosClient.post('/api/shops', data);
};

export const getShopDetails = (id) => {
  return axiosClient.get(`/api/shops/${id}`);
};

export const getShopProducts = (id) => {
  return axiosClient.get(`/api/shops/${id}/products`);
};
