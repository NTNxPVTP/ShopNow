import axiosClient from './axiosClient';

export const login = (email, password) => {
  return axiosClient.post('/api/auth/authenticate', { email, password });
};

export const register = (data) => {
  return axiosClient.post('/api/auth/register', data);
};

