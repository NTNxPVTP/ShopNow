import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Row, Col, Image, Button, Alert, Form } from 'react-bootstrap';
import { getProductById } from '../../api/productApi';
import { useAuth } from '../../contexts/AuthContext';
import { useCart } from '../../contexts/CartContext';
import LoadingSpinner from '../../components/LoadingSpinner';

const ProductDetailPage = () => {
  const { id } = useParams();
  const { user, isAuthenticated } = useAuth();
  const { addToCart } = useCart();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [quantity, setQuantity] = useState(1);
  const [added, setAdded] = useState(false);

  useEffect(() => {
    const fetchProduct = async () => {
      setLoading(true);
      try {
        const res = await getProductById(id);
        setProduct(res.data);
      } catch (err) {
        if (err.response?.status === 404) {
          setError('Không tìm thấy sản phẩm.');
        } else {
          setError('Không thể tải thông tin sản phẩm.');
        }
      } finally {
        setLoading(false);
      }
    };
    fetchProduct();
  }, [id]);

  const handleAddToCart = () => {
    if (product) {
      addToCart(product, quantity);
      setAdded(true);
      setTimeout(() => setAdded(false), 2000);
    }
  };

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const isOutOfStock = product?.status === 'SOLD' || product?.quantity === 0;

  if (loading) return <LoadingSpinner />;
  if (error) return <Alert variant="danger">{error}</Alert>;
  if (!product) return null;

  return (
    <Row>
      <Col md={6}>
        {product.pictureUrl ? (
          <Image src={product.pictureUrl} alt={product.name} fluid rounded />
        ) : (
          <div
            className="bg-light d-flex align-items-center justify-content-center rounded"
            style={{ height: '400px' }}
          >
            <span className="text-muted">Không có ảnh</span>
          </div>
        )}
      </Col>
      <Col md={6}>
        <h2>{product.name}</h2>
        <h3 className="text-primary">{formatPrice(product.price)}</h3>
        <p>
          <strong>Trạng thái: </strong>
          <span className={isOutOfStock ? 'text-danger' : 'text-success'}>
            {isOutOfStock ? 'Hết hàng' : 'Còn hàng'}
          </span>
        </p>
        <p>
          <strong>Tồn kho: </strong>
          {product.quantity}
        </p>

        {added && <Alert variant="success">Đã thêm vào giỏ hàng!</Alert>}

        {isAuthenticated && user?.role === 'CUSTOMER' && (
          <div className="mt-3">
            <Form.Group className="mb-3" style={{ maxWidth: '120px' }}>
              <Form.Label>Số lượng</Form.Label>
              <Form.Control
                type="number"
                min={1}
                max={product.quantity}
                value={quantity}
                onChange={(e) => setQuantity(Math.max(1, parseInt(e.target.value) || 1))}
                disabled={isOutOfStock}
              />
            </Form.Group>
            <Button
              variant="warning"
              size="lg"
              onClick={handleAddToCart}
              disabled={isOutOfStock}
            >
              {isOutOfStock ? 'Hết hàng' : 'Thêm vào giỏ'}
            </Button>
          </div>
        )}
      </Col>
    </Row>
  );
};

export default ProductDetailPage;
