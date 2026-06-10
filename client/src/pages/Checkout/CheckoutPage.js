import React, { useState } from 'react';
import { Form, Button, Alert, Card } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { useCart } from '../../contexts/CartContext';
import { createOrder } from '../../api/orderApi';

const CheckoutPage = () => {
  const { items, totalPrice, clearCart } = useCart();
  const [customerName, setCustomerName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [addressShipping, setAddressShipping] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [orderResult, setOrderResult] = useState(null);

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (items.length === 0) {
      setError('Giỏ hàng trống. Vui lòng thêm sản phẩm.');
      return;
    }
    if (!customerName.trim() || !phoneNumber.trim() || !addressShipping.trim()) {
      setError('Vui lòng điền đầy đủ thông tin.');
      return;
    }

    setLoading(true);
    try {
      const data = {
        listItems: items.map((item) => ({
          productId: item.productId,
          quantity: item.quantity,
        })),
        addressShipping: addressShipping.trim(),
        phoneNumber: phoneNumber.trim(),
        customerName: customerName.trim(),
      };
      const res = await createOrder(data);
      setOrderResult(res.data);
      clearCart();
    } catch (err) {
      setError(err.response?.data?.message || 'Đặt hàng thất bại. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  if (orderResult) {
    return (
      <div className="text-center py-5">
        <Alert variant="success">
          <h4>Đặt hàng thành công!</h4>
          <p>Mã đơn hàng: {orderResult.id}</p>
          <p>Tổng tiền: {formatPrice(orderResult.totalPrice)}</p>
          <p>Trạng thái: {orderResult.status}</p>
        </Alert>
        <Link to="/orders" className="btn btn-primary">
          Xem đơn hàng
        </Link>
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="text-center py-5">
        <h3>Giỏ hàng trống</h3>
        <Link to="/products" className="btn btn-primary">
          Mua sắm ngay
        </Link>
      </div>
    );
  }

  return (
    <div>
      <h2 className="mb-4">Thanh toán</h2>
      {error && <Alert variant="danger">{error}</Alert>}

      <Card className="mb-4">
        <Card.Body>
          <h5>Tóm tắt đơn hàng</h5>
          <ul>
            {items.map((item) => (
              <li key={item.productId}>
                {item.name} x {item.quantity} = {formatPrice(item.price * item.quantity)}
              </li>
            ))}
          </ul>
          <strong>Tổng: {formatPrice(totalPrice)}</strong>
        </Card.Body>
      </Card>

      <Form onSubmit={handleSubmit}>
        <Form.Group className="mb-3">
          <Form.Label>Tên khách hàng *</Form.Label>
          <Form.Control
            type="text"
            placeholder="Nhập tên"
            value={customerName}
            onChange={(e) => setCustomerName(e.target.value)}
            required
          />
        </Form.Group>
        <Form.Group className="mb-3">
          <Form.Label>Số điện thoại *</Form.Label>
          <Form.Control
            type="tel"
            placeholder="Nhập số điện thoại"
            value={phoneNumber}
            onChange={(e) => setPhoneNumber(e.target.value)}
            required
          />
        </Form.Group>
        <Form.Group className="mb-3">
          <Form.Label>Địa chỉ giao hàng *</Form.Label>
          <Form.Control
            as="textarea"
            rows={3}
            placeholder="Nhập địa chỉ giao hàng"
            value={addressShipping}
            onChange={(e) => setAddressShipping(e.target.value)}
            required
          />
        </Form.Group>
        <Button type="submit" variant="success" size="lg" disabled={loading}>
          {loading ? 'Đang xử lý...' : 'Đặt hàng'}
        </Button>
      </Form>
    </div>
  );
};

export default CheckoutPage;
