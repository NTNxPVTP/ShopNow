import axiosClient from './axiosClient';

export const getMyProfile = () => {
  return axiosClient.get('/api/users/me');
};

export const updateMyProfile = (data) => {
  return axiosClient.put('/api/users/me', data);
};
