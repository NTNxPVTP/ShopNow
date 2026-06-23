import React, { useState, useEffect } from 'react';
import { Form, Button, Alert, Card, Badge } from 'react-bootstrap';
import { Link, useParams } from 'react-router-dom';
import { getOrderById } from '../../api/orderApi';
import { processPayment } from '../../api/paymentApi';
import LoadingSpinner from '../../components/LoadingSpinner';

const statusBadge = (status) => {
  const map = { IN_PROCESS: 'warning', DELIVERING: 'info', PAID: 'success' };
  return <Badge bg={map[status] || 'secondary'}>{status}</Badge>;
};

const CheckoutPage = () => {
  const { orderId } = useParams();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);
  const [paymentResult, setPaymentResult] = useState(null);

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  useEffect(() => {
    const fetchOrder = async () => {
      try {
        const res = await getOrderById(orderId);
        setOrder(res.data);
      } catch (err) {
        setError('Không thể lấy thông tin đơn hàng.');
      } finally {
        setLoading(false);
      }
    };
    fetchOrder();
  }, [orderId]);

  const handlePayment = async (e) => {
    e.preventDefault();
    setError('');
    setPaying(true);

    try {
      const paymentRes = await processPayment({
        orderId: order.id,
        amount: order.totalPrice,
        method: 'CREDIT_CARD' // Backend only supports CREDIT_CARD currently
      });
      setPaymentResult(paymentRes.data);
      // Update local order status to PAID
      setOrder({ ...order, status: 'PAID' });
    } catch (err) {
      setError(err.response?.data?.message || 'Thanh toán thất bại. Vui lòng thử lại.');
    } finally {
      setPaying(false);
    }
  };

  if (loading) return <LoadingSpinner />;
  if (error && !order) return <div className="py-5"><Alert variant="danger">{error}</Alert></div>;
  if (!order) return <div className="py-5">Không tìm thấy đơn hàng.</div>;

  if (paymentResult || order.status === 'PAID') {
    return (
      <div className="text-center py-5">
        <Alert variant="success">
          <h4>Thanh toán thành công!</h4>
          <p>Mã đơn hàng: {order.id}</p>
          <p>Tổng tiền: {formatPrice(order.totalPrice)}</p>
          {paymentResult && (
            <>
              <hr />
              <h5>Thông tin giao dịch</h5>
              <p>Mã giao dịch: {paymentResult.transactionId}</p>
              <p>Phương thức: {paymentResult.method}</p>
            </>
          )}
        </Alert>
        <Link to="/orders" className="btn btn-primary mt-3">
          Về danh sách đơn hàng
        </Link>
      </div>
    );
  }

  return (
    <div>
      <h2 className="mb-4">Thanh toán Đơn hàng</h2>
      {error && <Alert variant="danger">{error}</Alert>}

      <Card className="mb-4">
        <Card.Body>
          <h5>Thông tin đơn hàng</h5>
          <p><strong>Mã đơn:</strong> {order.id}</p>
          <p><strong>Khách hàng:</strong> {order.customerName}</p>
          <p><strong>SĐT:</strong> {order.phoneNumber}</p>
          <p><strong>Địa chỉ:</strong> {order.addressShipping}</p>
          <p><strong>Trạng thái:</strong> {statusBadge(order.status)}</p>
          <h4 className="mt-4 text-danger">Tổng thanh toán: {formatPrice(order.totalPrice)}</h4>
        </Card.Body>
      </Card>

      <Form onSubmit={handlePayment}>
        <Form.Group className="mb-4">
          <Form.Label>Phương thức thanh toán</Form.Label>
          <Form.Control
            type="text"
            value="Thẻ tín dụng (Credit Card)"
            readOnly
            disabled
          />
          <Form.Text className="text-muted">
            Hệ thống hiện tại chỉ hỗ trợ thanh toán qua Thẻ tín dụng.
          </Form.Text>
        </Form.Group>
        
        <Button type="submit" variant="success" size="lg" disabled={paying}>
          {paying ? 'Đang xử lý...' : 'Xác nhận Thanh toán'}
        </Button>
      </Form>
    </div>
  );
};

export default CheckoutPage;
