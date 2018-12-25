import axiosInstance from './axios';
import * as _ from 'utils/commonUtils';
import { handleError } from 'utils/apiUtils';

export const login = async ({ username, password }) => {
  try {
    const res = await axiosInstance.post('/api/login', {
      username,
      password,
    });
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const logout = async () => {
  try {
    const res = await axiosInstance.get('/api/logout');
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
