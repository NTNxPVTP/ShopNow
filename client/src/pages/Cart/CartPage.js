import React, { useState } from 'react';
import { Table, Button, Form, Card, Alert } from 'react-bootstrap';
import { Link, useHistory } from 'react-router-dom';
import { useCart } from '../../contexts/CartContext';
import { createOrder } from '../../api/orderApi';

const CartPage = () => {
  const { items, updateQuantity, removeFromCart } = useCart();
  const history = useHistory();

  const [selectedItems, setSelectedItems] = useState([]);
  const [customerName, setCustomerName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [addressShipping, setAddressShipping] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const handleSelect = (productId) => {
    if (selectedItems.includes(productId)) {
      setSelectedItems(selectedItems.filter((id) => id !== productId));
    } else {
      setSelectedItems([...selectedItems, productId]);
    }
  };

  const handleSelectAll = () => {
    if (selectedItems.length === items.length) {
      setSelectedItems([]);
    } else {
      setSelectedItems(items.map((i) => i.productId));
    }
  };

  const getSelectedTotal = () => {
    return items
      .filter((i) => selectedItems.includes(i.productId))
      .reduce((sum, item) => sum + item.price * item.quantity, 0);
  };

  const handleCreateOrder = async (e) => {
    e.preventDefault();
    if (selectedItems.length === 0) {
      setError('Vui lòng chọn ít nhất một sản phẩm để đặt hàng.');
      return;
    }
    if (!customerName.trim() || !phoneNumber.trim() || !addressShipping.trim()) {
      setError('Vui lòng điền đầy đủ thông tin giao hàng.');
      return;
    }

    const orderItems = items.filter((i) => selectedItems.includes(i.productId));

    setLoading(true);
    setError('');
    try {
      const data = {
        listItems: orderItems.map((item) => ({
          productId: item.productId,
          quantity: item.quantity,
        })),
        addressShipping: addressShipping.trim(),
        phoneNumber: phoneNumber.trim(),
        customerName: customerName.trim(),
      };

      // 1. Create order
      await createOrder(data);

      // 2. Remove selected items from cart
      for (const item of orderItems) {
        await removeFromCart(item.productId);
      }

      // 3. Redirect to orders page
      history.push('/orders');
    } catch (err) {
      setError(err.response?.data?.message || 'Tạo đơn hàng thất bại. Vui lòng thử lại.');
      setLoading(false);
    }
  };

  if (items.length === 0) {
    return (
      <div className="text-center py-5">
        <h3>Giỏ hàng trống</h3>
        <p className="text-muted">Hãy thêm sản phẩm vào giỏ hàng.</p>
        <Link to="/products" className="btn btn-primary">
          Mua sắm ngay
        </Link>
      </div>
    );
  }

  return (
    <div>
      <h2 className="mb-4">Giỏ hàng</h2>
      {error && <Alert variant="danger">{error}</Alert>}
      <Table responsive striped bordered hover>
        <thead>
          <tr>
            <th style={{ width: '40px' }}>
              <Form.Check 
                type="checkbox" 
                checked={selectedItems.length === items.length && items.length > 0}
                onChange={handleSelectAll}
              />
            </th>
            <th>Sản phẩm</th>
            <th>Giá</th>
            <th style={{ width: '120px' }}>Số lượng</th>
            <th>Thành tiền</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.productId}>
              <td>
                <Form.Check 
                  type="checkbox"
                  checked={selectedItems.includes(item.productId)}
                  onChange={() => handleSelect(item.productId)}
                />
              </td>
              <td>
                <div className="d-flex align-items-center">
                  {item.pictureUrl && (
                    <img
                      src={item.pictureUrl}
                      alt={item.name}
                      style={{ width: '50px', height: '50px', objectFit: 'cover', marginRight: '10px' }}
                    />
                  )}
                  {item.name}
                </div>
              </td>
              <td>{formatPrice(item.price)}</td>
              <td>
                <Form.Control
                  type="number"
                  min={1}
                  value={item.quantity}
                  onChange={(e) =>
                    updateQuantity(item.productId, parseInt(e.target.value) || 1)
                  }
                  size="sm"
                />
              </td>
              <td>{formatPrice(item.price * item.quantity)}</td>
              <td>
                <Button
                  variant="outline-danger"
                  size="sm"
                  onClick={() => removeFromCart(item.productId)}
                >
                  Xóa
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
      
      <div className="text-end mb-4">
        <h4>Tổng tiền chọn mua: <span className="text-danger">{formatPrice(getSelectedTotal())}</span></h4>
      </div>

      {selectedItems.length > 0 && (
        <Card className="mb-5 border-success">
          <Card.Header className="bg-success text-white">Thông tin giao hàng</Card.Header>
          <Card.Body>
            <Form onSubmit={handleCreateOrder}>
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
              <Form.Group className="mb-4">
                <Form.Label>Địa chỉ giao hàng *</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={2}
                  placeholder="Nhập địa chỉ giao hàng"
                  value={addressShipping}
                  onChange={(e) => setAddressShipping(e.target.value)}
                  required
                />
              </Form.Group>
              <div className="text-end">
                <Button type="submit" variant="success" size="lg" disabled={loading}>
                  {loading ? 'Đang xử lý...' : 'Tạo Đơn Hàng'}
                </Button>
              </div>
            </Form>
          </Card.Body>
        </Card>
      )}
    </div>
  );
};

export default CartPage;
