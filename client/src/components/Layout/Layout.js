import React from 'react';
import { Container } from 'react-bootstrap';
import Header from './Header';

const Layout = ({ children }) => {
  return (
    <>
      <Header />
      <Container className="py-4">{children}</Container>
    </>
  );
};

export default Layout;
