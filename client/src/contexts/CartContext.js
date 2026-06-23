import React, { createContext, useContext, useState, useEffect } from 'react';
import { getCart, addItemToCart, updateCartItemQuantity, removeCartItem, clearCartApi } from '../api/cartApi';
import { useAuth } from './AuthContext';

const CartContext = createContext(null);

export const useCart = () => {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
};

export const CartProvider = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const [items, setItems] = useState([]);
  const [totalPrice, setTotalPrice] = useState(0);

  const fetchCart = async () => {
    try {
      if (!isAuthenticated) {
        setItems([]);
        setTotalPrice(0);
        return;
      }
      const res = await getCart();
      const cartItems = res.data.items || [];
      setItems(cartItems);
      setTotalPrice(cartItems.reduce((sum, item) => sum + item.price * item.quantity, 0));
    } catch (error) {
      console.error("Error fetching cart:", error);
    }
  };

  useEffect(() => {
    fetchCart();
  }, [isAuthenticated]);

  const addToCart = async (product, quantity = 1) => {
    try {
      if (!isAuthenticated) return;
      await addItemToCart({
        productId: product.id,
        quantity: quantity
      });
      await fetchCart();
    } catch (error) {
      console.error("Error adding to cart:", error);
    }
  };

  const updateQuantity = async (productId, quantity) => {
    try {
      if (!isAuthenticated) return;
      await updateCartItemQuantity(productId, quantity);
      await fetchCart();
    } catch (error) {
      console.error("Error updating cart item quantity:", error);
    }
  };

  const removeFromCart = async (productId) => {
    try {
      if (!isAuthenticated) return;
      await removeCartItem(productId);
      await fetchCart();
    } catch (error) {
      console.error("Error removing from cart:", error);
    }
  };

  const clearCart = async () => {
    try {
      if (!isAuthenticated) return;
      await clearCartApi();
      await fetchCart();
    } catch (error) {
      console.error("Error clearing cart:", error);
    }
  };

  return (
    <CartContext.Provider
      value={{
        items,
        totalPrice,
        addToCart,
        updateQuantity,
        removeFromCart,
        clearCart,
      }}
    >
      {children}
    </CartContext.Provider>
  );
};

export default CartContext;
