import React from 'react';
import { Route, Redirect } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const RoleRoute = ({ component: Component, roles, ...rest }) => {
  const { isAuthenticated, user } = useAuth();

  return (
    <Route
      {...rest}
      render={(props) => {
        if (!isAuthenticated) {
          return <Redirect to="/login" />;
        }
        if (roles && !roles.includes(user?.role)) {
          return <Redirect to="/products" />;
        }
        return <Component {...props} />;
      }}
    />
  );
};

export default RoleRoute;
