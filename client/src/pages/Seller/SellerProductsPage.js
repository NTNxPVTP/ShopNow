import React, { useState, useEffect } from 'react';
import { Table, Button, Alert } from 'react-bootstrap';
import { Link, useHistory } from 'react-router-dom';
import { deleteProduct } from '../../api/productApi';
import { getMyShops, getShopProducts } from '../../api/shopApi';
import LoadingSpinner from '../../components/LoadingSpinner';
import Pagination from '../../components/Pagination';

const SellerProductsPage = () => {
  const [allProducts, setAllProducts] = useState([]);
  const [products, setProducts] = useState([]);
  const [pageInfo, setPageInfo] = useState({ number: 0, totalPages: 0 });
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const history = useHistory();

  const fetchProducts = async () => {
    setLoading(true);
    setError('');
    try {
      const shopsRes = await getMyShops();
      const shops = shopsRes.data || [];

      const productsPromises = shops.map(async (shop) => {
        const res = await getShopProducts(shop.id);
        const shopProds = res.data || [];
        return shopProds.map(p => ({ ...p, shopName: shop.name }));
      });
      
      const productsArrays = await Promise.all(productsPromises);
      const combinedProducts = productsArrays.flat();

      setAllProducts(combinedProducts);
      updatePagination(combinedProducts, 1);
      setPage(1);
    } catch (err) {
      setError('Không thể tải danh sách sản phẩm.');
    } finally {
      setLoading(false);
    }
  };

  const updatePagination = (dataList, currentPage) => {
    const size = 10;
    const totalElements = dataList.length;
    const totalPages = Math.ceil(totalElements / size) || 1;
    const paginatedProducts = dataList.slice((currentPage - 1) * size, currentPage * size);
    
    setProducts(paginatedProducts);
    setPageInfo({ number: currentPage - 1, totalPages, totalElements });
  };

  useEffect(() => {
    if (allProducts.length > 0) {
      updatePagination(allProducts, page);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  useEffect(() => {
    fetchProducts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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
              <th>Cửa hàng</th>
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
                <td>{p.shopName}</td>
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
