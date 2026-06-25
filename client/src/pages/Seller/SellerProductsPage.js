import React, { useState, useEffect } from 'react';
import { Table, Button, Alert } from 'react-bootstrap';
import { Link, useHistory } from 'react-router-dom';
import { getProducts, deleteProduct } from '../../api/productApi';
import LoadingSpinner from '../../components/LoadingSpinner';
import Pagination from '../../components/Pagination';

const SellerProductsPage = () => {
  const [products, setProducts] = useState([]);
  const [pageInfo, setPageInfo] = useState({ number: 0, totalPages: 0 });
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const history = useHistory();

  useEffect(() => {
    fetchProducts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  const fetchProducts = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await getProducts({ page, size: 10 });
      setProducts(res.data.items || []);
      setPageInfo(res.data.pageInfo || { number: 0, totalPages: 0 });
    } catch (err) {
      setError('Không thể tải danh sách sản phẩm.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa sản phẩm này?')) return;
    try {
      await deleteProduct(id);
      fetchProducts();
    } catch (err) {
      if (err.response?.status === 403) {
        setError('Bạn không có quyền xóa sản phẩm này.');
      } else {
        setError('Xóa sản phẩm thất bại.');
      }
    }
  };

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Quản lý sản phẩm</h2>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}
      {loading && <LoadingSpinner />}

      {!loading && products.length === 0 && !error && (
        <p className="text-muted">Chưa có sản phẩm nào.</p>
      )}

      {products.length > 0 && (
        <Table responsive striped bordered hover>
          <thead>
            <tr>
              <th>Tên</th>
              <th>Giá</th>
              <th>Tồn kho</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {products.map((p) => (
              <tr key={p.id}>
                <td>{p.name}</td>
                <td>{formatPrice(p.price)}</td>
                <td>{p.quantity}</td>
                <td>{p.status}</td>
                <td>
                  <Button
                    as={Link}
                    to={`/seller/products/${p.id}/edit`}
                    variant="outline-primary"
                    size="sm"
                    className="me-2"
                  >
                    Sửa
                  </Button>
                  <Button
                    variant="outline-danger"
                    size="sm"
                    onClick={() => handleDelete(p.id)}
                  >
                    Xóa
                  </Button>
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

export default SellerProductsPage;
