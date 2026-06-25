import axiosClient from './axiosClient';

export const getProducts = (params) => {
  return axiosClient.get('/api/products', { params });
};

export const getProductById = (id) => {
  return axiosClient.get(`/api/products/${id}`);
};


export const updateProduct = (id, data) => {
  return axiosClient.patch(`/api/products/${id}`, data);
};

export const deleteProduct = (id) => {
  return axiosClient.delete(`/api/products/${id}`);
};
