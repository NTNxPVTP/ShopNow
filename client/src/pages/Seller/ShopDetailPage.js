import React, { useState, useEffect } from 'react';
import { useParams, useHistory } from 'react-router-dom';
import { Row, Col, Card, Button, Spinner, Alert, Badge, Table } from 'react-bootstrap';
import { getShopDetails, getShopProducts } from '../../api/shopApi';

const ShopDetailPage = () => {
  const { id } = useParams();
  const history = useHistory();
  const [shop, setShop] = useState(null);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line
  }, [id]);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [shopRes, productsRes] = await Promise.all([
        getShopDetails(id),
        getShopProducts(id)
      ]);
      setShop(shopRes.data);
      setProducts(productsRes.data || []);
      setError('');
    } catch (err) {
      setError('Không thể tải dữ liệu shop. Vui lòng thử lại sau.');
    } finally {
      setLoading(false);
    }
  };

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  if (loading) {
    return (
      <div className="text-center mt-5">
        <Spinner animation="border" variant="primary" />
        <p className="mt-2 text-muted">Đang tải thông tin chi tiết...</p>
      </div>
    );
  }

  if (error || !shop) {
    return (
      <div className="mt-4">
        <Button variant="link" onClick={() => history.push('/seller/shops')} className="mb-3 text-decoration-none">
          &larr; Quay lại danh sách Shop
        </Button>
        <Alert variant="danger">{error || 'Không tìm thấy shop'}</Alert>
      </div>
    );
  }

  return (
    <div className="py-4">
      <Button variant="link" onClick={() => history.push('/seller/shops')} className="mb-3 p-0 text-decoration-none text-secondary">
        &larr; Trở về Quản lý Shop
      </Button>

      {/* Shop Info Header */}
      <Card className="border-0 shadow-sm rounded-4 mb-5 overflow-hidden">
        <div className="bg-primary bg-gradient p-4 text-white d-flex align-items-center">
          <img 
            src={shop.avatarUrl || 'https://via.placeholder.com/100?text=Shop'} 
            alt={shop.name}
            className="rounded-circle border border-3 border-white shadow-sm me-4 bg-white"
            style={{ width: '100px', height: '100px', objectFit: 'cover' }}
          />
          <div>
            <h2 className="mb-1 fw-bold">{shop.name}</h2>
            <p className="mb-0 opacity-75">
              <i className="bi bi-geo-alt-fill me-1"></i> {shop.address || 'Chưa có địa chỉ'}
            </p>
          </div>
          <div className="ms-auto text-end">
            <Badge bg={shop.isActive ? "success" : "secondary"} className="fs-6 mb-2">
              {shop.isActive ? "Đang hoạt động" : "Tạm ngưng"}
            </Badge>
            <p className="mb-0 small opacity-75">
              Tạo ngày: {new Date(shop.createdAt).toLocaleDateString('vi-VN')}
            </p>
          </div>
        </div>
      </Card>

      {/* Products List */}
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="fw-bold mb-0 text-dark">Sản phẩm của Shop ({products.length})</h4>
        <Button 
          variant="outline-primary" 
          className="rounded-pill px-3 shadow-sm"
          onClick={() => history.push('/seller/products/new')} // Có thể redirect đến trang tạo sản phẩm truyền theo shopId
        >
          + Thêm sản phẩm mới
        </Button>
      </div>

      <Card className="border-0 shadow-sm rounded-4">
        <Card.Body className="p-0">
          {products.length === 0 ? (
            <div className="text-center p-5">
              <h5 className="text-muted">Shop này chưa có sản phẩm nào</h5>
              <Button 
                variant="primary" 
                className="mt-3 rounded-pill"
                onClick={() => history.push('/seller/products/new')}
              >
                Thêm sản phẩm đầu tiên
              </Button>
            </div>
          ) : (
            <Table responsive hover className="mb-0 align-middle">
              <thead className="bg-light">
                <tr>
                  <th className="border-0 py-3 ps-4">Sản phẩm</th>
                  <th className="border-0 py-3">Giá</th>
                  <th className="border-0 py-3">Kho</th>
                  <th className="border-0 py-3">Trạng thái</th>
                  <th className="border-0 py-3 text-end pe-4">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {products.map(p => (
                  <tr key={p.id}>
                    <td className="ps-4">
                      <div className="d-flex align-items-center">
                        <img 
                          src={p.pictureUrl || 'https://via.placeholder.com/50?text=SP'} 
                          alt={p.name}
                          className="rounded me-3"
                          style={{ width: '50px', height: '50px', objectFit: 'cover' }}
                        />
                        <span className="fw-medium">{p.name}</span>
                      </div>
                    </td>
                    <td className="fw-medium text-danger">{formatPrice(p.price)}</td>
                    <td>{p.quantity}</td>
                    <td>
                      <Badge bg={p.status === 'ACTIVE' ? 'success' : 'warning'} text="dark" className="bg-opacity-25 border">
                        {p.status}
                      </Badge>
                    </td>
                    <td className="text-end pe-4">
                      <Button variant="light" size="sm" className="me-2 text-primary shadow-sm" onClick={() => history.push(`/seller/products/${p.id}/edit`)}>Sửa</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </Card.Body>
      </Card>
    </div>
  );
};

export default ShopDetailPage;
