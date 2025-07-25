export default async function Cart() {
  const data: CartListResponseDto = {
    message: "success",
    cartItems: [
      {
        itemCode: "ITEM001",
        itemName: "츤데레 AI 피규어",
        price: 25000,
        quantity: 1,
        imageUrls: ["a.png", "a.png"],
      },
      {
        itemCode: "ITEM002",
        itemName: "얀데레 머그컵",
        price: 12000,
        quantity: 2,
        imageUrls: ["a.png", "a.png"],
      },
      {
        itemCode: "ITEM003",
        itemName: "메이드복 세트",
        price: 80000,
        quantity: 1,
        imageUrls: ["a.png", "a.png"],
      },
      {
        itemCode: "ITEM004",
        itemName: "집사용 에이프런",
        price: 15000,
        quantity: 3,
        imageUrls: ["a.png", "a.png"],
      },
      {
        itemCode: "ITEM005",
        itemName: "고양이 귀 헤드폰",
        price: 35000,
        quantity: 1,
        imageUrls: ["a.png", "a.png"],
      },
    ],
  };
  return (
    <div>
      <div></div>
    </div>
  );
}
