import React from 'react';
import { Navbar, Nav, Container, Badge } from 'react-bootstrap';
import { Link, useHistory } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { useCart } from '../../contexts/CartContext';

const Header = () => {
  const { isAuthenticated, user, logout } = useAuth();
  const { items } = useCart();
  const history = useHistory();

  const handleLogout = () => {
    logout();
    history.push('/login');
  };

  return (
    <Navbar bg="dark" variant="dark" expand="lg" sticky="top">
      <Container>
        <Navbar.Brand as={Link} to="/products">
          ShopNow
        </Navbar.Brand>
        <Navbar.Toggle aria-controls="main-nav" />
        <Navbar.Collapse id="main-nav">
          <Nav className="me-auto">
            <Nav.Link as={Link} to="/products">
              Sản phẩm
            </Nav.Link>
            {isAuthenticated && user?.role === 'CUSTOMER' && (
              <>

                <Nav.Link as={Link} to="/orders">
                  Đơn hàng
                </Nav.Link>
              </>
            )}
            {isAuthenticated && user?.role === 'SELLER' && (
              <>
                <Nav.Link as={Link} to="/seller/shops">
                  Quản lý Shop
                </Nav.Link>
                <Nav.Link as={Link} to="/seller/products">
                  Quản lý SP
                </Nav.Link>
                <Nav.Link as={Link} to="/seller/sub-orders">
                  Sub-orders
                </Nav.Link>
              </>
            )}
            {isAuthenticated && user?.role === 'ADMIN' && (
              <Nav.Link as={Link} to="/admin/categories">
                Danh mục
              </Nav.Link>
            )}
          </Nav>
          <Nav>
            {isAuthenticated ? (
              <>
                <Navbar.Text className="me-3">
                  {user?.email} ({user?.role})
                </Navbar.Text>
                {user?.role === 'CUSTOMER' && (
                  <Nav.Link as={Link} to="/cart" className="me-2">
                    🛒 Giỏ hàng{' '}
                    {items.length > 0 && <Badge bg="warning" text="dark">{items.length}</Badge>}
                  </Nav.Link>
                )}
                <Nav.Link as={Link} to="/profile">Hồ sơ</Nav.Link>
                <Nav.Link onClick={handleLogout}>Đăng xuất</Nav.Link>
              </>
            ) : (
              <Nav.Link as={Link} to="/login">
                Đăng nhập
              </Nav.Link>
            )}
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
};

export default Header;
