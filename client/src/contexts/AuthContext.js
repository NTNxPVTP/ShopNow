import React, { createContext, useContext, useState, useEffect } from 'react';
import { jwtDecode } from 'jwt-decode';
import { login as loginApi } from '../api/authApi';

const AuthContext = createContext(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [accessToken, setAccessToken] = useState(localStorage.getItem('access_token'));
  const [user, setUser] = useState(null);

  useEffect(() => {
    if (accessToken) {
      try {
        const decoded = jwtDecode(accessToken);
        setUser({
          email: decoded.sub,
          role: decoded.role || decoded.authorities?.[0]?.replace('ROLE_', '') || null,
        });
      } catch (err) {
        // Token không hợp lệ
        setAccessToken(null);
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
      }
    } else {
      setUser(null);
    }
  }, [accessToken]);

  const login = async (email, password) => {
    const response = await loginApi(email, password);
    const { access_token, refresh_token } = response.data;
    localStorage.setItem('access_token', access_token);
    localStorage.setItem('refresh_token', refresh_token);
    setAccessToken(access_token);
    const decoded = jwtDecode(access_token);
    const userData = {
      email: decoded.sub,
      role: decoded.role || decoded.authorities?.[0]?.replace('ROLE_', '') || null,
    };
    setUser(userData);
    return userData;
  };

  const logout = () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    setAccessToken(null);
    setUser(null);
  };

  const isAuthenticated = !!accessToken && !!user;

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, login, logout, accessToken }}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthContext;
