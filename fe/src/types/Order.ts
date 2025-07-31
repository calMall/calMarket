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
