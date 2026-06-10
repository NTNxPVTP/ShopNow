import React, { createContext, useContext, useReducer, useEffect } from 'react';

const CartContext = createContext(null);

const CART_STORAGE_KEY = 'shopnow_cart';

const getInitialCart = () => {
  try {
    const stored = localStorage.getItem(CART_STORAGE_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (err) {
    // ignore
  }
  return { items: [] };
};

const calculateTotal = (items) => {
  return items.reduce((sum, item) => sum + item.price * item.quantity, 0);
};

const cartReducer = (state, action) => {
  let newItems;
  switch (action.type) {
    case 'ADD_TO_CART': {
      const existing = state.items.find((item) => item.productId === action.payload.productId);
      if (existing) {
        newItems = state.items.map((item) =>
          item.productId === action.payload.productId
            ? { ...item, quantity: item.quantity + action.payload.quantity }
            : item
        );
      } else {
        newItems = [...state.items, action.payload];
      }
      return { ...state, items: newItems, totalPrice: calculateTotal(newItems) };
    }
    case 'UPDATE_QUANTITY': {
      const qty = Math.max(1, action.payload.quantity);
      newItems = state.items.map((item) =>
        item.productId === action.payload.productId ? { ...item, quantity: qty } : item
      );
      return { ...state, items: newItems, totalPrice: calculateTotal(newItems) };
    }
    case 'REMOVE_FROM_CART': {
      newItems = state.items.filter((item) => item.productId !== action.payload.productId);
      return { ...state, items: newItems, totalPrice: calculateTotal(newItems) };
    }
    case 'CLEAR_CART': {
      return { items: [], totalPrice: 0 };
    }
    default:
      return state;
  }
};

export const useCart = () => {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
};

export const CartProvider = ({ children }) => {
  const initialState = getInitialCart();
  initialState.totalPrice = calculateTotal(initialState.items);

  const [state, dispatch] = useReducer(cartReducer, initialState);

  useEffect(() => {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify({ items: state.items }));
  }, [state.items]);

  const addToCart = (product, quantity = 1) => {
    dispatch({
      type: 'ADD_TO_CART',
      payload: {
        productId: product.id,
        name: product.name,
        price: product.price,
        pictureUrl: product.pictureUrl,
        quantity,
      },
    });
  };

  const updateQuantity = (productId, quantity) => {
    dispatch({ type: 'UPDATE_QUANTITY', payload: { productId, quantity } });
  };

  const removeFromCart = (productId) => {
    dispatch({ type: 'REMOVE_FROM_CART', payload: { productId } });
  };

  const clearCart = () => {
    dispatch({ type: 'CLEAR_CART' });
  };

  return (
    <CartContext.Provider
      value={{
        items: state.items,
        totalPrice: state.totalPrice,
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
