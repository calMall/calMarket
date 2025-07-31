const url = process.env.NEXT_PUBLIC_BASE_URL;

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
