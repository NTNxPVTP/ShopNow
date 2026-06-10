import axiosClient from './axiosClient';

export const login = (email, password) => {
  return axiosClient.post('/authenticate', { email, password });
};
