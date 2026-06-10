import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Form, Button, InputGroup } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { getProducts } from '../../api/productApi';
import { getCategories } from '../../api/categoryApi';
import Pagination from '../../components/Pagination';
import LoadingSpinner from '../../components/LoadingSpinner';

const ProductListPage = () => {
  const [products, setProducts] = useState([]);
  const [pageInfo, setPageInfo] = useState({ number: 0, totalPages: 0 });
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Filters
  const [keyword, setKeyword] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [inStockOnly, setInStockOnly] = useState(false);
  const [page, setPage] = useState(1);
  const size = 12;

  useEffect(() => {
    getCategories()
      .then((res) => setCategories(res.data))
      .catch(() => {});
  }, []);

  useEffect(() => {
    fetchProducts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  const fetchProducts = async () => {
    setLoading(true);
    setError('');
    try {
      const params = { page, size };
      if (keyword) params.keyword = keyword;
      if (categoryId) params.categoryId = categoryId;
      if (minPrice) params.minPrice = minPrice;
      if (maxPrice) params.maxPrice = maxPrice;
      if (inStockOnly) params.inStockOnly = true;

      const res = await getProducts(params);
      setProducts(res.data.items || []);
      setPageInfo(res.data.pageInfo || { number: 0, totalPages: 0 });
    } catch (err) {
      setError('Không thể tải danh sách sản phẩm.');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(1);
    fetchProducts();
  };

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  return (
    <div>
      <h2 className="mb-4">Sản phẩm</h2>

      {/* Filters */}
      <Form onSubmit={handleSearch} className="mb-4">
        <Row className="g-2 align-items-end">
          <Col md={3}>
            <InputGroup>
              <Form.Control
                placeholder="Tìm kiếm..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
              />
            </InputGroup>
          </Col>
          <Col md={2}>
            <Form.Select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
              <option value="">Tất cả danh mục</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name}
                </option>
              ))}
            </Form.Select>
          </Col>
          <Col md={2}>
            <Form.Control
              type="number"
              placeholder="Giá từ"
              value={minPrice}
              onChange={(e) => setMinPrice(e.target.value)}
            />
          </Col>
          <Col md={2}>
            <Form.Control
              type="number"
              placeholder="Giá đến"
              value={maxPrice}
              onChange={(e) => setMaxPrice(e.target.value)}
            />
          </Col>
          <Col md={2}>
            <Form.Check
              type="checkbox"
              label="Còn hàng"
              checked={inStockOnly}
              onChange={(e) => setInStockOnly(e.target.checked)}
            />
          </Col>
          <Col md={1}>
            <Button type="submit" variant="primary" className="w-100">
              Lọc
            </Button>
          </Col>
        </Row>
      </Form>

      {loading && <LoadingSpinner />}
      {error && <p className="text-danger">{error}</p>}

      {!loading && products.length === 0 && !error && (
        <p className="text-muted text-center">Không tìm thấy sản phẩm nào.</p>
      )}

      <Row>
        {products.map((product) => (
          <Col key={product.id} xs={6} sm={6} md={4} lg={3} className="mb-4">
            <Card as={Link} to={`/products/${product.id}`} className="h-100 text-decoration-none">
              {product.pictureUrl && (
                <Card.Img
                  variant="top"
                  src={product.pictureUrl}
                  alt={product.name}
                  style={{ height: '200px', objectFit: 'cover' }}
                />
              )}
              <Card.Body>
                <Card.Title className="fs-6">{product.name}</Card.Title>
                <Card.Text className="text-primary fw-bold">
                  {formatPrice(product.price)}
                </Card.Text>
                <Card.Text>
                  <small className={product.status === 'ACTIVE' ? 'text-success' : 'text-danger'}>
                    {product.status === 'ACTIVE' ? 'Còn hàng' : 'Hết hàng'}
                  </small>
                </Card.Text>
              </Card.Body>
            </Card>
          </Col>
        ))}
      </Row>

      <Pagination
        currentPage={page}
        totalPages={pageInfo.totalPages || 1}
        onPageChange={setPage}
      />
    </div>
  );
};

export default ProductListPage;
