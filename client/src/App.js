import React from 'react';
import { BrowserRouter as Router, Switch, Route, Redirect } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { CartProvider } from './contexts/CartContext';
import Layout from './components/Layout/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import RoleRoute from './components/RoleRoute';

// Pages
import LoginPage from './pages/Login/LoginPage';
import ProductListPage from './pages/Products/ProductListPage';
import ProductDetailPage from './pages/Products/ProductDetailPage';
import CartPage from './pages/Cart/CartPage';
import CheckoutPage from './pages/Checkout/CheckoutPage';
import OrderListPage from './pages/Orders/OrderListPage';
import OrderDetailPage from './pages/Orders/OrderDetailPage';
import SellerProductsPage from './pages/Seller/SellerProductsPage';
import ProductFormPage from './pages/Seller/ProductFormPage';
import SellerSubOrdersPage from './pages/Seller/SellerSubOrdersPage';
import CategoryManagementPage from './pages/Admin/CategoryManagementPage';
import NotFoundPage from './pages/NotFound/NotFoundPage';

function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <Router>
          <Layout>
            <Switch>
              {/* Public */}
              <Route exact path="/login" component={LoginPage} />
              <Route exact path="/products" component={ProductListPage} />
              <Route exact path="/products/:id" component={ProductDetailPage} />

              {/* Protected - any authenticated user */}
              <ProtectedRoute exact path="/cart" component={CartPage} />
              <ProtectedRoute exact path="/checkout" component={CheckoutPage} />

              {/* Customer */}
              <RoleRoute exact path="/orders" roles={['CUSTOMER']} component={OrderListPage} />
              <RoleRoute exact path="/orders/:id" roles={['CUSTOMER']} component={OrderDetailPage} />

              {/* Seller */}
              <RoleRoute exact path="/seller/products" roles={['SELLER']} component={SellerProductsPage} />
              <RoleRoute exact path="/seller/products/new" roles={['SELLER']} component={ProductFormPage} />
              <RoleRoute exact path="/seller/products/:id/edit" roles={['SELLER']} component={ProductFormPage} />
              <RoleRoute exact path="/seller/sub-orders" roles={['SELLER']} component={SellerSubOrdersPage} />

              {/* Admin */}
              <RoleRoute exact path="/admin/categories" roles={['ADMIN']} component={CategoryManagementPage} />

              {/* Default redirect */}
              <Route exact path="/">
                <Redirect to="/products" />
              </Route>

              {/* 404 */}
              <Route component={NotFoundPage} />
            </Switch>
          </Layout>
        </Router>
      </CartProvider>
    </AuthProvider>
  );
}

export default App;
