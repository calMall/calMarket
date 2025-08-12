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
