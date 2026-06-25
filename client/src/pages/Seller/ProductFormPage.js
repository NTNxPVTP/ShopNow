import React, { useState, useEffect } from 'react';
import { Form, Button, Alert } from 'react-bootstrap';
import { useParams, useHistory } from 'react-router-dom';
import { updateProduct, getProductById } from '../../api/productApi';
import { createProduct } from '../../api/shopApi';
import { getCategories } from '../../api/categoryApi';
import LoadingSpinner from '../../components/LoadingSpinner';

const ProductFormPage = () => {
  const { id, shopId: paramShopId } = useParams();
  const history = useHistory();
  const isEdit = !!id;

  const [name, setName] = useState('');
  const [pictureUrl, setPictureUrl] = useState('');
  const [quantity, setQuantity] = useState('');
  const [price, setPrice] = useState('');
  const [categoryIds, setCategoryIds] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [pageLoading, setPageLoading] = useState(isEdit);

  useEffect(() => {
    getCategories()
      .then((res) => setCategories(res.data))
      .catch(() => {});

    if (isEdit) {
      getProductById(id)
        .then((res) => {
          const p = res.data;
          setName(p.name || '');
          setPictureUrl(p.pictureUrl || '');
          setQuantity(p.quantity?.toString() || '');
          setPrice(p.price?.toString() || '');
          setCategoryIds(p.categoryIds ? Array.from(p.categoryIds) : []);
        })
        .catch(() => setError('Không thể tải thông tin sản phẩm.'))
        .finally(() => setPageLoading(false));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleCategoryChange = (e) => {
    const selected = Array.from(e.target.selectedOptions, (opt) => opt.value);
    setCategoryIds(selected);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (isEdit) {
        await updateProduct(id, {
          name: name || undefined,
          pictureUrl: pictureUrl || undefined,
          quantity: quantity ? parseInt(quantity) : undefined,
          price: price ? parseFloat(price) : undefined,
        });
      } else {
        if (!paramShopId) {
          setError('Shop ID bị thiếu trên URL.');
          setLoading(false);
          return;
        }
        await createProduct(paramShopId, {
          name,
          pictureUrl: pictureUrl || null,
          quantity: parseInt(quantity),
          price: parseFloat(price),
          categoryIds: categoryIds.length > 0 ? categoryIds : null,
        });
      }
      history.push('/seller/products');
    } catch (err) {
      if (err.response?.status === 403) {
        setError('Bạn không có quyền thực hiện thao tác này.');
      } else {
        setError(err.response?.data?.message || 'Thao tác thất bại.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (pageLoading) return <LoadingSpinner />;

  return (
    <div>
      <h2 className="mb-4">{isEdit ? 'Chỉnh sửa sản phẩm' : 'Thêm sản phẩm mới'}</h2>
      {error && <Alert variant="danger">{error}</Alert>}

      <Form onSubmit={handleSubmit}>
        <Form.Group className="mb-3">
          <Form.Label>Tên sản phẩm *</Form.Label>
          <Form.Control
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>URL hình ảnh</Form.Label>
          <Form.Control
            type="url"
            value={pictureUrl}
            onChange={(e) => setPictureUrl(e.target.value)}
          />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Số lượng *</Form.Label>
          <Form.Control
            type="number"
            min={0}
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            required
          />
        </Form.Group>

        <Form.Group className="mb-3">
          <Form.Label>Giá *</Form.Label>
          <Form.Control
            type="number"
            min={0}
            step="0.01"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            required
          />
        </Form.Group>



        <Form.Group className="mb-3">
          <Form.Label>Danh mục</Form.Label>
          <Form.Select multiple value={categoryIds} onChange={handleCategoryChange}>
            {categories.map((cat) => (
              <option key={cat.id} value={cat.id}>
                {cat.name}
              </option>
            ))}
          </Form.Select>
          <Form.Text className="text-muted">Giữ Ctrl để chọn nhiều danh mục</Form.Text>
        </Form.Group>

        <Button type="submit" variant="primary" disabled={loading}>
          {loading ? 'Đang xử lý...' : isEdit ? 'Cập nhật' : 'Tạo sản phẩm'}
        </Button>
        <Button
          variant="secondary"
          className="ms-2"
          onClick={() => history.push('/seller/products')}
        >
          Hủy
        </Button>
      </Form>
    </div>
  );
};

export default ProductFormPage;
