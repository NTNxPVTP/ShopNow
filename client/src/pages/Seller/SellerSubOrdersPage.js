import React, { useState, useEffect } from 'react';
import { Table, Form, Badge, Alert, Modal, Card } from 'react-bootstrap';
import { getSubOrders, getSubOrderById } from '../../api/subOrderApi';
import Pagination from '../../components/Pagination';
import LoadingSpinner from '../../components/LoadingSpinner';

const statusBadge = (status) => {
  const map = { IN_PROCESS: 'warning', DELIVERING: 'info', PAID: 'success' };
  return <Badge bg={map[status] || 'secondary'}>{status}</Badge>;
};

const SellerSubOrdersPage = () => {
  const [subOrders, setSubOrders] = useState([]);
  const [pageInfo, setPageInfo] = useState({ number: 0, totalPages: 0 });
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selectedSubOrder, setSelectedSubOrder] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);

  useEffect(() => {
    fetchSubOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, status]);

  const fetchSubOrders = async () => {
    setLoading(true);
    setError('');
    try {
      const params = { page };
      if (status) params.status = status;
      const res = await getSubOrders(params);
      setSubOrders(res.data.items || []);
      setPageInfo(res.data.pageInfo || { number: 0, totalPages: 0 });
    } catch (err) {
      setError('Không thể tải danh sách sub-order.');
    } finally {
      setLoading(false);
    }
  };

  const handleRowClick = async (id) => {
    setDetailLoading(true);
    setShowModal(true);
    try {
      const res = await getSubOrderById(id);
      setSelectedSubOrder(res.data);
    } catch (err) {
      setSelectedSubOrder(null);
    } finally {
      setDetailLoading(false);
    }
  };

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleString('vi-VN');
  };

  return (
    <div>
      <h2 className="mb-4">Sub-orders</h2>

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

      {!loading && subOrders.length === 0 && !error && (
        <p className="text-muted">Chưa có sub-order nào.</p>
      )}

      {subOrders.length > 0 && (
        <Table responsive striped bordered hover>
          <thead>
            <tr>
              <th>Mã</th>
              <th>Trạng thái</th>
              <th>Tổng tiền</th>
              <th>Ngày tạo</th>
            </tr>
          </thead>
          <tbody>
            {subOrders.map((sub) => (
              <tr key={sub.id} onClick={() => handleRowClick(sub.id)} style={{ cursor: 'pointer' }}>
                <td><small>{sub.id?.substring(0, 8)}...</small></td>
                <td>{statusBadge(sub.status)}</td>
                <td>{formatPrice(sub.totalPrice)}</td>
                <td>{formatDate(sub.createdAt)}</td>
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

      {/* Detail Modal */}
      <Modal show={showModal} onHide={() => setShowModal(false)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Chi tiết Sub-order</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {detailLoading && <LoadingSpinner />}
          {selectedSubOrder && !detailLoading && (
            <Card>
              <Card.Body>
                <p><strong>Mã:</strong> {selectedSubOrder.id}</p>
                <p><strong>Shop ID:</strong> {selectedSubOrder.shopId}</p>
                <p><strong>Trạng thái:</strong> {statusBadge(selectedSubOrder.status)}</p>
                <p><strong>Tổng tiền:</strong> {formatPrice(selectedSubOrder.totalPrice)}</p>
                <p><strong>Ngày tạo:</strong> {formatDate(selectedSubOrder.createdAt)}</p>
                <p><strong>Cập nhật:</strong> {formatDate(selectedSubOrder.updatedAt)}</p>

                {selectedSubOrder.orderDetails && selectedSubOrder.orderDetails.length > 0 && (
                  <>
                    <h6 className="mt-3">Chi tiết sản phẩm</h6>
                    <Table size="sm" bordered>
                      <thead>
                        <tr>
                          <th>Sản phẩm</th>
                          <th>Số lượng</th>
                          <th>Giá</th>
                        </tr>
                      </thead>
                      <tbody>
                        {selectedSubOrder.orderDetails.map((detail, idx) => (
                          <tr key={idx}>
                            <td>{detail.productName || detail.productId}</td>
                            <td>{detail.quantity}</td>
                            <td>{formatPrice(detail.price || 0)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </Table>
                  </>
                )}
              </Card.Body>
            </Card>
          )}
        </Modal.Body>
      </Modal>
    </div>
  );
};

export default SellerSubOrdersPage;
