import React, { useState, useEffect } from 'react';
import { Table, Form, Badge, Alert } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { getOrders } from '../../api/orderApi';
import Pagination from '../../components/Pagination';
import LoadingSpinner from '../../components/LoadingSpinner';

const statusBadge = (status) => {
  const map = {
    IN_PROCESS: 'warning',
    DELIVERING: 'info',
    PAID: 'success',
  };
  return <Badge bg={map[status] || 'secondary'}>{status}</Badge>;
};

const OrderListPage = () => {
  const [orders, setOrders] = useState([]);
  const [pageInfo, setPageInfo] = useState({ number: 0, totalPages: 0 });
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, status]);

  const fetchOrders = async () => {
    setLoading(true);
    setError('');
    try {
      const params = { page };
      if (status) params.status = status;
      const res = await getOrders(params);
      setOrders(res.data.items || []);
      setPageInfo(res.data.pageInfo || { number: 0, totalPages: 0 });
    } catch (err) {
      setError('Không thể tải danh sách đơn hàng.');
    } finally {
      setLoading(false);
    }
  };

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('vi-VN');
  };

  return (
    <div>
      <h2 className="mb-4">Đơn hàng của tôi</h2>

      <Form.Select
        className="mb-3"
        style={{ maxWidth: '200px' }}
        value={status}
        onChange={(e) => { setStatus(e.target.value); setPage(1); }}
      >
        <option value="">Tất cả trạng thái</option>
        <option value="IN_PROCESS">Đang xử lý</option>
        <option value="DELIVERING">Đang giao</option>
        <option value="PAID">Đã thanh toán</option>
      </Form.Select>

      {loading && <LoadingSpinner />}
      {error && <Alert variant="danger">{error}</Alert>}

      {!loading && orders.length === 0 && !error && (
        <p className="text-muted">Chưa có đơn hàng nào.</p>
      )}

      {orders.length > 0 && (
        <Table responsive striped bordered hover>
          <thead>
            <tr>
              <th>Mã đơn</th>
              <th>Trạng thái</th>
              <th>Tổng tiền</th>
              <th>Ngày tạo</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.id}>
                <td><small>{order.id?.substring(0, 8)}...</small></td>
                <td>{statusBadge(order.status)}</td>
                <td>{formatPrice(order.totalPrice)}</td>
                <td>{formatDate(order.createdAt)}</td>
                <td>
                  <Link to={`/orders/${order.id}`} className="btn btn-sm btn-outline-primary">
                    Chi tiết
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}

      <Pagination
        currentPage={page}
        totalPages={pageInfo.totalPages || 1}
        onPageChange={setPage}
      />
    </div>
  );
};

export default OrderListPage;
