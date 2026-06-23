import axiosClient from './axiosClient';

export const getCart = () => {
  return axiosClient.get('/api/carts');
};

export const addItemToCart = (data) => {
  return axiosClient.post('/api/carts/items', data);
};

export const updateCartItemQuantity = (productId, quantity) => {
  return axiosClient.put(`/api/carts/items/${productId}`, null, { params: { quantity } });
};

export const removeCartItem = (productId) => {
  return axiosClient.delete(`/api/carts/items/${productId}`);
};

export const clearCartApi = () => {
  return axiosClient.delete('/api/carts');
};
