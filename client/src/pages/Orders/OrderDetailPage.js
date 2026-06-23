import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Card, Table, Badge, Alert, Button } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { getOrderById } from '../../api/orderApi';
import LoadingSpinner from '../../components/LoadingSpinner';

const statusBadge = (status) => {
  const map = { IN_PROCESS: 'warning', DELIVERING: 'info', PAID: 'success' };
  return <Badge bg={map[status] || 'secondary'}>{status}</Badge>;
};

const OrderDetailPage = () => {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchOrder = async () => {
      try {
        const res = await getOrderById(id);
        setOrder(res.data);
      } catch (err) {
        setError('Không thể tải chi tiết đơn hàng.');
      } finally {
        setLoading(false);
      }
    };
    fetchOrder();
  }, [id]);

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleString('vi-VN');
  };

  if (loading) return <LoadingSpinner />;
  if (error) return <Alert variant="danger">{error}</Alert>;
  if (!order) return null;

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Chi tiết đơn hàng</h2>
        {order.status !== 'PAID' && (
          <Button as={Link} to={`/checkout/${order.id}`} variant="success" size="lg">
            Thanh toán ngay
          </Button>
        )}
      </div>
      <Card className="mb-4">
        <Card.Body>
          <p><strong>Mã đơn:</strong> {order.id}</p>
          <p><strong>Trạng thái:</strong> {statusBadge(order.status)}</p>
          <p><strong>Tổng tiền:</strong> {formatPrice(order.totalPrice)}</p>
          <p><strong>Khách hàng:</strong> {order.customerName}</p>
          <p><strong>SĐT:</strong> {order.phoneNumber}</p>
          <p><strong>Địa chỉ:</strong> {order.addressShipping}</p>
          <p><strong>Ngày tạo:</strong> {formatDate(order.createdAt)}</p>
        </Card.Body>
      </Card>

      <h4>Sub-orders</h4>
      {order.subOrders && order.subOrders.length > 0 ? (
        order.subOrders.map((sub) => (
          <Card key={sub.id} className="mb-3">
            <Card.Body>
              <p><strong>Shop ID:</strong> <small>{sub.shopId}</small></p>
              <p><strong>Trạng thái:</strong> {statusBadge(sub.status)}</p>
              <p><strong>Tổng:</strong> {formatPrice(sub.totalPrice)}</p>
              {sub.orderDetails && sub.orderDetails.length > 0 && (
                <Table size="sm" bordered>
                  <thead>
                    <tr>
                      <th>Sản phẩm</th>
                      <th>Số lượng</th>
                      <th>Giá</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sub.orderDetails.map((detail, idx) => (
                      <tr key={idx}>
                        <td>{detail.productName || detail.productId}</td>
                        <td>{detail.quantity}</td>
                        <td>{formatPrice(detail.price || 0)}</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}
            </Card.Body>
          </Card>
        ))
      ) : (
        <p className="text-muted">Không có sub-order.</p>
      )}
    </div>
  );
};

export default OrderDetailPage;
