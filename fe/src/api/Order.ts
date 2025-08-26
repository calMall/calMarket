const url = process.env.NEXT_PUBLIC_BASE_URL;

export const postOrderByProduct = async (
  orderRequest: OrderRequestDto
): Promise<ResponseDTO> => {
  const data = await fetch(`${url}/orders`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(orderRequest),
  });

  if (!data.ok) {
    let errorMessage = data.statusText;
    const errorBody = await data.json();
    if (errorBody && errorBody.message) {
      errorMessage = errorBody.message;
    }
    const error: any = new Error(errorMessage);
    error.status = data.status;
    throw error;
  }
  return data.json();
};
export const getOrderDetail = async (
  id: number
): Promise<OrderDetailResponseDto> => {
  const data = await fetch(`${url}/orders/${id}`, {
    method: "GET",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
  });

  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status;
    throw error;
  }
  return data.json();
};
export const getOrderList = async (): Promise<OrderListResponseDto> => {
  const data = await fetch(`${url}/orders/history`, {
    method: "GET",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
  });

  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status;
    throw error;
  }
  return data.json();
};

export const PostCancelOrder = async (
  orderId: number
): Promise<ResponseDTO> => {
  const data = await fetch(`${url}/orders/cancel/${orderId}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
  });

  if (!data.ok) {
    let errorMessage = data.statusText;
    const errorBody = await data.json();
    if (errorBody && errorBody.message) {
      errorMessage = errorBody.message;
    }
    const error: any = new Error(errorMessage);
    error.status = data.status;
    throw error;
  }
  return data.json();
};
export const PostRefundOrder = async (
  orderId: number
): Promise<ResponseDTO> => {
  const data = await fetch(`${url}/orders/refund/${orderId}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
  });

  if (!data.ok) {
    let errorMessage = data.statusText;
    const errorBody = await data.json();
    if (errorBody && errorBody.message) {
      errorMessage = errorBody.message;
    }
    const error: any = new Error(errorMessage);
    error.status = data.status;
    throw error;
  }
  return data.json();
};
