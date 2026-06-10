import React from 'react';
import { Link } from 'react-router-dom';

const NotFoundPage = () => {
  return (
    <div className="text-center py-5">
      <h1 className="display-1">404</h1>
      <h3>Trang không tìm thấy</h3>
      <p className="text-muted">Trang bạn đang tìm kiếm không tồn tại.</p>
      <Link to="/products" className="btn btn-primary">
        Về trang sản phẩm
      </Link>
    </div>
  );
};

export default NotFoundPage;
