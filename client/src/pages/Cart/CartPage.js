import React from 'react';
import { Table, Button, Form } from 'react-bootstrap';
import { Link, useHistory } from 'react-router-dom';
import { useCart } from '../../contexts/CartContext';

const CartPage = () => {
  const { items, totalPrice, updateQuantity, removeFromCart } = useCart();
  const history = useHistory();

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
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
      <Table responsive striped bordered hover>
        <thead>
          <tr>
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
      <div className="d-flex justify-content-between align-items-center mt-3">
        <h4>Tổng cộng: {formatPrice(totalPrice)}</h4>
        <Button variant="success" size="lg" onClick={() => history.push('/checkout')}>
          Thanh toán
        </Button>
      </div>
    </div>
  );
};

export default CartPage;
