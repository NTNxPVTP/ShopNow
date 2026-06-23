import React, { useState } from 'react';
import { Form, Button, Card, Alert, Container, Row, Col } from 'react-bootstrap';
import { useHistory, Link } from 'react-router-dom';
import { register } from '../../api/authApi';
import { useAuth } from '../../contexts/AuthContext';

const RegisterPage = () => {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('CUSTOMER');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const history = useHistory();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);
    try {
      // Gọi API đăng ký
      await register({ name, email, password, role });
      
      setSuccess('Đăng ký thành công! Đang tự động đăng nhập...');
      
      // Đăng nhập tự động ngay sau khi đăng ký thành công
      const userData = await login(email, password);
      // Redirect theo role
      setTimeout(() => {
        switch (userData.role) {
          case 'SELLER':
            history.push('/seller/products');
            break;
          case 'ADMIN':
            history.push('/admin/categories');
            break;
          default:
            history.push('/products');
        }
      }, 1000);

    } catch (err) {
      setError(
        err.response?.data?.message || 'Đăng ký thất bại. Email có thể đã tồn tại.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container>
      <Row className="justify-content-center mt-5">
        <Col md={6} lg={4}>
          <Card>
            <Card.Body>
              <h3 className="text-center mb-4">Đăng ký</h3>
              {error && <Alert variant="danger">{error}</Alert>}
              {success && <Alert variant="success">{success}</Alert>}
              <Form onSubmit={handleSubmit}>
                <Form.Group className="mb-3" controlId="name">
                  <Form.Label>Họ và tên</Form.Label>
                  <Form.Control
                    type="text"
                    placeholder="Nhập họ và tên"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                  />
                </Form.Group>
                <Form.Group className="mb-3" controlId="email">
                  <Form.Label>Email</Form.Label>
                  <Form.Control
                    type="email"
                    placeholder="Nhập email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                </Form.Group>
                <Form.Group className="mb-3" controlId="password">
                  <Form.Label>Mật khẩu</Form.Label>
                  <Form.Control
                    type="password"
                    placeholder="Nhập mật khẩu"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                  />
                </Form.Group>
                <Form.Group className="mb-4" controlId="role">
                  <Form.Label>Đăng ký dưới dạng</Form.Label>
                  <Form.Select value={role} onChange={(e) => setRole(e.target.value)}>
                    <option value="CUSTOMER">Người mua hàng (Customer)</option>
                    <option value="SELLER">Người bán hàng (Seller)</option>
                  </Form.Select>
                </Form.Group>
                <Button variant="success" type="submit" className="w-100" disabled={loading}>
                  {loading ? 'Đang xử lý...' : 'Đăng ký'}
                </Button>
              </Form>
              <div className="text-center mt-3">
                <span className="text-muted">Đã có tài khoản? </span>
                <Link to="/login">Đăng nhập</Link>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
};

export default RegisterPage;
