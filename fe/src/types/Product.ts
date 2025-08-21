interface ProductDetailResponseDto extends ResponseDTO {
  product: {
    itemCode: string;
    itemName: string;
    itemCaption: string;
    catchcopy: string;
    score: number;
    reviewCount: number;
    price: number;
    imageUrls: string[];
  };
}
interface CartListResponseDto extends ResponseDTO {
  cartItems: cartItem[];
}

interface cartItem {
  id: number;
  itemCode: string;
  itemName: string;
  price: number;
  quantity: number;
  imageUrls: string[];
}
