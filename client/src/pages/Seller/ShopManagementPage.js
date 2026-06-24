import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Button, Modal, Form, Alert, Badge, Spinner } from 'react-bootstrap';
import { useHistory } from 'react-router-dom';
import { getMyShops, createShop } from '../../api/shopApi';

const ShopManagementPage = () => {
  const [shops, setShops] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Modal state
  const [showModal, setShowModal] = useState(false);
  const [newShop, setNewShop] = useState({ name: '', address: '', avatarUrl: '' });
  const [creating, setCreating] = useState(false);

  const history = useHistory();

  useEffect(() => {
    fetchShops();
  }, []);

  const fetchShops = async () => {
    try {
      setLoading(true);
      const res = await getMyShops();
      setShops(res.data || []);
      setError('');
    } catch (err) {
      setError('Không thể tải danh sách shop.');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateShop = async (e) => {
    e.preventDefault();
    try {
      setCreating(true);
      await createShop(newShop);
      setShowModal(false);
      setNewShop({ name: '', address: '', avatarUrl: '' });
      fetchShops();
    } catch (err) {
      alert('Tạo shop thất bại. Vui lòng thử lại.');
    } finally {
      setCreating(false);
    }
  };

  if (loading) {
    return (
      <div className="text-center mt-5">
        <Spinner animation="border" variant="primary" />
        <p className="mt-2 text-muted">Đang tải dữ liệu shop...</p>
      </div>
    );
  }

  return (
    <div className="py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2 className="mb-0 fw-bold" style={{ color: '#2c3e50' }}>Quản lý Shop của bạn</h2>
        <Button 
          variant="primary" 
          className="rounded-pill px-4 shadow-sm"
          onClick={() => setShowModal(true)}
        >
          <i className="bi bi-plus-circle me-2"></i> + Tạo Shop mới
        </Button>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      {shops.length === 0 && !error ? (
        <div className="text-center p-5 bg-light rounded-4 border border-dashed">
          <h4 className="text-muted mb-3">Bạn chưa có shop nào</h4>
          <p className="text-muted mb-4">Hãy bắt đầu kinh doanh bằng cách tạo một shop mới ngay bây giờ!</p>
          <Button variant="outline-primary" onClick={() => setShowModal(true)}>Tạo Shop ngay</Button>
        </div>
      ) : (
        <Row className="g-4">
          {shops.map(shop => (
            <Col key={shop.id} xs={12} md={6} lg={4}>
              <Card 
                className="h-100 shadow-sm border-0 shop-card transition-all" 
                style={{ cursor: 'pointer', borderRadius: '15px', overflow: 'hidden' }}
                onClick={() => history.push(`/seller/shops/${shop.id}`)}
              >
                <div 
                  className="bg-primary bg-gradient position-relative" 
                  style={{ height: '100px' }}
                >
                  {shop.isActive ? (
                    <Badge bg="success" className="position-absolute top-0 end-0 m-2">Đang hoạt động</Badge>
                  ) : (
                    <Badge bg="secondary" className="position-absolute top-0 end-0 m-2">Tạm ngưng</Badge>
                  )}
                </div>
                <Card.Body className="text-center position-relative pt-0">
                  <div 
                    className="bg-white p-1 rounded-circle d-inline-block shadow-sm"
                    style={{ marginTop: '-40px', marginBottom: '15px' }}
                  >
                    <img 
                      src={shop.avatarUrl || 'https://via.placeholder.com/80?text=Shop'} 
                      alt="Shop Avatar" 
                      className="rounded-circle"
                      style={{ width: '80px', height: '80px', objectFit: 'cover' }}
                    />
                  </div>
                  <Card.Title className="fw-bold fs-5 mb-1">{shop.name}</Card.Title>
                  <Card.Text className="text-muted small mb-3">
                    <i className="bi bi-geo-alt-fill me-1"></i> {shop.address || 'Chưa cập nhật địa chỉ'}
                  </Card.Text>
                  <Button variant="light" className="w-100 rounded-pill text-primary fw-medium border">
                    Xem danh sách sản phẩm &rarr;
                  </Button>
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      {/* Modal Tạo Shop */}
      <Modal show={showModal} onHide={() => setShowModal(false)} centered>
        <Modal.Header closeButton className="border-0 pb-0">
          <Modal.Title className="fw-bold">Đăng ký Shop mới</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form onSubmit={handleCreateShop}>
            <Form.Group className="mb-3">
              <Form.Label className="fw-medium">Tên Shop <span className="text-danger">*</span></Form.Label>
              <Form.Control 
                type="text" 
                placeholder="Nhập tên shop của bạn" 
                required
                value={newShop.name}
                onChange={e => setNewShop({...newShop, name: e.target.value})}
                className="rounded-3"
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label className="fw-medium">Địa chỉ</Form.Label>
              <Form.Control 
                type="text" 
                placeholder="Nhập địa chỉ cửa hàng"
                value={newShop.address}
                onChange={e => setNewShop({...newShop, address: e.target.value})}
                className="rounded-3"
              />
            </Form.Group>
            <Form.Group className="mb-4">
              <Form.Label className="fw-medium">Ảnh đại diện (URL)</Form.Label>
              <Form.Control 
                type="text" 
                placeholder="https://example.com/image.jpg"
                value={newShop.avatarUrl}
                onChange={e => setNewShop({...newShop, avatarUrl: e.target.value})}
                className="rounded-3"
              />
            </Form.Group>
            <div className="d-grid gap-2">
              <Button variant="primary" type="submit" disabled={creating} className="rounded-pill py-2">
                {creating ? <Spinner size="sm" /> : 'Xác nhận đăng ký'}
              </Button>
              <Button variant="light" onClick={() => setShowModal(false)} className="rounded-pill border">
                Hủy bỏ
              </Button>
            </div>
          </Form>
        </Modal.Body>
      </Modal>

      <style jsx="true">{`
        .shop-card:hover {
          transform: translateY(-5px);
          box-shadow: 0 10px 20px rgba(0,0,0,0.1) !important;
        }
        .border-dashed {
          border-style: dashed !important;
          border-width: 2px !important;
          border-color: #dee2e6 !important;
        }
      `}</style>
    </div>
  );
};

export default ShopManagementPage;
