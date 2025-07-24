const url = process.env.NEXT_PUBLIC_BASE_URL;
export const getProductDetail = async (
  itemCode: string
): Promise<ProductDetailResponseDto> => {
  const data = await fetch(`${url}/products/${itemCode}`, {
    method: "GET",
    credentials: "include",
  });
  if (!data.ok) throw new Error(data.statusText);
  return data.json();
};
