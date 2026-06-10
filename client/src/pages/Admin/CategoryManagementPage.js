import React, { useState, useEffect } from 'react';
import { Table, Form, Button, Alert } from 'react-bootstrap';
import { getCategories, createCategory } from '../../api/categoryApi';
import LoadingSpinner from '../../components/LoadingSpinner';

const CategoryManagementPage = () => {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [name, setName] = useState('');
  const [creating, setCreating] = useState(false);
  const [success, setSuccess] = useState('');

  useEffect(() => {
    fetchCategories();
  }, []);

  const fetchCategories = async () => {
    setLoading(true);
    try {
      const res = await getCategories();
      setCategories(res.data);
    } catch (err) {
      setError('Không thể tải danh sách danh mục.');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!name.trim()) {
      setError('Tên danh mục không được để trống.');
      return;
    }

    setCreating(true);
    try {
      await createCategory({ name: name.trim() });
      setName('');
      setSuccess('Tạo danh mục thành công!');
      fetchCategories();
    } catch (err) {
      if (err.response?.status === 403) {
        setError('Không đủ quyền. Chỉ ADMIN mới có thể tạo danh mục.');
      } else {
        setError(err.response?.data?.message || 'Tạo danh mục thất bại.');
      }
    } finally {
      setCreating(false);
    }
  };

  return (
    <div>
      <h2 className="mb-4">Quản lý danh mục</h2>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}
      {success && <Alert variant="success" onClose={() => setSuccess('')} dismissible>{success}</Alert>}

      <Form onSubmit={handleCreate} className="mb-4">
        <div className="d-flex gap-2">
          <Form.Control
            type="text"
            placeholder="Tên danh mục mới"
            value={name}
            onChange={(e) => setName(e.target.value)}
            style={{ maxWidth: '300px' }}
          />
          <Button type="submit" variant="success" disabled={creating}>
            {creating ? 'Đang tạo...' : 'Thêm'}
          </Button>
        </div>
      </Form>

      {loading && <LoadingSpinner />}

      {!loading && categories.length > 0 && (
        <Table striped bordered hover>
          <thead>
            <tr>
              <th>#</th>
              <th>Tên danh mục</th>
              <th>ID</th>
            </tr>
          </thead>
          <tbody>
            {categories.map((cat, idx) => (
              <tr key={cat.id}>
                <td>{idx + 1}</td>
                <td>{cat.name}</td>
                <td><small>{cat.id}</small></td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}

      {!loading && categories.length === 0 && !error && (
        <p className="text-muted">Chưa có danh mục nào.</p>
      )}
    </div>
  );
};

export default CategoryManagementPage;
