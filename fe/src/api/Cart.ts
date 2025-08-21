const url = process.env.NEXT_PUBLIC_BASE_URL;

export const getCart = async (): Promise<CartListResponseDto> => {
  const data = await fetch(`${url}/cart`, {
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
export const postCart = async (
  itemCode: string,
  quantity: number
): Promise<ResponseDTO> => {
  const data = await fetch(`${url}/cart`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ itemCode, quantity }),
  });

  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status;
    throw error;
  }
  return data.json();
};

export const deleteCart = async (
  cartItemIds: number[]
): Promise<ResponseDTO> => {
  const data = await fetch(`${url}/cart/remove-selected`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(cartItemIds),
  });

  if (!data.ok) {
    const error: any = new Error(data.statusText);
    console.log(error);
    error.status = data.status;
    throw error;
  }
  return data.json();
};

export const getCheckout = async (
  cartItemIds: number[]
): Promise<OrderCheckout> => {
  console.log(cartItemIds);
  const data = await fetch(`${url}/cart/list-for-order`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(cartItemIds),
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

export const increaseProduct = async (
  cartCode: number
): Promise<ResponseDTO> => {
  const data = await fetch(`${url}/cart/${cartCode}/increase`, {
    method: "PATCH",
    credentials: "include",
  });
  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status;
    throw error;
  }
  return data.json();
};
export const decreaseProduct = async (
  cartCode: number
): Promise<ResponseDTO> => {
  const data = await fetch(`${url}/cart/${cartCode}/decrease`, {
    method: "PATCH",
    credentials: "include",
  });
  if (!data.ok) {
    const error: any = new Error(data.statusText);
    error.status = data.status;
    throw error;
  }
  return data.json();
};
