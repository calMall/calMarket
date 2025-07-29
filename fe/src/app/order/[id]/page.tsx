export default function OrderDetail() {
  const data: OrderDetailResponseDto = {
    message: "success",
    order: {
      orderId: 12345,
      itemCode: "ITEM789",
      itemName: "おっとり執事のティーセット",
      price: 5800,
      quantity: 2,
      date: "2025-07-28",
      imageList: [
        "https://thumbnail.image.rakuten.co.jp/@0_mall/best1mobile/cabinet/compass1704878231.jpg?_ex=128x128",
      ],
      deliveryAddress: "東京都千代田区神田練塀町300番地",
      orderDate: "2025-07-27T10:30:00Z",
    },
  };

  return (
    <div>
      <div></div>
    </div>
  );
}
