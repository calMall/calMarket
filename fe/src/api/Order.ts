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
    const error: any = new Error(data.statusText);
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
