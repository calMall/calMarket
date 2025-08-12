interface OrderDetailResponseDto extends ResponseDTO {
  order: {
    orderId: number;
    itemCode: string;
    itemName: string;
    price: number;
    quantity: number;
    date: string;
    imageList: string[];
    deliveryAddress: string;
    orderDate: string;
  };
}
interface OrderCheckout extends ResponseDTO {
  cartList: CheckoutItem[];
}
interface CheckoutItem {
  id: number;
  imageUrl: string;
  itemCode: string;
  itemName: string;
  price: number;
  quantity: number;
}
interface TempOrderItem {
  itemCode: string;
  itemName: string;
  itemCaption: string;
  catchcopy: string;
  score: number;
  reviewCount: number;
  price: number;
  imageUrls: string[];
}
interface OrderRequestDto {
  deliveryAddress: string;
  items: {
    itemCode: string;
    quantity: number;
  }[];
}
