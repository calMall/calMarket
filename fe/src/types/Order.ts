interface OrderDetailResponseDto extends ResponseDTO {
  order: OrderInfo;
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

interface OrderedItem {
  imageList: string[];
  itemCode: string;
  itemName: string;
  price: number;
  quantity: number;
}
interface OrderInfo {
  deliveryAddress: string;
  orderDate: string;
  orderId: number;
  orderItems: OrderedItem[];
  status: OrderStatus;
}
type OrderStatus =
  | "DELIVERED"
  | "SHIPPED"
  | "PENDING"
  | "CANCELLED"
  | "REFUNDED";

interface OrderListResponseDto extends ResponseDTO {
  orders: OrderInfoOnList[];
}

interface OrderInfoOnList {
  orderId: number;
  itemCode: string;
  itemName: string;
  price: number;
  quantity: number;
  date: string;
  imageList: string[];
  orderDate: string;
}
