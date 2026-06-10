import axiosClient from './axiosClient';

export const getCategories = () => {
  return axiosClient.get('/api/categories');
};

export const createCategory = (data) => {
  return axiosClient.post('/api/categories', data);
};
